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
import android.os.Build
import android.os.Bundle
import android.util.ArraySet
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.collection.arraySetOf
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import com.android.vending.AUTH_TOKEN_SCOPE
import com.android.vending.R
import com.android.vending.buildRequestHeaders
import com.android.vending.getAuthToken
import com.google.android.finsky.GoogleApiResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.microg.vending.billing.DEFAULT_ACCOUNT_TYPE
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
private const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"
private const val KEY_TOTAL_BYTES_TO_DOWNLOAD = "total_bytes_to_download"
private const val KEY_STATUS = "status"
private const val KEY_ERROR_CODE = "error_code"
private const val KEY_SESSION_ID = "session_id"
private const val KEY_SESSION_STATE = "session_state"

private const val STATUS_UNKNOWN = -1
private const val STATUS_DOWNLOADING = 0
private const val STATUS_DOWNLOADED = 1

private const val ACTION_UPDATE_SERVICE = "com.google.android.play.core.splitinstall.receiver.SplitInstallUpdateIntentService"

private const val FILE_SAVE_PATH = "phonesky-download-service"
private const val TAG = "SplitInstallManager"

class SplitInstallManager(val context: Context) {

    private var httpClient: HttpClient = HttpClient(context)

    suspend fun startInstall(callingPackage: String, splits: List<Bundle>): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
//        val callingPackage = runCatching { PackageUtils.getAndCheckCallingPackage(context, packageName) }.getOrNull() ?: return
        if (splits.all { it.getString(KEY_LANGUAGE) == null && it.getString(KEY_MODULE_NAME) == null }) return false
        Log.d(TAG, "startInstall: start")
        val needInstallSplitPack = arraySetOf<String>()
        for (split in splits) {
            val splitName = split.getString(KEY_LANGUAGE)?.let { "$SPLIT_LANGUAGE_TAG$it" } ?: split.getString(KEY_MODULE_NAME) ?: continue
            val splitInstalled = checkSplitInstalled(callingPackage, splitName)
            if (splitInstalled) continue
            needInstallSplitPack.add(splitName)
        }
        Log.d(TAG, "startInstall needInstallSplitPack: $needInstallSplitPack")
        if (needInstallSplitPack.isEmpty()) return false
        val oauthToken = runCatching { withContext(Dispatchers.IO) { getOauthToken() } }.getOrNull()
        Log.d(TAG, "startInstall oauthToken: $oauthToken")
        if (oauthToken.isNullOrEmpty()) return false
        notify(context)
        val triples = runCatching { requestDownloadUrls(callingPackage, oauthToken, needInstallSplitPack) }.getOrNull()
        Log.w(TAG, "startInstall requestDownloadUrls triples: $triples")
        if (triples.isNullOrEmpty()) {
            NotificationManagerCompat.from(context).cancel(SPLIT_INSTALL_NOTIFY_ID)
            return false
        }
        val intent = runCatching { installSplitPackage(context, callingPackage, triples) }.getOrNull()
        NotificationManagerCompat.from(context).cancel(SPLIT_INSTALL_NOTIFY_ID)
        if (intent == null) { return false }
        sendCompleteBroad(context, callingPackage, intent)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private suspend fun installSplitPackage(context: Context, callingPackage: String, downloadList: ArraySet<Triple<String, String, Int>>): Intent {
        Log.d(TAG, "installSplitPackage start ")
        if (!context.splitSaveFile().exists()) context.splitSaveFile().mkdir()
        val downloadSplitPackage = downloadSplitPackage(context, callingPackage, downloadList)
        if (!downloadSplitPackage) {
            Log.w(TAG, "installSplitPackage download failed")
            throw RuntimeException("installSplitPackage downloadSplitPackage has error")
        }
        Log.d(TAG, "installSplitPackage downloaded success")

        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_INHERIT_EXISTING)
        params.setAppPackageName(callingPackage)
        params.setAppLabel(callingPackage + "Subcontracting")
        params.setInstallLocation(PackageInfo.INSTALL_LOCATION_INTERNAL_ONLY)
        try {
            @SuppressLint("PrivateApi") val method = PackageInstaller.SessionParams::class.java.getDeclaredMethod(
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
            downloadList.forEach { item ->
                val pkgPath = File(context.splitSaveFile().toString(), item.first)
                session.openWrite(item.first, 0, -1).use { outputStream ->
                    FileInputStream(pkgPath).use { inputStream -> inputStream.copyTo(outputStream) }
                    session.fsync(outputStream)
                }
                totalDownloaded += pkgPath.length()
                pkgPath.delete()
            }
            val deferred = CompletableDeferred<Intent>()
            deferredMap[sessionId] = deferred
            val intent = Intent(context, InstallResultReceiver::class.java).apply {
                putExtra(KEY_BYTES_DOWNLOADED, totalDownloaded)
            }
            val pendingIntent = PendingIntent.getBroadcast(context, sessionId, intent, 0)
            session.commit(pendingIntent.intentSender)
            Log.d(TAG, "installSplitPackage session commit")
            return deferred.await()
        } catch (e: IOException) {
            Log.w(TAG, "Error installing split", e)
            throw e
        } finally {
            session?.close()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private suspend fun downloadSplitPackage(context: Context, callingPackage: String, downloadList: ArraySet<Triple<String, String, Int>>): Boolean =
        coroutineScope {
            val results = downloadList.map { info ->
                Log.d(TAG, "downloadSplitPackage: $info")
                async {
                    val downloaded = runCatching {
                        httpClient.download(info.second, File(context.splitSaveFile().toString(), info.first), SPLIT_INSTALL_REQUEST_TAG)
                    }.onFailure {
                        Log.w(TAG, "downloadSplitPackage url:${info.second} save:${info.first}", it)
                    }.getOrNull() != null
                    downloaded.also { updateSplitInstallRecord(callingPackage, Triple(info.first, info.second, if (it) STATUS_DOWNLOADED else STATUS_UNKNOWN)) }
                }
            }.awaitAll()
            return@coroutineScope results.all { it }
        }

    @RequiresApi(Build.VERSION_CODES.M)
    private suspend fun requestDownloadUrls(callingPackage: String, authToken: String, packs: MutableSet<String>): ArraySet<Triple<String, String, Int>> {
        val versionCode = PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(callingPackage, 0))
        val requestUrl =
            StringBuilder("https://play-fe.googleapis.com/fdfe/delivery?doc=$callingPackage&ot=1&vc=$versionCode&bvc=$versionCode&pf=1&pf=2&pf=3&pf=4&pf=5&pf=7&pf=8&pf=9&pf=10&da=4&bda=4&bf=4&fdcf=1&fdcf=2&ch=")
        packs.forEach { requestUrl.append("&mn=").append(it) }
        Log.d(TAG, "requestDownloadUrls start")
        val languages = packs.filter { it.startsWith(SPLIT_LANGUAGE_TAG) }.map { it.replace(SPLIT_LANGUAGE_TAG, "") }
        Log.d(TAG, "requestDownloadUrls languages: $languages")
        val response = httpClient.get(
            url = requestUrl.toString(),
            headers = buildRequestHeaders(authToken, 1, languages).onEach { Log.d(TAG, "key:${it.key}  value:${it.value}") },
            adapter = GoogleApiResponse.ADAPTER
        )
        Log.d(TAG, "requestDownloadUrls end response -> $response")
        val splitPkgInfoList = response.response?.splitReqResult?.pkgList?.pkgDownLoadInfo ?: throw RuntimeException("splitPkgInfoList is null")
        val packSet = ArraySet<Triple<String, String, Int>>()
        splitPkgInfoList.filter {
            !it.splitPkgName.isNullOrEmpty() && !it.downloadUrl.isNullOrEmpty()
        }.forEach { info ->
            packs.filter {
                it.contains(info.splitPkgName!!)
            }.forEach {
                packSet.add(Triple(first = it, second = info.downloadUrl!!, STATUS_DOWNLOADING))
            }
        }
        Log.d(TAG, "requestDownloadUrls end packSet -> $packSet")
        return packSet.onEach { updateSplitInstallRecord(callingPackage, it) }
    }

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

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkSplitInstalled(callingPackage: String, splitName: String): Boolean {
        if (!splitInstallRecord.containsKey(callingPackage)) return false
        return splitInstallRecord[callingPackage]?.find { it.first == splitName }?.third != STATUS_UNKNOWN
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun updateSplitInstallRecord(callingPackage: String, triple: Triple<String, String, Int>) {
        splitInstallRecord[callingPackage]?.let { triples ->
            val find = triples.find { it.first == triple.first }
            find?.let { triples.remove(it) }
            triples.add(triple)
        } ?: run {
            val triples = ArraySet<Triple<String, String, Int>>()
            triples.add(triple)
            splitInstallRecord[callingPackage] = triples
        }
    }

    private fun notify(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(NOTIFY_CHANNEL_ID, NOTIFY_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        NotificationCompat.Builder(context, NOTIFY_CHANNEL_ID).setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.getString(R.string.split_install, context.getString(R.string.app_name))).setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(
                NotificationCompat.DEFAULT_ALL
            ).build().also {
                notificationManager.notify(SPLIT_INSTALL_NOTIFY_ID, it)
            }
    }

    private fun Context.splitSaveFile() = File(filesDir, FILE_SAVE_PATH)

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
        // Installation records, including subpackage name, download path, and installation status
        private val splitInstallRecord = HashMap<String, ArraySet<Triple<String, String, Int>>>()
        private val deferredMap = mutableMapOf<Int, CompletableDeferred<Intent>>()
    }
}
