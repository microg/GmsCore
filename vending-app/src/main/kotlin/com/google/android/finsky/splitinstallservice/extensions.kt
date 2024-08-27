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
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import com.android.vending.R
import com.android.vending.RequestLanguagePackage
import com.android.vending.licensing.AUTH_TOKEN_SCOPE
import com.android.vending.licensing.encodeGzip
import com.android.vending.licensing.getAuthToken
import com.android.vending.licensing.getDefaultLicenseRequestHeaderBuilder
import com.android.vending.licensing.getLicenseRequestHeaders
import com.google.android.finsky.GoogleApiResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.microg.gms.settings.SettingsContract
import org.microg.vending.billing.DEFAULT_ACCOUNT_TYPE
import org.microg.vending.billing.core.HttpClient
import java.io.File
import java.io.FileInputStream
import java.io.IOException

const val SPLIT_INSTALL_REQUEST_TAG = "splitInstallRequestTag"
private const val SPLIT_INSTALL_NOTIFY_ID = 111

private const val NOTIFY_CHANNEL_ID = "splitInstall"
private const val NOTIFY_CHANNEL_NAME = "Split Install"
private const val KEY_LANGUAGE = "language"
private const val KEY_LANGUAGES = "languages"
private const val KEY_PACKAGE = "pkg"
private const val KEY_MODULE_NAME = "module_name"
private const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"
private const val KEY_TOTAL_BYTES_TO_DOWNLOAD = "total_bytes_to_download"
private const val KEY_STATUS = "status"
private const val KEY_ERROR_CODE = "error_code"
private const val KEY_SESSION_ID = "session_id"
private const val KEY_SESSION_STATE = "session_state"

private const val ACTION_UPDATE_SERVICE = "com.google.android.play.core.splitinstall.receiver.SplitInstallUpdateIntentService"

private const val FILE_SAVE_PATH = "phonesky-download-service"
private const val TAG = "SplitInstallExtensions"

private val mutex = Mutex()
private val deferredMap = mutableMapOf<Int, CompletableDeferred<Intent>>()

private var lastSplitPackageName: String? = null
private val splitRecord = arrayListOf<Array<String>>()

private fun Context.splitSaveFile() = File(filesDir, FILE_SAVE_PATH)

