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
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import org.microg.gms.profile.Build.VERSION.SDK_INT
import java.io.ByteArrayOutputStream
import java.io.File

const val TAG = "ChannelInstall"
const val EXTRA_MESSENGER = "messenger"
const val EXTRA_CALLER_PACKAGE = "calling_package"
const val EXTRA_INSTALL_PACKAGE = "installing_app_package"
const val EXTRA_INSTALL_PACKAGE_ICON = "installPackageIcon"
const val EXTRA_INSTALL_PACKAGE_NAME = "installPackageName"
const val INSTALL_RESULT_RECV_ACTION = "com.android.vending.install.PACAKGE"
const val SOURCE_PACKAGE = "source_package"

fun Context.hasInstallPermission() = if (SDK_INT >= 26) {
    packageManager.canRequestPackageInstalls()
} else {
    true
}

fun Context.extractInstallAppInfo(installPackage: String, uri: Uri): Pair<String, ByteArray?>? {
    val tempFile = File.createTempFile("temp_apk_", "$installPackage.apk", cacheDir).apply {
        contentResolver.openInputStream(uri)?.use { input ->
            outputStream().use { output -> input.copyTo(output) }
        }
    }
    val result = packageManager.getPackageArchiveInfo(tempFile.absolutePath, PackageManager.GET_META_DATA)?.let {
        val appName = it.applicationInfo?.loadLabel(packageManager).toString()
        val appIcon = it.applicationInfo?.loadIcon(packageManager)?.toByteArrayOrNull()
        Pair(appName, appIcon)
    }
    tempFile.delete()
    return result
}

fun Context.uriToApkFiles(uriList: ArrayList<Uri>): List<File> {
    return uriList.mapIndexedNotNull { uriIndex, uri ->
        File.createTempFile("temp_apk_", "$uriIndex.apk", cacheDir).apply {
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