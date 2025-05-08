package com.android.vending.installer

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.pm.PackageInfoCompat
import com.google.android.finsky.splitinstallservice.PackageComponent
import com.google.android.finsky.splitinstallservice.SplitInstallService
import kotlinx.coroutines.CompletableDeferred
import org.microg.vending.billing.core.HttpClient
import org.microg.vending.enterprise.CommitingSession
import org.microg.vending.enterprise.Downloading
import org.microg.vending.enterprise.InstallComplete
import org.microg.vending.enterprise.InstallError
import org.microg.vending.enterprise.InstallProgress
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.ArrayList

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal suspend fun Context.installPackages(
        packageName: String,
        componentFiles: List<File>,
        isUpdate: Boolean = false
) {
    val notifyId = createNotificationId(packageName, emptyList())
    installPackagesInternal(
            packageName = packageName,
            componentNames = componentFiles.map { it.name },
            notifyId = notifyId,
            isUpdate = isUpdate
    ) {_, notifyId, fileName, to ->
        val component = componentFiles.find { it.name == fileName }!!
        FileInputStream(component).use { it.copyTo(to) }
        component.delete()
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal suspend fun Context.installPackagesFromNetwork(
        packageName: String,
        components: List<PackageComponent>,
        httpClient: HttpClient = HttpClient(),
        isUpdate: Boolean = false,
        emitProgress: (notifyId: Int, InstallProgress) -> Unit = { _, _ -> }
) {

    val downloadProgress = mutableMapOf<PackageComponent, Long>()
    val versionTempDir = this.createInstallTempDir(packageName)
    //Generate a notifyId based on the package name and download module to prevent multiple notifications from appearing when the download content is the same
    val notifyId = createNotificationId(packageName, components)
    installPackagesInternal(
            packageName = packageName,
            componentNames = components.map { it.componentName },
            notifyId = notifyId,
            isUpdate = isUpdate,
            emitProgress = emitProgress,
    ) {tempFiles, notifyId, fileName, to ->
        val component = components.find { it.componentName == fileName }!!

        // Create a temporary file to store the downloaded APK (as a subdirectory based on versionCode)
        val tempFile = File(versionTempDir, "$fileName.apk")
        val downloadedBytes = if (tempFile.exists()) tempFile.length() else 0L
        Log.v(TAG, "installing $fileName for $packageName from network apk size:" + component.size + " downloaded: " + downloadedBytes)
        if (downloadedBytes < component.size) {
            httpClient.download(component.url, FileOutputStream(tempFile, downloadedBytes > 0), downloadedBytes = downloadedBytes) { progress ->
                downloadProgress[component] = progress
                emitProgress(notifyId, Downloading(
                        bytesDownloaded = downloadProgress.values.sum(),
                        bytesTotal = components.sumOf { it.size }
                ))
            }
        }
        tempFiles.add(tempFile.absolutePath)
        tempFile.inputStream().use { inputStream ->
            inputStream.copyTo(to)
        }

    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private suspend fun Context.installPackagesInternal(
        packageName: String,
        componentNames: List<String>,
        notifyId: Int,
        isUpdate: Boolean = false,
        emitProgress: (notifyId: Int, InstallProgress) -> Unit = { _, _ -> },
        writeComponent: suspend (tempFiles:MutableList<String>, notifyId: Int, componentName: String, to: OutputStream) -> Unit
) {
    Log.v(TAG, "installPackages start")
    Log.d(TAG, "installPackagesInternal: ${this is SplitInstallService}")
    val packageInstaller = packageManager.packageInstaller
    // Contrary to docs, MODE_INHERIT_EXISTING cannot be used if package is not yet installed.
    val params = SessionParams(
            if (isUpdate) SessionParams.MODE_FULL_INSTALL
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
    val sessionId: Int
    var session: PackageInstaller.Session? = null
    try {
        sessionId = packageInstaller.createSession(params)
        session = packageInstaller.openSession(sessionId)
        val tempFiles = mutableListOf<String>()
        for (component in componentNames) {
            session.openWrite(component, 0, -1).use { outputStream ->
                try {
                    writeComponent(tempFiles, notifyId, component, outputStream)
                    session!!.fsync(outputStream)
                } catch (e: Exception) {
                    Log.w(TAG, "Error writing component notifyId $notifyId")
                    emitProgress(notifyId, InstallError("Download Error"))
                    throw e
                }
            }
        }
        val deferred = CompletableDeferred<Unit>()
        Log.e(TAG, "installPackagesInternal pendingSessions size: ${SessionResultReceiver.pendingSessions.size}")
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
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val intent = Intent(this, SessionResultReceiver::class.java)
        intent.putStringArrayListExtra(SessionResultReceiver.KEY_TEMP_FILES, ArrayList(tempFiles))
        intent.putExtra(SessionResultReceiver.KEY_NOTIFY_ID, notifyId)
        val pendingIntent = PendingIntent.getBroadcast(
                this, sessionId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        emitProgress(notifyId, CommitingSession(createPendingIntent(VENDING_INSTALL_ACTION, sessionId, pendingIntent)
                , createPendingIntent(VENDING_INSTALL_DELETE_ACTION, sessionId, null)))
        if (!keyguardManager.isKeyguardLocked) {
            session.commit(pendingIntent.intentSender)
            // don't abandon if `finally` step is reached after this point
            session = null

            Log.d(TAG, "installPackages session commit")
            return deferred.await()
        } else {
            Log.d(TAG, "installPackagesInternal: The screen is locked and waiting for the user to click the notification to install")
            // don't abandon if `finally` step is reached after this point
            session = null
            throw Exception("The device screen is off, waiting for installation")
        }
    } catch (e: IOException) {
        Log.w(TAG, "Error installing packages", e)
        throw e
    } finally {
        // discard downloaded data
        session?.let {
            Log.d(TAG, "Discarding session after error")
            it.abandon()
        }
    }
}

private fun Context.createInstallTempDir(packageName: String) : File {
    val versionCode =PackageInfoCompat.getLongVersionCode(
            packageManager.getPackageInfo(packageName, 0)
    )

    val tempDir = File(cacheDir, "temp_apk").apply {
        if (!exists()) mkdirs()
    }

    val packageTempDir = File(tempDir, packageName).apply {
        if (!exists()) mkdirs()
    }

    val versionTempDir = File(packageTempDir, versionCode.toString()).apply {
        if (!exists()) mkdirs()
    }
    return versionTempDir
}


private fun createNotificationId(packageName: String, components: List<PackageComponent>) : Int{
    val hash = (packageName + components.joinToString("") { it.componentName }).hashCode()
    return hash and Int.MAX_VALUE
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private fun Context.createPendingIntent(action: String, sessionId: Int, pendingIntent: PendingIntent? = null): PendingIntent {
    val installIntent = Intent(this.applicationContext, InstallReceiver::class.java).apply {
        this.action = action
        putExtra(SESSION_ID, sessionId)
        if (pendingIntent != null) {
            putExtra(SESSION_RESULT_RECEIVER_INTENT, pendingIntent)
        }
    }

    val pendingInstallIntent = PendingIntent.getBroadcast(
            this.applicationContext,
            0,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    return pendingInstallIntent
}
