/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.installer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.provider.Settings
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
import org.microg.gms.vending.AllowType
import org.microg.gms.vending.InstallChannelData
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val REQUEST_INSTALL_PERMISSION = 1001

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ChannelInstallActivity : AppCompatActivity() {

    private val callingPackageName: String?
        get() = runCatching {
            intent?.extras?.getString(EXTRA_CALLER_PACKAGE)
        }.getOrNull()

    private val installPackageName: String?
        get() = runCatching {
            intent?.extras?.getString(EXTRA_INSTALL_PACKAGE)
        }.getOrNull()

    private val packUris: ArrayList<Uri>?
        get() = runCatching {
            if (SDK_INT >= 33) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            }
        }.getOrNull()

    private var callerPackagePermissionType = AllowType.ALLOWED_REQUEST.value

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val installEnabled = VendingPreferences.isInstallEnabled(this)
        if (!installEnabled) {
            Log.d(TAG, "onCreate: Install is disabled ")
            return onResult(error = "Install is disabled")
        }
        ProfileManager.ensureInitialized(this)
        if (packUris.isNullOrEmpty() || callingPackageName.isNullOrEmpty() || installPackageName.isNullOrEmpty()) {
            Log.d(TAG, "onCreate: Missing $packUris or $callingPackageName or $installPackageName")
            return onResult(error = "Missing parameters")
        }
        callerPackagePermissionType = checkCallerInstallPermission(callingPackageName!!)
        if (callerPackagePermissionType == AllowType.ALLOWED_NEVER.value) {
            return onResult(error = "$callingPackageName is not allowed to install")
        }
        lifecycleScope.launch {
            if (callerPackagePermissionType == AllowType.ALLOWED_REQUEST.value || callerPackagePermissionType == AllowType.ALLOWED_SINGLE.value) {
                callerPackagePermissionType = showRequestInstallReminder()
            }
            InstallChannelData.updateLocalChannelData(callingPackageName!!, callerPackagePermissionType).let {
                VendingPreferences.updateChannelInstallList(this@ChannelInstallActivity, it)
            }
            if (callerPackagePermissionType == AllowType.ALLOWED_ALWAYS.value || callerPackagePermissionType == AllowType.ALLOWED_SINGLE.value) {
                if (hasInstallPermission()) {
                    Log.d(TAG, "onCreate: hasInstallPermission")
                    handleInstallRequest()
                } else {
                    openInstallPermissionSettings()
                }
                return@launch
            }
            onResult(error = "$callingPackageName request install permission denied")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_INSTALL_PERMISSION) {
            if (hasInstallPermission()) {
                Log.d(TAG, "onCreate: requestInstallPermission granted")
                handleInstallRequest()
            } else {
                onResult(error = "Install Permission denied")
            }
        }
    }

    private fun checkCallerInstallPermission(callingPackage: String): Int {
        val channelDataSet = InstallChannelData.loadChannelDataSet(VendingPreferences.loadChannelInstallList(this))
        if (channelDataSet.isEmpty() || channelDataSet.none { it.packageName == callingPackage }) {
            return AllowType.ALLOWED_REQUEST.value
        }
        val channelData = channelDataSet.first { it.packageName == callingPackage }
        return channelData.allowed
    }

    private fun handleInstallRequest() {
        lifecycleScope.launch {
            val isSuccess = runCatching {
                withContext(Dispatchers.IO) {
                    installPackages(installPackageName!!, uriToApkFiles(packUris!!))
                }
            }.isSuccess
            Log.d(TAG, "handleInstallRequest: installPackages<$installPackageName> isSuccess: $isSuccess")
            if (isSuccess) {
                onResult()
            } else {
                onResult(error = "Install failed")
            }
        }
    }

    private suspend fun showRequestInstallReminder() = suspendCoroutine { con ->
        val appInfoFromUris = extractInstallAppInfo(installPackageName!!, packUris!!.first())
        val intent = Intent(this, AskInstallReminderActivity::class.java)
        intent.putExtra(EXTRA_MESSENGER, Messenger(object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                con.resume(msg.what)
            }
        }))
        intent.putExtra(EXTRA_CALLER_PACKAGE, callingPackageName)
        intent.putExtra(EXTRA_INSTALL_PACKAGE, installPackageName)
        appInfoFromUris?.first?.let { intent.putExtra(EXTRA_INSTALL_PACKAGE_NAME, it) }
        appInfoFromUris?.second?.let { intent.putExtra(EXTRA_INSTALL_PACKAGE_ICON, it) }
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

    private fun onResult(result: Int = RESULT_OK, error: String? = null) {
        Log.d(TAG, "onResult: error: $error ")
        sendBroadcastReceiver(callingPackageName, installPackageName, result, error)
        setResult(result, Intent().apply { putExtra("error", error) })
        finish()
    }
}