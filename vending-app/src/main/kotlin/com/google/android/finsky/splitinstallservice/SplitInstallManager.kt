/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.finsky.splitinstallservice

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.collection.arraySetOf
import androidx.core.content.pm.PackageInfoCompat
import com.android.vending.installer.KEY_BYTES_DOWNLOADED
import com.android.vending.installer.SPLIT_LANGUAGE_TAG
import com.android.vending.installer.installPackagesFromNetwork
import com.google.android.finsky.syncDeviceInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.microg.vending.billing.AuthManager
import org.microg.vending.billing.core.HttpClient
import org.microg.vending.delivery.requestDownloadUrls
import org.microg.vending.enterprise.Downloading
import org.microg.vending.ui.notifySplitInstallProgress

private const val KEY_LANGUAGE = "language"
private const val KEY_LANGUAGES = "languages"
private const val KEY_MODULE_NAME = "module_name"
private const val KEY_TOTAL_BYTES_TO_DOWNLOAD = "total_bytes_to_download"
private const val KEY_STATUS = "status"
private const val KEY_ERROR_CODE = "error_code"
private const val KEY_SESSION_ID = "session_id"
private const val KEY_MODULE_NAMES = "module_names"
private const val KEY_SESSION_STATE = "session_state"

private const val ACTION_UPDATE_SERVICE = "com.google.android.play.core.splitinstall.receiver.SplitInstallUpdateIntentService"

private const val TAG = "SplitInstallManager"

class SplitInstallManager(val context: Context) {

    private var httpClient: HttpClient = HttpClient()
    private val mutex = Mutex()

    suspend fun splitInstallFlow(callingPackage: String, splits: List<Bundle>): Boolean {
        var packagesToDownload: List<String> = listOf()
        var components:List<PackageComponent>? = null
        mutex.withLock {
            if (SDK_INT < 23) return false
//        val callingPackage = runCatching { PackageUtils.getAndCheckCallingPackage(context, packageName) }.getOrNull() ?: return
            if (splits.all { it.getString(KEY_LANGUAGE) == null && it.getString(KEY_MODULE_NAME) == null }) return false
            Log.v(TAG, "splitInstallFlow: start")

            packagesToDownload = splits.mapNotNull { split ->
                split.getString(KEY_LANGUAGE)?.let { "$SPLIT_LANGUAGE_TAG$it" }
                        ?: split.getString(KEY_MODULE_NAME)
            }.filter { shouldDownload(callingPackage, it) }

            Log.v(TAG, "splitInstallFlow will query for these packages: $packagesToDownload")
            if (packagesToDownload.isEmpty()) return false


            val authData = runCatching { withContext(Dispatchers.IO) {
                AuthManager.getAuthData(context)
            } }.getOrNull()
            Log.v(TAG, "splitInstallFlow oauthToken: $authData")
            if (authData?.authToken.isNullOrEmpty()) return false
            authData!!

            components = runCatching {
                kotlin.runCatching {
                    //Synchronize account device information to prevent failure to obtain sub-package download information
                    syncDeviceInfo(context, AccountManager.get(context).accounts.firstOrNull { it.name == authData.email }!!, authData.authToken, authData.gsfId.toLong(16))
                }
                httpClient.requestDownloadUrls(
                        context = context,
                        packageName = callingPackage,
                        versionCode = PackageInfoCompat.getLongVersionCode(
                                context.packageManager.getPackageInfo(callingPackage, 0)
                        ),
                        auth = authData,
                        requestSplitPackages = packagesToDownload
                )
            }.getOrNull()

            Log.v(TAG, "splitInstallFlow requestDownloadUrls returned these components: $components")
            if (components.isNullOrEmpty()) {
                return false
            }
            components!!.forEach {
                splitInstallRecord[it] = DownloadStatus.PENDING
            }
        }

        val success = runCatching {

            var lastNotification = 0L
            context.installPackagesFromNetwork(
                    packageName = callingPackage,
                    components = components!!,
                    httpClient = httpClient,
                    isUpdate = false,
                    splitInstall = true,
            ) { notifyId, progress ->
                // Android rate limits notification updates by some vague rule of "not too many in less than one second"
                if (progress !is Downloading || lastNotification + 250 < System.currentTimeMillis()) {
                    context.notifySplitInstallProgress(callingPackage, notifyId, progress)
                    lastNotification = System.currentTimeMillis()
                }
            }
        }.isSuccess

        return if (success) {
            sendCompleteBroad(context, callingPackage, components!!.sumOf { it.size }, packagesToDownload)
            components!!.forEach { splitInstallRecord[it] = DownloadStatus.COMPLETE }
            true
        } else {
            components!!.forEach { splitInstallRecord[it] = DownloadStatus.FAILED }
            false
        }
    }

    /**
     * Tests if a split apk has already been requested in this session. Returns true if it is
     * pending or downloaded, and returns false if download failed or it is not yet known.
     */
    @RequiresApi(23)
    private fun shouldDownload(callingPackage: String, splitName: String): Boolean {
        return splitInstallRecord.keys.find { it.packageName == callingPackage && it.componentName == splitName }
                ?.let {
                    splitInstallRecord[it] == DownloadStatus.FAILED
                } ?: true
    }

    private fun sendCompleteBroad(context: Context, packageName: String, bytes: Long, moduleList: List<String>) {
        Log.d(TAG, "sendCompleteBroadcast: $bytes bytes splits:$moduleList")
        val moduleNames = arraySetOf<String>()
        val languages = arraySetOf<String>()
        moduleList?.forEach {
            if (it.startsWith(SPLIT_LANGUAGE_TAG)) {
                languages.add(it)
            } else {
                moduleNames.add(it)
            }
        }
        Log.d(TAG, "sendInstallCompleteBroad: moduleNames -> $moduleNames languages -> $languages")
        val extra = Bundle().apply {
            putInt(KEY_STATUS, 5)
            putInt(KEY_ERROR_CODE, 0)
            putInt(KEY_SESSION_ID, 0)
            putLong(KEY_TOTAL_BYTES_TO_DOWNLOAD, bytes)
            if (languages.isNotEmpty()) {
                putStringArrayList(KEY_LANGUAGES, ArrayList(languages))
            }
            if (moduleNames.isNotEmpty()) {
                putStringArrayList(KEY_MODULE_NAMES, ArrayList(moduleNames))
            }
            putLong(KEY_BYTES_DOWNLOADED, bytes)
        }

        val broadcastIntent = Intent(ACTION_UPDATE_SERVICE).apply {
            setPackage(packageName)
            putExtra(KEY_SESSION_STATE, extra)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        context.sendBroadcast(broadcastIntent)
    }

    fun release() {
        splitInstallRecord.clear()
        deferredMap.clear()
    }

    companion object {
        // Installation records, including (sub)package name, download path, and installation status
        internal val splitInstallRecord: MutableMap<PackageComponent, DownloadStatus> = mutableMapOf()
        private val deferredMap = mutableMapOf<Int, CompletableDeferred<Intent>>()
    }
}
