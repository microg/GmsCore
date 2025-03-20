/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.finsky.splitinstallservice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.pm.PackageInfoCompat
import com.android.vending.installer.KEY_BYTES_DOWNLOADED
import com.android.vending.installer.installPackagesFromNetwork
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.vending.billing.AuthManager
import org.microg.vending.billing.core.HttpClient
import org.microg.vending.delivery.requestDownloadUrls
import org.microg.vending.enterprise.Downloading
import org.microg.vending.splitinstall.SPLIT_LANGUAGE_TAG
import org.microg.vending.ui.notifySplitInstallProgress

private const val KEY_LANGUAGE = "language"
private const val KEY_LANGUAGES = "languages"
private const val KEY_MODULE_NAME = "module_name"
private const val KEY_TOTAL_BYTES_TO_DOWNLOAD = "total_bytes_to_download"
private const val KEY_STATUS = "status"
private const val KEY_ERROR_CODE = "error_code"
private const val KEY_SESSION_ID = "session_id"
private const val KEY_SESSION_STATE = "session_state"

private const val ACTION_UPDATE_SERVICE = "com.google.android.play.core.splitinstall.receiver.SplitInstallUpdateIntentService"

private const val TAG = "SplitInstallManager"

class SplitInstallManager(val context: Context) {

    private var httpClient: HttpClient = HttpClient()

    suspend fun splitInstallFlow(callingPackage: String, splits: List<Bundle>): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
//        val callingPackage = runCatching { PackageUtils.getAndCheckCallingPackage(context, packageName) }.getOrNull() ?: return
        if (splits.all { it.getString(KEY_LANGUAGE) == null && it.getString(KEY_MODULE_NAME) == null }) return false
        Log.v(TAG, "splitInstallFlow: start")

        val packagesToDownload = splits.mapNotNull { split ->
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


        val components = runCatching {
            httpClient.requestDownloadUrls(
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

        components.forEach {
            splitInstallRecord[it] = DownloadStatus.PENDING
        }

        val success = runCatching {

            var lastNotification = 0L
            context.installPackagesFromNetwork(
                packageName = callingPackage,
                components = components,
                httpClient = httpClient,
                isUpdate = false
            ) { session, progress ->
                // Android rate limits notification updates by some vague rule of "not too many in less than one second"
                if (progress !is Downloading || lastNotification + 250 < System.currentTimeMillis()) {
                    context.notifySplitInstallProgress(callingPackage, session, progress)
                    lastNotification = System.currentTimeMillis()
                }
            }
        }.isSuccess

        return if (success) {
            sendCompleteBroad(context, callingPackage, components.sumOf { it.size })
            components.forEach { splitInstallRecord[it] = DownloadStatus.COMPLETE }
            true
        } else {
            components.forEach { splitInstallRecord[it] = DownloadStatus.FAILED }
            false
        }
    }

    /**
     * Tests if a split apk has already been requested in this session. Returns true if it is
     * pending or downloaded, and returns false if download failed or it is not yet known.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun shouldDownload(callingPackage: String, splitName: String): Boolean {
        return splitInstallRecord.keys.find { it.packageName == callingPackage && it.componentName == splitName }
            ?.let {
                splitInstallRecord[it] == DownloadStatus.FAILED
        } ?: true
    }

    private fun sendCompleteBroad(context: Context, packageName: String, bytes: Long) {
        Log.d(TAG, "sendCompleteBroadcast: $bytes bytes")
        val extra = Bundle().apply {
            putInt(KEY_STATUS, 5)
            putInt(KEY_ERROR_CODE, 0)
            putInt(KEY_SESSION_ID, 0)
            putLong(KEY_TOTAL_BYTES_TO_DOWNLOAD, bytes)
            //putString(KEY_LANGUAGES, intent.getStringExtra(KEY_LANGUAGE))
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
