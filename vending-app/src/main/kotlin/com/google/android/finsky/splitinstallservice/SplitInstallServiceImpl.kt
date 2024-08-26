/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.finsky.splitinstallservice

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.Session
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import com.android.vending.R
import com.google.android.phonesky.header.GoogleApiRequest
import com.google.android.play.core.splitinstall.protocol.ISplitInstallService
import com.google.android.play.core.splitinstall.protocol.ISplitInstallServiceCallback
import org.microg.vending.billing.DEFAULT_ACCOUNT_TYPE
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

class SplitInstallServiceImpl(private val context: Context) : ISplitInstallService.Stub(){

    private val tempFilePath = File(context.filesDir,"phonesky-download-service")

    override fun startInstall(
        pkg: String,
        splits: List<Bundle>,
        bundle0: Bundle,
        callback: ISplitInstallServiceCallback
    ) {
        Log.i(TAG, "Start install for package: $pkg")
        trySplitInstall(pkg, splits, false)
        taskQueue.put(Runnable {
            try{
                callback.onStartInstall(1, Bundle())
            }catch (ignored: RemoteException){
            }
        })
        taskQueue.take().run()
    }

    override fun completeInstalls(
        pkg: String,
        sessionId: Int,
        bundle0: Bundle,
        callback: ISplitInstallServiceCallback
    ) {
        Log.i(TAG, "Complete installs not implemented")
    }

    override fun cancelInstall(
        pkg: String,
        sessionId: Int,
        callback: ISplitInstallServiceCallback
    ) {
        Log.i(TAG, "Cancel install not implemented")
    }

    override fun getSessionState(
        pkg: String,
        sessionId: Int,
        callback: ISplitInstallServiceCallback
    ) {
        Log.i(TAG, "getSessionState not implemented")
    }

    override fun getSessionStates(pkg: String, callback: ISplitInstallServiceCallback) {
        Log.i(TAG, "getSessionStates for package: $pkg")
        callback.onGetSessionStates(ArrayList<Bundle>(1))
    }

    override fun splitRemoval(
        pkg: String,
        splits: List<Bundle>,
        callback: ISplitInstallServiceCallback
    ) {
        Log.i(TAG, "Split removal not implemented")
    }

    override fun splitDeferred(
        pkg: String,
        splits: List<Bundle>,
        bundle0: Bundle,
        callback: ISplitInstallServiceCallback
    ) {
        Log.i(TAG, "Split deferred not implemented")
        callback.onDeferredInstall(Bundle())
    }

    override fun getSessionState2(
        pkg: String,
        sessionId: Int,
        callback: ISplitInstallServiceCallback
    ) {
        Log.i(TAG, "getSessionState2 not implemented")
    }

    override fun getSessionStates2(pkg: String, callback: ISplitInstallServiceCallback) {
        Log.i(TAG, "getSessionStates2 not implemented")
    }

    override fun getSplitsAppUpdate(pkg: String, callback: ISplitInstallServiceCallback) {
        Log.i(TAG, "Get splits for app update not implemented")
    }

    override fun completeInstallAppUpdate(pkg: String, callback: ISplitInstallServiceCallback) {
        Log.i(TAG, "Complete install for app update not implemented")
    }

    override fun languageSplitInstall(
        pkg: String,
        splits: List<Bundle>,
        bundle0: Bundle,
        callback: ISplitInstallServiceCallback
    ) {
        Log.i(TAG, "Language split installation requested for $pkg")
        trySplitInstall(pkg, splits, true)
        taskQueue.take().run()
    }

    override fun languageSplitUninstall(
        pkg: String,
        splits: List<Bundle>,
        callback: ISplitInstallServiceCallback
    ) {
        Log.i(TAG, "Language split uninstallation requested but app not found, package: %s$pkg")
    }

