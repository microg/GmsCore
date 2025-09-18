/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.vending.installer

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.PendingIntentCompat
import androidx.core.content.pm.PackageInfoCompat
import com.google.android.finsky.splitinstallservice.PackageComponent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.vending.billing.core.HttpClient
import org.microg.vending.enterprise.CommitingSession
import org.microg.vending.enterprise.Downloading
import org.microg.vending.enterprise.InstallComplete
import org.microg.vending.enterprise.InstallError
import org.microg.vending.enterprise.InstallProgress
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream

@RequiresApi(21)
internal suspend fun Context.installPackages(
        packageName: String,
        componentFiles: List<File>,
        isUpdate: Boolean = false,
        splitInstall: Boolean = false,
) {
    val notifyId = createNotificationId(packageName, emptyList())
    installPackagesInternal(
            packageName = packageName,
            componentNames = componentFiles.map { it.name },
            notifyId = notifyId,
            isUpdate = isUpdate,
            splitInstall = splitInstall,
    ) {_, notifyId, fileName, to ->
        val component = componentFiles.find { it.name == fileName }!!
        FileInputStream(component).use { it.copyTo(to) }
        component.delete()
    }
}

@RequiresApi(21)
internal suspend fun Context.installPackagesFromNetwork(
        packageName: String,
        components: List<PackageComponent>,
        httpClient: HttpClient = HttpClient(),
        isUpdate: Boolean = false,
        splitInstall: Boolean = false,
        emitProgress: (notifyId: Int, InstallProgress) -> Unit = { _, _ -> }
) {

    val downloadProgress = mutableMapOf<PackageComponent, Long>()
    //Generate a notifyId based on the package name and download module to prevent multiple notifications from appearing when the download content is the same
    val notifyId = createNotificationId(packageName, components)
    installPackagesInternal(
            packageName = packageName,
            componentNames = components.map { it.componentName },
            notifyId = notifyId,
            isUpdate = isUpdate,
            splitInstall = splitInstall,
            emitProgress = emitProgress,
    ) {downloadedBytes, notifyId, fileName, to ->
        val component = components.find { it.componentName == fileName }!!
        Log.v(TAG, "installing $fileName for $packageName from network apk size:" + component.size + " downloaded: " + downloadedBytes)
        if (downloadedBytes < component.size) {
            // Emit progress for the first time as soon as possible, before any network interaction
            emitProgress(notifyId, Downloading(
                    bytesDownloaded = downloadProgress.values.sum(),
                    bytesTotal = components.sumOf { it.size }
            ))
            httpClient.download(component.url, to, downloadedBytes = downloadedBytes) { progress ->
                downloadProgress[component] = progress
                emitProgress(notifyId, Downloading(
                        bytesDownloaded = downloadProgress.values.sum(),
                        bytesTotal = components.sumOf { it.size }
                ))
            }
        }
    }
}