suspend fun trySplitInstall(context: Context, httpClient: HttpClient, pkg: String, splits: List<Bundle>) {
    if (lastSplitPackageName != null && lastSplitPackageName != pkg && mutex.isLocked) {
        mutex.unlock()
    }
    mutex.withLock {
        Log.d(TAG, "trySplitInstall: pkg: $pkg")
        var splitNames:Array<String> ?= null
        try {
            if (splits.any { it.getString(KEY_LANGUAGE) != null }) {
                splitNames = splits.mapNotNull { bundle -> bundle.getString(KEY_LANGUAGE) }.toTypedArray()
                Log.d(TAG, "langNames: ${splitNames.contentToString()}")
                if (splitNames.isEmpty() || splitRecord.any { splitNames.contentEquals(it) }) {
                    return@withLock
                }
                lastSplitPackageName = pkg
                requestSplitsPackage(context, httpClient, pkg, splitNames, emptyArray())
                splitRecord.add(splitNames)
            } else if (splits.any { it.getString(KEY_MODULE_NAME) != null }) {
                splitNames = splits.mapNotNull { bundle -> bundle.getString(KEY_MODULE_NAME) }.toTypedArray()
                Log.d(TAG, "moduleNames: ${splitNames.contentToString()}")
                if (splitNames.isEmpty() || splitRecord.any { splitNames.contentEquals(it) }) {
                    return@withLock
                }
                lastSplitPackageName = pkg
                requestSplitsPackage(context, httpClient, pkg, emptyArray(), splitNames)
                splitRecord.add(splitNames)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error downloading split", e)
            splitNames?.run { splitRecord.remove(this) }
            NotificationManagerCompat.from(context).cancel(SPLIT_INSTALL_NOTIFY_ID)
        }
        return@withLock
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
        .setContentTitle(context.getString(R.string.split_install, context.getString(R.string.app_name))).setPriority(NotificationCompat.PRIORITY_DEFAULT).setDefaults(NotificationCompat.DEFAULT_ALL)
        .build().also {
            notificationManager.notify(SPLIT_INSTALL_NOTIFY_ID, it)
        }
}

private suspend fun requestSplitsPackage(context: Context, httpClient: HttpClient, packageName: String, langName: Array<String>, splitName: Array<String>) {
    Log.d(TAG, "requestSplitsPackage packageName: $packageName langName: ${langName.contentToString()} splitName: ${splitName.contentToString()}")
    notify(context)
    val downloadUrls = getDownloadUrls(context, httpClient, packageName, langName, splitName)
    Log.d(TAG, "requestSplitsPackage download url size : " + downloadUrls.size)
    if (downloadUrls.isEmpty()) {
        throw RuntimeException("requestSplitsPackage download url is empty")
    }
    if (!context.splitSaveFile().exists()) {
        context.splitSaveFile().mkdir()
    }
    val intent = installSplitPackage(context, httpClient, downloadUrls, packageName, langName.firstOrNull())
    sendCompleteBroad(context, intent)
}

private suspend fun getDownloadUrls(context: Context, httpClient: HttpClient, packageName: String, langName: Array<String>, splitName: Array<String>): ArrayList<Array<String>> {
    Log.d(TAG, "getDownloadUrls: start -> langName:${langName.contentToString()} splitName:${splitName.contentToString()}")
    val versionCode = PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(packageName, 0))
    val requestUrl = StringBuilder(
        "https://play-fe.googleapis.com/fdfe/delivery?doc=$packageName&ot=1&vc=$versionCode&bvc=$versionCode&pf=1&pf=2&pf=3&pf=4&pf=5&pf=7&pf=8&pf=9&pf=10&da=4&bda=4&bf=4&fdcf=1&fdcf=2&ch="
    )
    for (language in langName) {
        requestUrl.append("&mn=config.").append(language)
    }
    for (split in splitName) {
        requestUrl.append("&mn=").append(split)
    }
    val accounts = AccountManager.get(context).getAccountsByType(DEFAULT_ACCOUNT_TYPE)
    var oauthToken: String? = null
    if (accounts.isEmpty()) {
        throw RuntimeException("No Google account found")
    } else for (account: Account in accounts) {
        oauthToken = try {
            AccountManager.get(context).getAuthToken(account, AUTH_TOKEN_SCOPE, false).getString(AccountManager.KEY_AUTHTOKEN)
        } catch (e: AuthenticatorException) {
            Log.w(TAG, "Could not fetch auth token for account $account")
            null
        }
        if (oauthToken != null) {
            break
        }
    }
    if (oauthToken == null) {
        throw RuntimeException("account oauthToken is null")
    }
    Log.d(TAG, "getDownloadUrls: requestDownloadUrl start")
    val response = httpClient.requestDownloadUrl(context, requestUrl.toString(), oauthToken, langName.toList())
    Log.d(TAG, "getDownloadUrls: requestDownloadUrl end response -> $response")
    val splitPkgInfoList = response?.response?.splitReqResult?.pkgList?.pkgDownLoadInfo ?: throw RuntimeException("splitPkgInfoList is null")
    val downloadUrls = ArrayList<Array<String>>()
    splitPkgInfoList.filter { !it.splitPkgName.isNullOrEmpty() && !it.downloadUrl.isNullOrEmpty() }.forEach { info ->
        langName.filter { "config.$it".contains(info.splitPkgName!!) }.forEach { downloadUrls.add(arrayOf(it, info.downloadUrl!!)) }
        splitName.filter { it.contains(info.splitPkgName!!) }.forEach { downloadUrls.add(arrayOf(it, info.downloadUrl!!)) }
    }
    return downloadUrls
}

private suspend fun HttpClient.requestDownloadUrl(context: Context, requestUrl: String, auth: String, requestLanguagePackage: List<String>) = runCatching {
    val androidId = SettingsContract.getSettings(
        context, SettingsContract.CheckIn.getContentUri(context), arrayOf(SettingsContract.CheckIn.ANDROID_ID)
    ) { cursor: Cursor -> cursor.getLong(0) }
    Log.d(TAG, "requestUrl->$requestUrl")
    Log.d(TAG, "auth->$auth")
    Log.d(TAG, "androidId->$androidId")
    Log.d(TAG, "requestLanguagePackage->$requestLanguagePackage")
    get(url = requestUrl, headers = getLicenseRequestHeaders(auth, 1).toMutableMap().apply {
        val xPsRh = String(
            Base64.encode(
                getDefaultLicenseRequestHeaderBuilder(1).languages(RequestLanguagePackage.Builder().language(requestLanguagePackage).build()).build().encode().encodeGzip(),
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
        )
        put("X-PS-RH", xPsRh)
    }.onEach {
        Log.d(TAG, "key:${it.key}  value:${it.value}")
    }, adapter = GoogleApiResponse.ADAPTER)
}.onFailure {
    Log.d(TAG, "requestDownloadUrl: ", it)
}.getOrNull()

private suspend fun HttpClient.downloadSplitPackage(context: Context, downloadUrls: ArrayList<Array<String>>): Boolean = coroutineScope {
    val results = downloadUrls.map { urls ->
        Log.d(TAG, "downloadSplitPackage: ${urls.contentToString()}")
        async {
            runCatching {
                download(urls[1], File(context.splitSaveFile().toString(), urls[0]), SPLIT_INSTALL_REQUEST_TAG)
            }.onFailure {
                Log.w(TAG, "downloadSplitPackage urls:${urls.contentToString()}: ", it)
            }.getOrNull() != null
        }
    }.awaitAll()
    return@coroutineScope results.all { it }
}

private suspend fun installSplitPackage(context: Context, httpClient: HttpClient, downloadUrl: ArrayList<Array<String>>, packageName: String, language: String?): Intent {
    Log.d(TAG, "installSplitPackage downloadUrl: ${downloadUrl.firstOrNull()}")
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        throw RuntimeException("installSplitPackage Not supported yet ")
    }
    val downloadSplitPackage = httpClient.downloadSplitPackage(context, downloadUrl)
    if (!downloadSplitPackage) {
        Log.w(TAG, "installSplitPackage download failed")
        throw RuntimeException("installSplitPackage downloadSplitPackage has error")
    }
    Log.d(TAG, "installSplitPackage downloaded success")
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(SPLIT_INSTALL_NOTIFY_ID)
    val packageInstaller = context.packageManager.packageInstaller
    val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_INHERIT_EXISTING)
    params.setAppPackageName(packageName)
    params.setAppLabel(packageName + "Subcontracting")
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
        downloadUrl.forEach { item ->
            val pkgPath = File(context.splitSaveFile().toString(), item[0])
            session.openWrite(item[0], 0, -1).use { outputStream ->
                FileInputStream(pkgPath).use { inputStream -> inputStream.copyTo(outputStream) }
                session.fsync(outputStream)
            }
            totalDownloaded += pkgPath.length()
            pkgPath.delete()
        }

        val deferred = CompletableDeferred<Intent>()
        deferredMap[sessionId] = deferred
        val intent = Intent(context, InstallResultReceiver::class.java).apply {
            putExtra(KEY_PACKAGE, packageName)
            putExtra(KEY_LANGUAGE, language)
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

private fun sendCompleteBroad(context: Context, intent: Intent) {
    Log.d(TAG, "sendCompleteBroadcast: intent:$intent")
    val extra = Bundle().apply {
        putInt(KEY_STATUS, 5)
        putLong(KEY_TOTAL_BYTES_TO_DOWNLOAD, intent.getLongExtra(KEY_BYTES_DOWNLOADED, 0))
        putString(KEY_LANGUAGES, intent.getStringExtra(KEY_LANGUAGE))
        putInt(KEY_ERROR_CODE, 0)
        putInt(KEY_SESSION_ID, 0)
        putLong(KEY_BYTES_DOWNLOADED, intent.getLongExtra(KEY_BYTES_DOWNLOADED, 0))
    }
    val broadcastIntent = Intent(ACTION_UPDATE_SERVICE).apply {
        setPackage(intent.getStringExtra(KEY_PACKAGE))
        putExtra(KEY_SESSION_STATE, extra)
        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    }
    context.sendBroadcast(broadcastIntent)
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
                    NotificationManagerCompat.from(context).cancel(SPLIT_INSTALL_NOTIFY_ID)
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
                    Log.d(TAG, "InstallResultReceiver onReceive: install fail -> $errorMsg")
                    if (sessionId != -1) {
                        deferredMap[sessionId]?.completeExceptionally(RuntimeException("install fail -> $errorMsg"))
                        deferredMap.remove(sessionId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error handling install result", e)
            NotificationManagerCompat.from(context).cancel(SPLIT_INSTALL_NOTIFY_ID)
            if (sessionId != -1) {
                deferredMap[sessionId]?.completeExceptionally(e)
            }
        }
    }
}

