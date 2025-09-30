/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.installer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.provider.Settings
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.android.vending.VendingPreferences
import com.android.vending.installer.installPackages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.microg.gms.profile.Build.VERSION.SDK_INT
import org.microg.gms.profile.ProfileManager
import org.microg.gms.utils.getFirstSignatureDigest
import org.microg.gms.vending.AllowType
import org.microg.gms.vending.InstallChannelData
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val REQUEST_INSTALL_PERMISSION = 1001

@RequiresApi(21)
class ChannelInstallActivity : AppCompatActivity() {

    private val callingPackageName: String?
        get() = callingActivity?.packageName

    private val packUris: List<Uri>
        get() {
            val list = if (SDK_INT >= 33) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            }
            if (list != null && !list.isEmpty()) return list
            val streamUri = if (SDK_INT >= 33) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            if (streamUri != null) return listOf(streamUri)
            return listOfNotNull(intent.data)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val installEnabled = VendingPreferences.isInstallEnabled(this)
        if (!installEnabled) {
            return onResult(RESULT_CANCELED, "Install is disabled")
        }
        ProfileManager.ensureInitialized(this)

        if (callingPackageName.isNullOrEmpty()) {
            Log.d(TAG, "onCreate: No calling activity, use startActivityForResult()")
            return onResult(RESULT_CANCELED, "No calling activity")
        }

        if (packUris.isEmpty()) {
            Log.d(TAG, "onCreate: Missing package URI: $intent")
            return onResult(RESULT_CANCELED, "Missing package URI")
        }

        val pkgSignSha256ByteArray = packageManager.getFirstSignatureDigest(callingPackageName!!, "SHA-256")
            ?: return onResult(RESULT_CANCELED, "$callingPackageName request install permission denied: signature is null")

        val pkgSignSha256Base64 = Base64.encodeToString(pkgSignSha256ByteArray, Base64.NO_WRAP)
        Log.d(TAG, "onCreate $callingPackageName pkgSignSha256Base64: $pkgSignSha256Base64")

        val channelInstallList = VendingPreferences.loadChannelInstallList(this)
        val channelDataSet = InstallChannelData.loadChannelDataSet(channelInstallList)

        val callerChannelData = callerToChannelData(channelDataSet, callingPackageName!!, pkgSignSha256Base64)
        if (callerChannelData.allowType == AllowType.REJECT_ALWAYS.value) {
            return onResult(RESULT_CANCELED, "$callingPackageName is not allowed to install")
        }

        val appInfo = extractInstallAppInfo(packUris!!) ?:
            return onResult(RESULT_CANCELED, "Can't extract app information from provided .apk")

        lifecycleScope.launchWhenStarted {
            var callerAllow = callerChannelData.allowType
            if (callerAllow == AllowType.REJECT_ONCE.value || callerAllow == AllowType.ALLOW_ONCE.value) {
                callerAllow = showRequestInstallReminder(appInfo)
            }
            Log.d(TAG, "onCreate: callerPackagePermissionType: $callerAllow")

            val localChannelDataStr = InstallChannelData.updateChannelDataString(channelDataSet, callerChannelData.apply { this.allowType = callerAllow })
            VendingPreferences.updateChannelInstallList(this@ChannelInstallActivity, localChannelDataStr)
            Log.d(TAG, "onCreate: localChannelDataStr: $localChannelDataStr")

            if (callerAllow == AllowType.ALLOW_ALWAYS.value || callerAllow == AllowType.ALLOW_ONCE.value) {
                if (hasInstallPermission()) {
                    Log.d(TAG, "onCreate: hasInstallPermission")
                    handleInstallRequest(appInfo.packageName)
                } else {
                    openInstallPermissionSettings()
                }
                return@launchWhenStarted
            }

            onResult(RESULT_CANCELED, "$callingPackageName request install permission denied", appInfo.packageName)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_INSTALL_PERMISSION) {
            if (hasInstallPermission()) {
                Log.d(TAG, "onCreate: requestInstallPermission granted")
                val appInfo = extractInstallAppInfo(packUris!!) ?: return onResult(RESULT_CANCELED, "File changed while granting permission")
                handleInstallRequest(appInfo.packageName)
            } else {
                onResult(RESULT_CANCELED, "Install Permission denied")
            }
        }
    }

    private fun callerToChannelData(channelDataSet: Set<InstallChannelData>, callingPackage: String, pkgSignSha256: String): InstallChannelData {
        if (channelDataSet.isEmpty() || channelDataSet.none { it.packageName == callingPackage && it.pkgSignSha256 == pkgSignSha256 }) {
            return InstallChannelData(callingPackage, AllowType.REJECT_ONCE.value, pkgSignSha256)
        }
        val channelData = channelDataSet.first { it.packageName == callingPackage && it.pkgSignSha256 == pkgSignSha256 }
        return channelData
    }

    private fun handleInstallRequest(installPackageName: String) {
        lifecycleScope.launch {
            val isSuccess = runCatching {
                withContext(Dispatchers.IO) {
                    installPackages(
                        packageName = installPackageName,
                        componentFiles = uriToApkFiles(packUris!!),
                        isUpdate = true
                    )
                }
            }.isSuccess
            Log.d(TAG, "handleInstallRequest: installPackages<$installPackageName> isSuccess: $isSuccess")
            if (isSuccess) {
                onResult(RESULT_OK, installPackageName = installPackageName)
            } else {
                onResult(RESULT_CANCELED, "Install failed")
            }
        }
    }

    private suspend fun showRequestInstallReminder(appInfo: InstallAppInfo) = suspendCoroutine { con ->
        val intent = Intent(this, AskInstallReminderActivity::class.java)
        intent.putExtra(EXTRA_MESSENGER, Messenger(object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                con.resume(msg.what)
            }
        }))
        intent.putExtra(EXTRA_CALLER_PACKAGE, callingPackageName)
        intent.putExtra(EXTRA_INSTALL_PACKAGE_NAME, appInfo.packageName)
        intent.putExtra(EXTRA_INSTALL_PACKAGE_LABEL, appInfo.appLabel)
        intent.putExtra(EXTRA_INSTALL_PACKAGE_ICON, appInfo.appIcon?.toByteArrayOrNull())
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun openInstallPermissionSettings() {
        Log.d(TAG, "openInstallPermissionSettings: request ")
        val intent = if (SDK_INT >= 26) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = "package:$packageName".toUri()
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        }
        startActivityForResult(intent, REQUEST_INSTALL_PERMISSION)
    }

    private fun onResult(result: Int = RESULT_OK, error: String? = null, installPackageName: String? = null) {
        Log.d(TAG, "onResult: error: $error ")
        sendBroadcastReceiver(callingPackageName, installPackageName, result, error)
        setResult(result, Intent().apply { putExtra("error", error) })
        finishAndRemoveTask()
    }
}