@RequiresApi(21)
private suspend fun Context.installPackagesInternal(
        packageName: String,
        componentNames: List<String>,
        notifyId: Int,
        isUpdate: Boolean = false,
        splitInstall: Boolean = false,
        emitProgress: (notifyId: Int, InstallProgress) -> Unit = { _, _ -> },
        writeComponent: suspend (downloadedBytes: Long, notifyId: Int, componentName: String, to: OutputStream) -> Unit
) {
    Log.v(TAG, "installPackages start $packageName")
    //Some systems are unable to retrieve information about installed apps, making the `installed` status unreliable.
    val installed = packageManager.getInstalledPackages(0).any {
        it.applicationInfo?.packageName == packageName
    }
    val packageInstaller = packageManager.packageInstaller
    // Contrary to docs, MODE_INHERIT_EXISTING cannot be used if package is not yet installed.
    val params = SessionParams(
            if (!splitInstall && (!installed || isUpdate)) SessionParams.MODE_FULL_INSTALL
            else SessionParams.MODE_INHERIT_EXISTING
    )
    params.setAppPackageName(packageName)
    val key = computeUniqueKey(packageName, componentNames)
    params.setAppLabel(key)
    params.setInstallLocation(PackageInfo.INSTALL_LOCATION_INTERNAL_ONLY)
    try {
        @SuppressLint("PrivateApi") val method = SessionParams::class.java.getDeclaredMethod(
                "setDontKillApp", Boolean::class.javaPrimitiveType
        )
        method.invoke(params, true)
    } catch (e: Exception) {
        Log.w(TAG, "Error setting dontKillApp", e)
    }
    val sessionId: Int
    var session: PackageInstaller.Session? = null
    try {
        val sessionInfo = packageInstaller.mySessions.firstOrNull{ it.appLabel == key }
        // This needs to be handled to prevent reusing sessions that are not in the active state,
        // which could cause `openRead` to throw an error.
        val existingSessionId = if (sessionInfo != null && sessionInfo.isActive) {
            sessionInfo.sessionId
        } else {
            Log.w(TAG, "installPackagesInternal my session fail")
            null
        }
        sessionId = existingSessionId ?: packageInstaller.createSession(params)
        for (info in packageInstaller.mySessions) {
            Log.d(TAG, "id=${info.sessionId}, createdBy=${info.appLabel}, isActive=${info.isActive}")
        }
        Log.d(TAG, "installPackagesInternal sessionId: $sessionId")
        session = packageInstaller.openSession(sessionId)

        for (component in componentNames) {
            val currentSize: Long = try {
                val inputStream = session.openRead(component)
                val totalSize = withContext(Dispatchers.IO) {
                    val buffer = ByteArray(4096)
                    var total = 0L
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        total += bytesRead
                    }
                    inputStream.close()
                    total
                }
                totalSize
            } catch (e: IOException) {
                Log.w(TAG, "installPackagesInternal session open read error, ${e.message}")
                0L
            }

            Log.d(TAG, "installPackagesInternal component: $component currentSize:$currentSize")
            session.openWrite(component, currentSize, -1).use { outputStream ->
                try {
                    writeComponent(currentSize, notifyId, component, outputStream)
                    session.fsync(outputStream)
                } catch (e: Exception) {
                    Log.w(TAG, "Error writing component notifyId $notifyId")
                    emitProgress(notifyId, InstallError("Download Error"))
                    throw e
                }
            }
        }
        val deferred = CompletableDeferred<Unit>()
        Log.w(TAG, "installPackagesInternal pendingSessions size: ${SessionResultReceiver.pendingSessions.size}")
        SessionResultReceiver.pendingSessions[sessionId] = SessionResultReceiver.OnResult(
                onSuccess = {
                    deferred.complete(Unit)
                    emitProgress(notifyId, InstallComplete)
                },
                onFailure = { message ->
                    deferred.completeExceptionally(RuntimeException(message))
                    emitProgress(notifyId, InstallError(message ?: "UNKNOWN"))
                }
        )
        val intent = Intent(this, SessionResultReceiver::class.java)
        intent.putExtra(SessionResultReceiver.KEY_NOTIFY_ID, notifyId)
        intent.putExtra(SessionResultReceiver.KEY_PACKAGE_NAME, packageName)
        val pendingIntent = PendingIntentCompat.getBroadcast(
                this, sessionId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT, true
        )!!

        emitProgress(notifyId, CommitingSession)
        session.commit(pendingIntent.intentSender)
        // don't abandon if `finally` step is reached after this point
        //session.close()

        Log.d(TAG, "installPackages session commit")
        return deferred.await()
    } catch (e: Exception) {
        Log.w(TAG, "Error installing packages", e)
        emitProgress(notifyId, InstallError(e.message ?: "UNKNOWN"))
        throw e
    } finally {
        // Close the session to release resources after error
        session?.let {
            Log.d(TAG, "Error occurred, session cleanup may be required")
            it.close()
        }
    }
}

private fun Context.computeUniqueKey(packageName: String, componentNames: List<String>) : String {
    try {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        if (packageInfo != null) {
            val versionCode = PackageInfoCompat.getLongVersionCode(
                    packageManager.getPackageInfo(packageName, 0)
            )
            return componentNames.joinToString(separator = "_", prefix = "${packageName}_${versionCode}")
        }
    } catch (e: PackageManager.NameNotFoundException) {
        Log.w(TAG, "Package not found", e)
    }
    return componentNames.joinToString(separator = "_", prefix = packageName)
}

private fun createNotificationId(packageName: String, components: List<PackageComponent>) : Int{
    val hash = (packageName + components.joinToString("") { it.componentName }).hashCode()
    return hash and Int.MAX_VALUE
}