    private fun trySplitInstall(pkg: String, splits: List<Bundle>, isLanguageSplit: Boolean) {
        Log.d(TAG, "trySplitInstall: $splits")
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    "splitInstall",
                    "Split Install",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
        val builder = NotificationCompat.Builder(context, "splitInstall")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.getString(R.string.split_install, context.getString(R.string.app_name)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        notificationManager.notify(NOTIFY_ID, builder.build())
        if (isLanguageSplit) {
            requestSplitsPackage(
                pkg, splits.map { bundle: Bundle -> bundle.getString("language") }.toTypedArray(),
                arrayOfNulls(0)
            )
        } else {
            requestSplitsPackage(
                pkg,
                arrayOfNulls(0),splits.map { bundle: Bundle -> bundle.getString("module_name") }.toTypedArray())
        }
    }

    private fun requestSplitsPackage(
        packageName: String,
        langName: Array<String?>,
        splitName: Array<String?>
    ): Boolean {
        Log.d(TAG,"requestSplitsPackage packageName: " + packageName + " langName: " + langName.contentToString() + " splitName: " + splitName.contentToString())
        if(langName.isEmpty() && splitName.isEmpty()){
            return false
        }

        val packageManager = context.packageManager
        val versionCode = PackageInfoCompat.getLongVersionCode(packageManager.getPackageInfo(packageName, 0))
        val downloadUrls = getDownloadUrls(packageName, langName, splitName, versionCode)
        Log.d(TAG, "requestSplitsPackage download url size : " + downloadUrls.size)
        if (downloadUrls.isEmpty()){
            Log.w(TAG, "requestSplitsPackage download url is empty")
            return false
        }
        try {
            if(!tempFilePath.exists()){
                tempFilePath.mkdir()
            }
            val language:String? = if (langName.isNotEmpty()) {
                langName[0]
            } else {
                null
            }

            taskQueue.put(Runnable {
                    installSplitPackage(downloadUrls, packageName, language)
            })


            return true
        } catch (e: Exception) {
            Log.e("SplitInstallServiceImpl", "Error downloading split", e)
            return false
        }
    }

    private fun downloadSplitPackage(downloadUrls: ArrayList<Array<String>>) : Boolean{
        Log.d(TAG, "downloadSplitPackage downloadUrl:$downloadUrls")
        var stat = true
        for(downloadUrl in downloadUrls){
            val url = URL(downloadUrl[1])
            val connection = url.openConnection() as HttpURLConnection
            connection.readTimeout = 30000
            connection.connectTimeout = 30000
            connection.requestMethod = "GET"
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                BufferedInputStream(connection.inputStream).use { inputstream ->
                    BufferedOutputStream(FileOutputStream(File(tempFilePath.toString(),downloadUrl[0]))).use { outputstream ->
                        inputstream.copyTo(outputstream)
                    }
                }
            }else{
                stat = false
            }
            Log.d(TAG, "downloadSplitPackage code: " + connection.responseCode)
        }
        return stat
    }

