/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.finsky.splitinstallservice

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AuthenticatorException
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import com.android.vending.AUTH_TOKEN_SCOPE
import com.android.vending.R
import com.android.vending.buildRequestHeaders
import com.android.vending.getAuthToken
import com.android.vending.installer.KEY_BYTES_DOWNLOADED
import com.android.vending.installer.installPackages
import com.android.vending.installer.packageDownloadLocation
import com.google.android.finsky.GoogleApiResponse
import com.google.android.finsky.splitinstallservice.SplitInstallManager.Companion.deferredMap
import com.google.android.finsky.splitinstallservice.SplitInstallManager.InstallResultReceiver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.microg.vending.billing.DEFAULT_ACCOUNT_TYPE
import org.microg.vending.billing.core.GooglePlayApi.Companion.URL_DELIVERY
import org.microg.vending.billing.core.HttpClient
import java.io.File
import java.io.FileInputStream
import java.io.IOException

private const val SPLIT_INSTALL_NOTIFY_ID = 111
private const val SPLIT_INSTALL_REQUEST_TAG = "splitInstallRequestTag"
private const val SPLIT_LANGUAGE_TAG = "config."

private const val NOTIFY_CHANNEL_ID = "splitInstall"
private const val NOTIFY_CHANNEL_NAME = "Split Install"
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

    private var httpClient: HttpClient = HttpClient(context)

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

        val oauthToken = runCatching { withContext(Dispatchers.IO) {
            getOauthToken()
        } }.getOrNull()
        Log.v(TAG, "splitInstallFlow oauthToken: $oauthToken")
        if (oauthToken.isNullOrEmpty()) return false

        notify(callingPackage)

        val components = runCatching { requestDownloadUrls(callingPackage, oauthToken, packagesToDownload) }.getOrNull()
        Log.v(TAG, "splitInstallFlow requestDownloadUrls returned these components: $components")
        if (components.isNullOrEmpty()) {
            NotificationManagerCompat.from(context).cancel(SPLIT_INSTALL_NOTIFY_ID)
            return false
        }

        val intent = downloadAndInstall(callingPackage, components)

        NotificationManagerCompat.from(context).cancel(SPLIT_INSTALL_NOTIFY_ID)
        if (intent == null) { return false }
        sendCompleteBroad(context, callingPackage, intent)
        return true
    }

    internal suspend fun downloadAndInstall(forPackage: String, downloadList: List<PackageComponent>, isUpdate: Boolean = false): Intent? {
        val packageFiles = downloadPackageComponents(context, downloadList)
        val installFiles = packageFiles.map {
            if (it.value == null) {
                Log.w(TAG, "splitInstallFlow download failed, as ${it.key} was not downloaded")
                throw RuntimeException("installSplitPackage downloadSplitPackage has error")
            } else it.value!!
        }
        Log.v(TAG, "splitInstallFlow downloaded success, downloaded ${installFiles.size} files")

        return runCatching {
            installPackages(context, forPackage, installFiles, isUpdate, deferredMap)
        }.getOrNull()

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private suspend fun requestDownloadUrls(callingPackage: String, authToken: String, requestSplitPackages: List<String>): List<PackageComponent> {
        val versionCode = PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(callingPackage, 0))
        val requestUrl =
            StringBuilder("$URL_DELIVERY?doc=$callingPackage&ot=1&vc=$versionCode&bvc=$versionCode" +
                    "&pf=1&pf=2&pf=3&pf=4&pf=5&pf=7&pf=8&pf=9&pf=10&da=4&bda=4&bf=4&fdcf=1&fdcf=2&ch=")
        requestSplitPackages.forEach { requestUrl.append("&mn=").append(it) }

        Log.v(TAG, "requestDownloadUrls start")
        val languages = requestSplitPackages.filter { it.startsWith(SPLIT_LANGUAGE_TAG) }.map { it.replace(SPLIT_LANGUAGE_TAG, "") }
        Log.d(TAG, "requestDownloadUrls languages: $languages")

        val response = httpClient.get(
            url = requestUrl.toString(),
            headers = buildRequestHeaders(authToken, 1, languages).onEach { Log.d(TAG, "key:${it.key}  value:${it.value}") },
            adapter = GoogleApiResponse.ADAPTER
        )
        Log.d(TAG, "requestDownloadUrls end response -> $response")

        val splitPackageResponses = response.response!!.splitReqResult!!.pkgList!!.pkgDownLoadInfo.filter {
            !it.splitPkgName.isNullOrEmpty() && !it.downloadUrl.isNullOrEmpty()
        }

        val components: List<PackageComponent> = splitPackageResponses.mapNotNull { info ->
            requestSplitPackages.firstOrNull {
                it.contains(info.splitPkgName!!)
            }?.let {
                PackageComponent(callingPackage, it, info.downloadUrl!!)
            }
        }

        Log.d(TAG, "requestDownloadUrls end -> $components")

        components.forEach {
            splitInstallRecord[it] = DownloadStatus.PENDING
        }

        return components
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private suspend fun downloadPackageComponents(
        context: Context,
        downloadList: List<PackageComponent>
    ): Map<PackageComponent, File?> = coroutineScope {
        downloadList.map { info ->
            Log.d(TAG, "downloadSplitPackage: $info")
            async {
                info to runCatching {
                    val file = File(context.packageDownloadLocation().toString(), info.componentName)
                    httpClient.download(
                        url = info.url,
                        downloadFile = file,
                        tag = SPLIT_INSTALL_REQUEST_TAG
                    )
                    file
                }.onFailure {
                    Log.w(TAG, "downloadSplitPackage failed to downlaod from url:${info.url} to be saved as `${info.componentName}`", it)
                }.also {
                    splitInstallRecord[info] = if (it.isSuccess) DownloadStatus.COMPLETE else DownloadStatus.FAILED
                }.getOrNull()
            }
        }.awaitAll().associate { it }
    }

    // TODO: use existing code
    private suspend fun getOauthToken(): String {
        val accounts = AccountManager.get(context).getAccountsByType(DEFAULT_ACCOUNT_TYPE)
        var oauthToken: String? = null
        if (accounts.isEmpty()) {
            Log.w(TAG, "No Google account found")
            throw RuntimeException("No Google account found")
        } else for (account: Account in accounts) {
            oauthToken = try {
                getAuthToken(AccountManager.get(context), account, AUTH_TOKEN_SCOPE).getString(AccountManager.KEY_AUTHTOKEN)
            } catch (e: AuthenticatorException) {
                Log.w(TAG, "Could not fetch auth token for account $account")
                null
            }
            if (oauthToken != null) {
                break
            }
        }
        return oauthToken ?: throw RuntimeException("oauthToken is null")
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

    /**
     * Tell user about the ongoing download.
     * TODO: make persistent
     */
    internal fun notify(installForPackage: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(NOTIFY_CHANNEL_ID, NOTIFY_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        val label = try {
            context.packageManager.getPackageInfo(installForPackage, 0).applicationInfo
                .loadLabel(context.packageManager)
        } catch (e: NameNotFoundException) {
            Log.e(TAG, "Couldn't load label for $installForPackage (${e.message}). Is it not installed?")
            return
        }

        NotificationCompat.Builder(context, NOTIFY_CHANNEL_ID).setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.getString(R.string.split_install, label)).setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(
                NotificationCompat.DEFAULT_ALL
            ).build().also {
                notificationManager.notify(SPLIT_INSTALL_NOTIFY_ID, it)
            }
    }


    private fun sendCompleteBroad(context: Context, packageName: String, intent: Intent) {
        Log.d(TAG, "sendCompleteBroadcast: intent:$intent")
        val extra = Bundle().apply {
            putInt(KEY_STATUS, 5)
            putInt(KEY_ERROR_CODE, 0)
            putInt(KEY_SESSION_ID, 0)
            putLong(KEY_TOTAL_BYTES_TO_DOWNLOAD, intent.getLongExtra(KEY_BYTES_DOWNLOADED, 0))
            putString(KEY_LANGUAGES, intent.getStringExtra(KEY_LANGUAGE))
            putLong(KEY_BYTES_DOWNLOADED, intent.getLongExtra(KEY_BYTES_DOWNLOADED, 0))
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
        httpClient.requestQueue.cancelAll(SPLIT_INSTALL_REQUEST_TAG)
        splitInstallRecord.clear()
        deferredMap.clear()
    }

    internal class InstallResultReceiver : BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        override fun onReceive(context: Context, intent: Intent) {
            val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
            val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
            Log.d(TAG, "onReceive status: $status sessionId: $sessionId")
            try {
                when (status) {
                    PackageInstaller.STATUS_SUCCESS -> {
                        Log.d(TAG, "InstallResultReceiver onReceive: install success")
                        if (sessionId != -1) {
                            deferredMap[sessionId]?.complete(intent)
                            deferredMap.remove(sessionId)
                        }
                    }

                    PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                        val extraIntent = intent.extras?.getParcelable(Intent.EXTRA_INTENT) as Intent?
                        extraIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        extraIntent?.run { ContextCompat.startActivity(context, this, null) }
                    }

                    else -> {
                        NotificationManagerCompat.from(context).cancel(SPLIT_INSTALL_NOTIFY_ID)
                        val errorMsg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                        Log.w(TAG, "InstallResultReceiver onReceive: install fail -> $errorMsg")
                        if (sessionId != -1) {
                            deferredMap[sessionId]?.completeExceptionally(RuntimeException("install fail -> $errorMsg"))
                            deferredMap.remove(sessionId)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error handling install result", e)
                if (sessionId != -1) {
                    deferredMap[sessionId]?.completeExceptionally(e)
                }
            }
        }
    }

    companion object {
        // Installation records, including (sub)package name, download path, and installation status
        internal val splitInstallRecord: MutableMap<PackageComponent, DownloadStatus> = mutableMapOf()
        private val deferredMap = mutableMapOf<Int, CompletableDeferred<Intent>>()
    }
}
