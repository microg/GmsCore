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
import kotlinx.coroutines.CompletableDeferred
import java.io.File
import java.io.FileInputStream
import java.io.IOException

@RequiresApi(Build.VERSION_CODES.M)
internal suspend fun Context.installPackages(
    callingPackage: String,
    componentFiles: List<File>,
    isUpdate: Boolean = false
) {
    Log.v(TAG, "installPackages start")

    val packageInstaller = packageManager.packageInstaller
    val installed = packageManager.getInstalledPackages(0).any {
        it.applicationInfo.packageName == callingPackage
    }
    // Contrary to docs, MODE_INHERIT_EXISTING cannot be used if package is not yet installed.
    val params = SessionParams(
        if (!installed || isUpdate) SessionParams.MODE_FULL_INSTALL
        else SessionParams.MODE_INHERIT_EXISTING
    )
    params.setAppPackageName(callingPackage)
    params.setAppLabel(callingPackage + "Subcontracting")
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
    var totalDownloaded = 0L
    try {
        sessionId = packageInstaller.createSession(params)
        session = packageInstaller.openSession(sessionId)
        componentFiles.forEach { file ->
            session.openWrite(file.name, 0, -1).use { outputStream ->
                FileInputStream(file).use { inputStream -> inputStream.copyTo(outputStream) }
                session.fsync(outputStream)
            }
            totalDownloaded += file.length()
            file.delete()
        }
        val deferred = CompletableDeferred<Unit>()

        SessionResultReceiver.pendingSessions[sessionId] = SessionResultReceiver.OnResult(
            onSuccess = { deferred.complete(Unit) },
            onFailure = { message -> deferred.completeExceptionally(RuntimeException(message)) }
        )

        val intent = Intent(this, SessionResultReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, sessionId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        session.commit(pendingIntent.intentSender)

        Log.d(TAG, "installPackages session commit")
        return deferred.await()
    } catch (e: IOException) {
        Log.w(TAG, "Error installing packages", e)
        throw e
    } finally {
        session?.close()
    }
}