    private fun installSplitPackage(
        downloadUrl: ArrayList<Array<String>>,
        packageName: String,
        language: String?
    ) {
        try {
            Log.d(TAG, "installSplitPackage downloadUrl:$downloadUrl")
            if (downloadSplitPackage(downloadUrl)) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFY_ID)
                val packageInstaller: PackageInstaller
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    packageInstaller = context.packageManager.packageInstaller
                    val params = PackageInstaller.SessionParams(
                        PackageInstaller.SessionParams.MODE_INHERIT_EXISTING
                    )
                    params.setAppPackageName(packageName)
                    params.setAppLabel(packageName + "Subcontracting")
                    params.setInstallLocation(PackageInfo.INSTALL_LOCATION_INTERNAL_ONLY)
                    try {
                        @SuppressLint("PrivateApi") val method =
                            PackageInstaller.SessionParams::class.java.getDeclaredMethod(
                                "setDontKillApp",
                                Boolean::class.javaPrimitiveType
                            )
                        method.invoke(params, true)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error setting dontKillApp", e)
                    }

                    val sessionId: Int
                    var session : Session? = null
                    var totalDownloaded = 0L
                    try {
                        sessionId = packageInstaller.createSession(params)
                        session = packageInstaller.openSession(sessionId)

                        try {
                            downloadUrl.forEach { item ->
                                val pkgPath = File(tempFilePath.toString(),item[0])
                                session.openWrite(item[0], 0, -1).use { outputStream ->
                                    FileInputStream(pkgPath).use { inputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                                    session.fsync(outputStream)
                                }

                                totalDownloaded += pkgPath.length()
                                pkgPath.delete()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error installing split", e)
                        }

                        val intent = Intent(context, InstallResultReceiver::class.java)
                        intent.putExtra("pkg", packageName)
                        intent.putExtra("language", language)
                        intent.putExtra("bytes_downloaded", totalDownloaded)
                        val pendingIntent = PendingIntent.getBroadcast(context,sessionId, intent, 0)
                        session.commit(pendingIntent.intentSender)
                        Log.d(TAG, "installSplitPackage commit")
                    } catch (e: IOException) {
                        Log.w(TAG, "Error installing split", e)
                    } finally {
                        session?.close()
                    }
                }
            } else {
                taskQueue.clear();
                Log.w(TAG, "installSplitPackage download failed")
            }
        } catch (e: Exception) {
            Log.w(TAG, "downloadSplitPackage: ", e)
        }
    }

    private fun getDownloadUrls(
        packageName: String,
        langName: Array<String?>,
        splitName: Array<String?>,
        versionCode: Long
    ): ArrayList<Array<String>> {
        Log.d(TAG, "getDownloadUrls: ")
        val downloadUrls = ArrayList<Array<String>>()
        try {
            val requestUrl = StringBuilder(
                "https://play-fe.googleapis.com/fdfe/delivery?doc=" + packageName + "&ot=1&vc=" + versionCode + "&bvc=" + versionCode +
                        "&pf=1&pf=2&pf=3&pf=4&pf=5&pf=7&pf=8&pf=9&pf=10&da=4&bda=4&bf=4&fdcf=1&fdcf=2&ch="
            )
            for (language in langName) {
                requestUrl.append("&mn=config.").append(language)
            }
            for (split in splitName) {
                requestUrl.append("&mn=").append(split)
            }
            val accounts = AccountManager.get(this.context).getAccountsByType(DEFAULT_ACCOUNT_TYPE)
            if (accounts.isEmpty()) {
                Log.w(TAG, "getDownloadUrls account is null")
                return downloadUrls
            }
            val googleApiRequest =
                GoogleApiRequest(
                    requestUrl.toString(), "GET", accounts[0], context,
                    langName.filterNotNull()
                )
            val response = googleApiRequest.sendRequest(null)
            val pkgs = response?.fdfeApiResponseValue?.splitReqResult?.pkgList?.pkgDownlaodInfo
            if (pkgs != null) {
                for (item in pkgs) {
                    for (lang in langName) {
                        if (TextUtils.equals("config.$lang", item.splitPkgName) || "config.$lang".startsWith(item.splitPkgName!!)) {
                            downloadUrls.add(arrayOf(lang!!, item.downloadUrl1!!))
                        }
                    }
                    Log.d(TAG, "requestSplitsPackage: $splitName")
                    for (split in splitName) {
                        if (split != null && TextUtils.equals(split, item.splitPkgName)) {
                            downloadUrls.add(arrayOf(split, item.downloadUrl1!!))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting download url", e)
        }
        return downloadUrls
    }

    class InstallResultReceiver : BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        override fun onReceive(context: Context, intent: Intent) {
            val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
            Log.d(TAG, "onReceive status: $status")
            try {
                when (status) {
                    PackageInstaller.STATUS_SUCCESS -> {
                        if (taskQueue.isNotEmpty()) {
                            thread {
                                taskQueue.take().run()
                            }
                        }
                        if(taskQueue.size <= 1){
                            NotificationManagerCompat.from(context).cancel(NOTIFY_ID)
                            sendCompleteBroad(context, intent)
                        }
                    }

                    PackageInstaller.STATUS_FAILURE -> {
                        taskQueue.clear();
                        val errorMsg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                        Log.d("InstallResultReceiver", errorMsg ?: "")
                    }

                    PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                        val extraIntent = intent.extras!![Intent.EXTRA_INTENT] as Intent?
                        extraIntent!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ContextCompat.startActivity(context, extraIntent, null)
                    }

                    else -> {
                        taskQueue.clear()
                        NotificationManagerCompat.from(context).cancel(NOTIFY_ID)
                        val errorMsg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                        Log.d("InstallResultReceiver", errorMsg ?: "")
                        Log.w(TAG, "onReceive: install fail")
                    }
                }
            } catch (e: Exception) {
                taskQueue.clear()
                NotificationManagerCompat.from(context).cancel(NOTIFY_ID)
                Log.w(TAG, "Error handling install result", e)
            }
        }

        private fun sendCompleteBroad(context: Context, originalIntent: Intent) {
            Log.d(TAG, "sendCompleteBroadcast: $originalIntent")
            val extra = Bundle().apply {
                putInt("status", 5)
                putLong("total_bytes_to_download", originalIntent.getLongExtra("bytes_downloaded", 0))
                putString("languages", originalIntent.getStringExtra("language"))
                putInt("error_code", 0)
                putInt("session_id", 0)
                putLong("bytes_downloaded", originalIntent.getLongExtra("bytes_downloaded", 0))
            }
            val broadcastIntent = Intent("com.google.android.play.core.splitinstall.receiver.SplitInstallUpdateIntentService").apply {
                setPackage(originalIntent.getStringExtra("pkg"))
                putExtra("session_state", extra)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            context.sendBroadcast(broadcastIntent)
        }
    }

    companion object {
        private val taskQueue: BlockingQueue<Runnable> = LinkedBlockingQueue()
        val TAG: String = SplitInstallServiceImpl::class.java.simpleName
        const val NOTIFY_ID = 111
    }
}

