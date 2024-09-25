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
) { fileName, to ->
    val component = componentFiles.find { it.name == fileName }!!
    FileInputStream(component).use { it.copyTo(to) }
    component.delete()
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal suspend fun Context.installPackagesFromNetwork(
    packageName: String,
    components: List<PackageComponent>,
    httpClient: HttpClient = HttpClient(),
    isUpdate: Boolean = false
) = installPackagesInternal(
    packageName = packageName,
    componentNames = components.map { it.componentName },
    isUpdate = isUpdate
) { fileName, to ->
    val component = components.find { it.componentName == fileName }!!
    Log.v(TAG, "installing $fileName for $packageName from network")
    httpClient.download(component.url, to)
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private suspend fun Context.installPackagesInternal(
    packageName: String,
    componentNames: List<String>,
    isUpdate: Boolean = false,
    writeComponent: suspend (componentName: String, to: OutputStream) -> Unit
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
    params.setAppLabel(packageName + "Subcontracting")
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
        sessionId = packageInstaller.createSession(params)
        session = packageInstaller.openSession(sessionId)
        componentNames.forEach { component ->
            session.openWrite(component, 0, -1).use { outputStream ->
                writeComponent(component, outputStream)
                session.fsync(outputStream)
            }
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
