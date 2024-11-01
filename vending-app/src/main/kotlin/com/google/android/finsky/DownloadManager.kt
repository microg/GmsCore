/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.android.vending.R
import com.google.android.finsky.assetmoduleservice.DownloadData
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

private const val corePoolSize = 0
private const val maximumPoolSize = 1
private const val keepAliveTime = 30L
private const val progressDelayTime = 1000L

private const val PACK_DOWNLOADING = 0
private const val PACK_DOWNLOADED = 1
private const val DOWNLOAD_PREPARE = 2

private const val CHANNEL_ID = "progress_notification_channel"
private const val NOTIFICATION_ID = 1
private const val CANCEL_ACTION = "CANCEL_DOWNLOAD"

private const val TAG = "DownloadManager"

class DownloadManager(private val context: Context) {

    private lateinit var notifyBuilder: NotificationCompat.Builder
    private lateinit var notificationLayout: RemoteViews
    private val downloadingRecord = ConcurrentHashMap<String, Future<*>>()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Download Progress"
            val descriptionText = "Shows download progress"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private val executor by lazy {
        ThreadPoolExecutor(
                corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, LinkedBlockingQueue()
        ) { r -> Thread(r).apply { name = "DownloadThread" } }
    }

    private val mHandler = object : Handler(Looper.getMainLooper()) {

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                PACK_DOWNLOADING -> {
                    val bundle = msg.obj as Bundle
                    val moduleName = bundle.getString(KEY_MODULE_NAME)!!
                    val downloadData = bundle.getSerializable(KEY_DOWNLOAD_DATA) as DownloadData
                    updateProgress((downloadData.bytesDownloaded * 100 / downloadData.totalBytesToDownload).toInt())
                    sendBroadcastForExistingFile(context, downloadData, moduleName, null, null)
                    sendMessageDelayed(obtainMessage(PACK_DOWNLOADING).apply { obj = bundle }, progressDelayTime)
                }

                PACK_DOWNLOADED -> {
                    val bundle = msg.obj as Bundle
                    val moduleName = bundle.getString(KEY_MODULE_NAME)!!
                    val dataBundle = bundle.getBundle(KEY_DOWNLOAD_PARK_BUNDLE)
                    val destinationFile = bundle.getString(KEY_FILE_PATH)?.let { File(it) }
                    val downloadData = bundle.getSerializable(KEY_DOWNLOAD_DATA) as DownloadData
                    sendBroadcastForExistingFile(context, downloadData, moduleName, dataBundle, destinationFile)
                }

                DOWNLOAD_PREPARE -> {
                    val downloadData = msg.obj as DownloadData
                    initNotification(downloadData.packageName)
                    context.registerReceiver(cancelReceiver, IntentFilter(CANCEL_ACTION))
                }
            }
        }
    }

    private fun initNotification(packageName: String) {
        val cancelIntent = Intent(CANCEL_ACTION)
        val cancelPendingIntent = PendingIntent.getBroadcast(context, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val packageManager: PackageManager = context.packageManager
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        val appName = packageManager.getApplicationLabel(applicationInfo).toString()
        val appIcon = packageManager.getApplicationIcon(applicationInfo)
        val bitmap = if (appIcon is BitmapDrawable) {
            appIcon.bitmap
        } else {
            val bitmapTemp = Bitmap.createBitmap(appIcon.intrinsicWidth, appIcon.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmapTemp)
            appIcon.setBounds(0, 0, canvas.width, canvas.height)
            appIcon.draw(canvas)
            bitmapTemp
        }

        notificationLayout = RemoteViews(context.packageName, R.layout.layout_download_notification)
        notificationLayout.setTextViewText(R.id.notification_title, context.getString(R.string.download_notification_attachment_file, appName))
        notificationLayout.setTextViewText(R.id.notification_text, context.getString(R.string.download_notification_tips))
        notificationLayout.setProgressBar(R.id.progress_bar, 100, 0, false)
        notificationLayout.setImageViewBitmap(R.id.app_icon, bitmap)
        notificationLayout.setOnClickPendingIntent(R.id.cancel_button, cancelPendingIntent)

        notifyBuilder =
                NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.ic_app_foreground).setStyle(NotificationCompat.DecoratedCustomViewStyle())
                        .setCustomContentView(notificationLayout).setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(true).setOnlyAlertOnce(true)
                        .setColor(ContextCompat.getColor(context, R.color.notification_color)).setColorized(true)
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notifyBuilder.build())
    }

    fun updateProgress(progress: Int) {
        notificationLayout.setProgressBar(R.id.progress_bar, 100, progress, false)
        notifyBuilder.setCustomContentView(notificationLayout)
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notifyBuilder.build())
    }

    private val cancelReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            cleanup()
        }
    }

    fun cleanup() {
        mHandler.removeCallbacksAndMessages(null)
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        runCatching { context.unregisterReceiver(cancelReceiver) }
    }

    @Synchronized
    fun prepareDownload(downloadData: DownloadData) {
        Log.d(TAG, "prepareDownload: ${downloadData.packageName}")
        val callingPackageName = downloadData.packageName
        if (downloadingRecord.containsKey(callingPackageName) && downloadingRecord[callingPackageName]?.isDone == false) {
            return
        }
        if (downloadingRecord.isNotEmpty() && !downloadingRecord.containsKey(callingPackageName)) {
            downloadingRecord.values.forEach { it.cancel(true) }
            cleanup()
            downloadingRecord.clear()
        }
        Log.d(TAG, "prepareDownload: ${downloadData.packageName} start")
        val future = executor.submit {
            mHandler.sendMessage(mHandler.obtainMessage(DOWNLOAD_PREPARE).apply { obj = downloadData })
            downloadData.moduleNames.forEach { moduleName ->
                mHandler.sendMessage(mHandler.obtainMessage(PACK_DOWNLOADING).apply {
                    obj = Bundle().apply {
                        putString(KEY_MODULE_NAME, moduleName)
                        putSerializable(KEY_DOWNLOAD_DATA, downloadData)
                    }
                })
                val packData = downloadData.getModuleData(moduleName)
                for (dataBundle in packData.packBundleList) {
                    val resourcePackageName: String? = dataBundle.getString(KEY_RESOURCE_PACKAGE_NAME)
                    val chunkName: String? = dataBundle.getString(KEY_CHUNK_NAME)
                    val resourceLink: String? = dataBundle.getString(KEY_RESOURCE_LINK)
                    val index: Int = dataBundle.getInt(KEY_INDEX)
                    val resourceBlockName: String? = dataBundle.getString(KEY_RESOURCE_BLOCK_NAME)
                    if (resourcePackageName == null || chunkName == null || resourceLink == null || resourceBlockName == null) {
                        continue
                    }
                    val filesDir = "${context.filesDir}/assetpacks/$index/$resourcePackageName/$chunkName/"
                    val destination = File(filesDir, resourceBlockName)
                    startDownload(moduleName, resourceLink, destination, dataBundle, downloadData) ?: return@forEach
                }
                mHandler.removeMessages(PACK_DOWNLOADING)
            }
            cleanup()
        }
        downloadingRecord[callingPackageName] = future
    }

    @Synchronized
    private fun startDownload(moduleName: String, downloadLink: String, destinationFile: File, dataBundle: Bundle, downloadData: DownloadData): String? {
        val uri = Uri.parse(downloadLink).toString()
        var retryCount = 0
        while (retryCount < 3) {
            val connection = URL(uri).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.connect()
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("Failed to download file: HTTP response code ${connection.responseCode}")
                }
                if (destinationFile.exists()) {
                    destinationFile.delete()
                } else destinationFile.parentFile?.mkdirs()
                connection.inputStream.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadData.incrementModuleBytesDownloaded(moduleName, bytesRead.toLong())
                        }
                    }
                }
                mHandler.sendMessage(mHandler.obtainMessage(PACK_DOWNLOADED).apply {
                    obj = Bundle().apply {
                        putString(KEY_MODULE_NAME, moduleName)
                        putString(KEY_FILE_PATH, destinationFile.absolutePath)
                        putBundle(KEY_DOWNLOAD_PARK_BUNDLE, dataBundle)
                        putSerializable(KEY_DOWNLOAD_DATA, downloadData)
                    }
                })
                return destinationFile.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "prepareDownload: startDownload error ", e)
                retryCount++
                if (retryCount >= 3) {
                    return null
                }
            } finally {
                connection.disconnect()
            }
        }
        return null
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: DownloadManager? = null
        fun get(context: Context): DownloadManager {
            return instance ?: synchronized(this) {
                instance ?: DownloadManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
