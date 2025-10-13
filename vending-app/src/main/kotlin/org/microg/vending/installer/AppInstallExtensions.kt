/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.installer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import org.microg.gms.profile.Build.VERSION.SDK_INT
import java.io.ByteArrayOutputStream
import java.io.File

const val TAG = "AppInstall"
const val EXTRA_MESSENGER = "messenger"
const val EXTRA_CALLER_PACKAGE = "calling_package"
const val EXTRA_INSTALL_PACKAGE = "installed_app_package"
const val EXTRA_INSTALL_PACKAGE_ICON = "installPackageIcon"
const val EXTRA_INSTALL_PACKAGE_NAME = "installPackageName"
const val EXTRA_INSTALL_PACKAGE_LABEL = "installPackageLabel"
const val INSTALL_RESULT_RECV_ACTION = "com.android.vending.install.PACAKGE"
const val SOURCE_PACKAGE = "source_package"

fun Context.hasInstallPermission() = if (SDK_INT >= 26) {
    packageManager.canRequestPackageInstalls()
} else {
    true
}

data class InstallAppInfo(val packageName: String, val appLabel: String, val appIcon: Drawable?)

fun Context.extractInstallAppInfo(uris: List<Uri>): InstallAppInfo? {
    var packageName: String? = null
    var appLabel: String? = null
    var appIcon: Drawable? = null
    for (item in uris) {
        var tempFile: File? = null
        try {
            tempFile = File.createTempFile("temp_apk_", ".apk", cacheDir).apply {
                contentResolver.openInputStream(item)?.use { input ->
                    outputStream().use { output -> input.copyTo(output) }
                }
            }
            val packageInfo = if (SDK_INT >= 33) {
                packageManager.getPackageArchiveInfo(tempFile.absolutePath, PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageArchiveInfo(tempFile.absolutePath, PackageManager.GET_META_DATA)
            } ?: continue
            Log.d(TAG, "Package: $packageInfo, App: ${packageInfo.applicationInfo}")
            if (packageName != null && packageInfo.packageName != packageName) {
                Log.w(TAG, "Inconsistent packages")
                return null
            }
            packageName = packageInfo.packageName
            val appInfo = packageInfo.applicationInfo.apply {
                this?.sourceDir = tempFile.absolutePath
                this?.publicSourceDir = tempFile.absolutePath
            } ?: continue
            val thisAppLabel = packageManager.getApplicationLabel(appInfo).toString()
            Log.d(TAG, "Got app label: $thisAppLabel")
            if (thisAppLabel != packageName && thisAppLabel.isNotBlank()) appLabel = thisAppLabel
            appIcon = packageManager.getApplicationIcon(appInfo)
            if (appLabel != null) break
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract app info: ${e.message}", e)
        } finally {
            tempFile?.delete()
        }
    }
    if (packageName != null) {
        return InstallAppInfo(packageName, appLabel ?: packageName, appIcon)
    }
    return null
}

fun Context.uriToApkFiles(uriList: List<Uri>): List<File> {
    return uriList.mapIndexedNotNull { uriIndex, uri ->
        File.createTempFile("temp_apk_", ".$uriIndex.apk", cacheDir).apply {
            contentResolver.openInputStream(uri)?.use { input ->
                outputStream().use { output -> input.copyTo(output) }
            }
        }
    }
}

fun Drawable.toByteArrayOrNull(): ByteArray? = runCatching {
    val bitmap = if (this is BitmapDrawable) {
        this.bitmap
    } else {
        createBitmap(intrinsicWidth, intrinsicHeight).also { bmp ->
            val canvas = Canvas(bmp)
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
        }
    }

    ByteArrayOutputStream().use { outputStream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        if (this !is BitmapDrawable) {
            bitmap.recycle()
        }
        outputStream.toByteArray()
    }
}.onFailure { e ->
    Log.w(TAG, "Failed to convert Drawable to ByteArray: ${e.message}", e)
}.getOrNull()

fun ByteArray.toDrawableOrNull(context: Context): Drawable? = runCatching {
    val bitmap = BitmapFactory.decodeByteArray(this, 0, size)
    bitmap.toDrawable(context.resources)
}.onFailure { e ->
    Log.w(TAG, "Failed to convert ByteArray to Drawable: ${e.message}", e)
}.getOrNull()

@RequiresApi(21)
fun Context.sendBroadcastReceiver(callingPackage: String?, installingPackage: String?, status: Int = 0, statusMessage: String? = null, sessionId: Int = 0) {
    try {
        Log.d(TAG, "transform broadcast to caller app start : $callingPackage, status: $status, sessionId:${sessionId}")
        if (callingPackage.isNullOrEmpty() || installingPackage.isNullOrEmpty()) {
            return
        }
        val forwardIntent = Intent(INSTALL_RESULT_RECV_ACTION).apply {
            putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionId)
            putExtra(PackageInstaller.EXTRA_STATUS, status)
            putExtra(PackageInstaller.EXTRA_STATUS_MESSAGE, statusMessage)
            putExtra(SOURCE_PACKAGE, packageName)
            putExtra(EXTRA_INSTALL_PACKAGE, installingPackage)
            setPackage(callingPackage)
        }
        sendBroadcast(forwardIntent)
        Log.d(TAG, "transform broadcast to caller app end: $callingPackage, status: $status, sessionId:${sessionId}")
    } catch (e: Exception) {
        Log.d(TAG, "error:${e.message}")
    }
}