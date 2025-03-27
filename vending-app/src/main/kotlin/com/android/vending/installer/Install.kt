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
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.finsky.splitinstallservice.PackageComponent
import kotlinx.coroutines.CompletableDeferred
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

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal suspend fun Context.installPackages(
    packageName: String,
    componentFiles: List<File>,
    isUpdate: Boolean = false
) = installPackagesInternal(
    packageName = packageName,
    componentNames = componentFiles.map { it.name },
    isUpdate = isUpdate
) { session, fileName, to ->
    val component = componentFiles.find { it.name == fileName }!!
    FileInputStream(component).use { it.copyTo(to) }
    component.delete()
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal suspend fun Context.installPackagesFromNetwork(
    packageName: String,
    components: List<PackageComponent>,
    httpClient: HttpClient = HttpClient(),
    isUpdate: Boolean = false,
    emitProgress: (session: Int, InstallProgress) -> Unit = { _, _ -> }
) {

    val downloadProgress = mutableMapOf<PackageComponent, Long>()

    installPackagesInternal(
        packageName = packageName,
        componentNames = components.map { it.componentName },
        isUpdate = isUpdate,
        emitProgress = emitProgress,
    ) { session, fileName, to ->
        val component = components.find { it.componentName == fileName }!!
        Log.v(TAG, "installing $fileName for $packageName from network")
        // Emit progress for the first time as soon as possible, before any network interaction
        emitProgress(session, Downloading(
            bytesDownloaded = downloadProgress.values.sum(),
            bytesTotal = components.sumOf { it.size }
        ))
        httpClient.download(component.url, to) { progress ->
            downloadProgress[component] = progress
            emitProgress(session, Downloading(
                bytesDownloaded = downloadProgress.values.sum(),
                bytesTotal = components.sumOf { it.size }
            ))
        }
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private suspend fun Context.installPackagesInternal(
    packageName: String,
    componentNames: List<String>,
    isUpdate: Boolean = false,
    emitProgress: (session: Int, InstallProgress) -> Unit = { _, _ -> },
    writeComponent: suspend (session: Int, componentName: String, to: OutputStream) -> Unit
) {
    Log.v(TAG, "installPackages start")

    val packageInstaller = packageManager.packageInstaller
    val installed = packageManager.getInstalledPackages(0).any {
        it.applicationInfo.packageName == packageName
    }
    // Contrary to docs, MODE_INHERIT_EXISTING cannot be used if package is not yet installed.
    val params = SessionParams(
        if (!installed || isUpdate) SessionParams.MODE_FULL_INSTALL
        else SessionParams.MODE_INHERIT_EXISTING
    )
    params.setAppPackageName(packageName)
    params.setAppLabel(packageName)
    params.setInstallLocation(PackageInfo.INSTALL_LOCATION_INTERNAL_ONLY)
    try {
        @SuppressLint("PrivateApi") val method = SessionParams::class.java.getDeclaredMethod(
            "setDontKillApp", Boolean::class.javaPrimitiveType
        )
        method.invoke(params, true)
    } catch (e: Exception) {
        Log.w(TAG, "Error setting dontKillApp", e)
    }
    var session: PackageInstaller.Session? = null
    // might throw, but we need no handling here as we don't emit progress beforehand
    val sessionId: Int = packageInstaller.createSession(params)
    try {
        session = packageInstaller.openSession(sessionId)
        for (component in componentNames) {
            session.openWrite(component, 0, -1).use { outputStream ->
                writeComponent(sessionId, component, outputStream)
                session!!.fsync(outputStream)
            }
        }
        val deferred = CompletableDeferred<Unit>()

        SessionResultReceiver.pendingSessions[sessionId] = SessionResultReceiver.OnResult(
            onSuccess = {
                deferred.complete(Unit)
                emitProgress(sessionId, InstallComplete)
                        },
            onFailure = { message ->
                deferred.completeExceptionally(RuntimeException(message))
                emitProgress(sessionId, InstallError(message ?: "UNKNOWN"))
            }
        )

        val intent = Intent(this, SessionResultReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, sessionId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

        emitProgress(sessionId, CommitingSession)
        session.commit(pendingIntent.intentSender)
        // don't abandon if `finally` step is reached after this point
        session = null

        Log.d(TAG, "installPackages session commit")
        return deferred.await()
    } catch (e: IOException) {
        Log.e(TAG, "Error installing packages", e)
        emitProgress(sessionId, InstallError(e.message ?: "UNKNOWN"))
        throw e
    } finally {
        // discard downloaded data
        session?.let {
            Log.d(TAG, "Discarding session after error")
            it.abandon()
        }
    }
}
