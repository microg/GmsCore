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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
import java.util.concurrent.ConcurrentHashMap

@RequiresApi(21)
internal suspend fun Context.installPackages(
        packageName: String,
        componentFiles: List<File>,
        isUpdate: Boolean = false,
        splitInstall: Boolean = false,
        keepFiles: Boolean = false,
        splitSessionId: Int? = null,
        onSessionCreated: ((Int) -> Unit)? = null,
        removedSplits: List<String> = emptyList(),
) {
    val notifyId = createNotificationId(packageName, emptyList())
    installPackagesInternal(
            packageName = packageName,
            componentNames = componentFiles.map { it.name },
            notifyId = notifyId,
            isUpdate = isUpdate,
            splitInstall = splitInstall,
            splitSessionId = splitSessionId,
            onSessionCreated = onSessionCreated,
            removedSplits = removedSplits,
    ) {_, notifyId, fileName, to ->
        val component = componentFiles.find { it.name == fileName }!!
        FileInputStream(component).use { it.copyTo(to) }
        if (!keepFiles) component.delete()
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
        removedSplits: List<String> = emptyList(),
        splitSessionId: Int? = null,
        onSessionCreated: ((Int) -> Unit)? = null,
        writeComponent: suspend (downloadedBytes: Long, notifyId: Int, componentName: String, to: OutputStream) -> Unit
) {
    Log.v(TAG, "installPackages start $packageName")
    //Some systems are unable to retrieve information about installed apps, making the `installed` status unreliable.
    val installed = packageManager.getInstalledPackages(0).any {
        it.applicationInfo?.packageName == packageName
    }
    val packageInstaller = packageManager.packageInstaller
    // Contrary to docs, MODE_INHERIT_EXISTING cannot be used if package is not yet installed.
    val mode = if (isUpdate || (!splitInstall && !installed)) SessionParams.MODE_FULL_INSTALL else SessionParams.MODE_INHERIT_EXISTING
    val params = SessionParams(mode)
    params.setAppPackageName(packageName)
    if (splitInstall && android.os.Build.VERSION.SDK_INT >= 31) params.setRequireUserAction(SessionParams.USER_ACTION_NOT_REQUIRED)
    val key = computeUniqueKey(packageName, componentNames)
    params.setAppLabel(key)
    params.setInstallLocation(if (splitInstall) PackageInfo.INSTALL_LOCATION_AUTO else PackageInfo.INSTALL_LOCATION_INTERNAL_ONLY)
    params.setDontKillAppCompat()
    var sessionId = -1
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

        if (android.os.Build.VERSION.SDK_INT >= 24 && mode == SessionParams.MODE_INHERIT_EXISTING) removedSplits.forEach { session.removeSplit(it) }

        for (component in componentNames) {
            val currentSize: Long = try {
                val inputStream = session!!.openRead(component)
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
            } catch (e: SecurityException) {
                Log.w(TAG, "installPackagesInternal session open read error, ${e.message}")
                //Handling exceptions java.lang.SecurityException: openRead not allowed after commit
                session!!.abandon()
                sessionId = packageInstaller.createSession(params)
                session = packageInstaller.openSession(sessionId)
                0L
            }

            Log.d(TAG, "installPackagesInternal component: $component currentSize:$currentSize")
            session!!.openWrite(component, currentSize, -1).use { outputStream ->
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
        onSessionCreated?.invoke(sessionId)
        if (splitSessionId == null) SessionResultReceiver.pendingSessions[sessionId] = SessionResultReceiver.OnResult(
                onSuccess = {
                    deferred.complete(Unit)
                    emitProgress(notifyId, InstallComplete)
                },
                onFailure = { message ->
                    deferred.completeExceptionally(RuntimeException(message))
                    emitProgress(notifyId, InstallError(message ?: "UNKNOWN"))
                },
                onCanceled = { deferred.cancel() }
        )
        val intent = Intent(this, SessionResultReceiver::class.java)
        intent.putExtra(SessionResultReceiver.KEY_NOTIFY_ID, splitSessionId ?: notifyId)
        intent.putExtra(SessionResultReceiver.KEY_PACKAGE_NAME, packageName)
        splitSessionId?.let { intent.putExtra(SessionResultReceiver.KEY_SPLIT_SESSION_ID, it) }
        val pendingIntent = PendingIntentCompat.getBroadcast(
                this, sessionId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT, true
        )!!

        emitProgress(notifyId, CommitingSession)
        session!!.commit(pendingIntent.intentSender)
        // don't abandon if `finally` step is reached after this point
        //session.close()

        Log.d(TAG, "installPackages session commit")
        if (splitSessionId == null) deferred.await()
    } catch (e: Exception) {
        SessionResultReceiver.pendingSessions.remove(sessionId)
        runCatching { session?.abandon() }
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

private const val FULL_INSTALL_PROBE_LABEL = "split-install-capability"
internal const val ACTION_FULL_INSTALL_PROBE = "com.android.vending.installer.FULL_INSTALL_PROBE"
private val fullInstallProbeMutex = Mutex()
private val fullInstallProbeResults = ConcurrentHashMap<Int, CompletableDeferred<Boolean?>>()
private var fullInstallKeepsAppAlive: Boolean? = null

@RequiresApi(21)
private fun SessionParams.setDontKillAppCompat(): Boolean =
    try {
        @SuppressLint("PrivateApi") val method = SessionParams::class.java.getDeclaredMethod(
                "setDontKillApp", Boolean::class.javaPrimitiveType
        )
        method.invoke(this, true)
        true
    } catch (e: Exception) {
        Log.w(TAG, "Error setting dontKillApp", e)
        false
    }

@RequiresApi(21)
internal suspend fun Context.canKeepAppAliveDuringFullInstall(): Boolean {
    if (android.os.Build.VERSION.SDK_INT < 31) return false
    return fullInstallProbeMutex.withLock {
        fullInstallKeepsAppAlive?.let { return@withLock it }
        val installer = packageManager.packageInstaller
        var id = -1
        try {
            installer.mySessions.filter { it.appLabel == FULL_INSTALL_PROBE_LABEL }.forEach { installer.abandonSession(it.sessionId) }
            val params = SessionParams(SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(packageName)
                setAppLabel(FULL_INSTALL_PROBE_LABEL)
                // The probe must stop before installation, even for privileged installers.
                setRequireUserAction(SessionParams.USER_ACTION_REQUIRED)
                check(setDontKillAppCompat()) { "Unable to request install process preservation" }
            }
            id = installer.createSession(params)
            installer.openSession(id).use { session ->
                val app = applicationInfo
                (listOf(app.sourceDir) + app.splitSourceDirs.orEmpty()).forEach { path ->
                    val file = File(path)
                    session.openWrite(file.name, 0, file.length()).use { output ->
                        file.inputStream().use { it.copyTo(output) }
                        session.fsync(output)
                    }
                }
                val result = CompletableDeferred<Boolean?>()
                fullInstallProbeResults[id] = result
                val intent = Intent(this, SessionResultReceiver::class.java).setAction(ACTION_FULL_INSTALL_PROBE)
                val callback = PendingIntentCompat.getBroadcast(this, id, intent, PendingIntent.FLAG_UPDATE_CURRENT, true)!!
                session.commit(callback.intentSender)
                fullInstallKeepsAppAlive = checkNotNull(withTimeoutOrNull(30_000L) { result.await() }) {
                    "Unable to determine full install behavior"
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Unable to check full install behavior (${e.javaClass.simpleName})")
            throw IllegalStateException("Unable to check full install behavior", e)
        } finally {
            fullInstallProbeResults.remove(id)
            if (id != -1) runCatching { installer.abandonSession(id) }
        }
        checkNotNull(fullInstallKeepsAppAlive)
    }
}

@RequiresApi(21)
internal fun Context.onFullInstallProbeResult(intent: Intent) {
    val id = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
    val installer = packageManager.packageInstaller
    var keepsAppAlive: Boolean? = null
    try {
        val info = installer.getSessionInfo(id)
        if (info?.appLabel == FULL_INSTALL_PROBE_LABEL && intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1) == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            @SuppressLint("PrivateApi") val method = PackageInstaller.SessionInfo::class.java.getDeclaredMethod("getDontKillApp")
            keepsAppAlive = method.invoke(info) as? Boolean
        }
    } catch (e: Exception) {
        Log.w(TAG, "Unable to read full install behavior (${e.javaClass.simpleName})")
    } finally {
        runCatching { installer.abandonSession(id) }
        fullInstallProbeResults.remove(id)?.complete(keepsAppAlive)
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
