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

private const val CHANNEL_ID = "progress_notification_channel"
private const val NOTIFICATION_ID = 1
private const val CANCEL_ACTION = "CANCEL_DOWNLOAD"

private const val TAG = "DownloadManager"

class DownloadManager(private val context: Context) {

    private val notifyBuilderMap = ConcurrentHashMap<String, NotificationCompat.Builder>()
    private val notificationLayoutMap = ConcurrentHashMap<String, RemoteViews>()
    private val downloadingRecord = ConcurrentHashMap<String, Future<*>>()

    @Volatile
    private var shouldStops = false

    private val cancelReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val moduleName = intent.getStringExtra(KEY_MODULE_NAME)
            if (moduleName != null) {
                cancelDownload(moduleName)
            }
        }
    }

    init {
        createNotificationChannel()
        val filter = IntentFilter(CANCEL_ACTION)
        context.registerReceiver(cancelReceiver, filter)
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

    private fun initNotification(moduleName: String, packageName: String) {
        val cancelIntent = Intent(CANCEL_ACTION).apply {
            putExtra(KEY_MODULE_NAME, moduleName)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val packageManager: PackageManager = context.packageManager
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        val appName = packageManager.getApplicationLabel(applicationInfo).toString()
        val appIcon = packageManager.getApplicationIcon(applicationInfo)
        val bitmap = if (appIcon is BitmapDrawable) {
            appIcon.bitmap
        } else {
            val bitmapTemp = Bitmap.createBitmap(
                appIcon.intrinsicWidth, appIcon.intrinsicHeight, Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmapTemp)
            appIcon.setBounds(0, 0, canvas.width, canvas.height)
            appIcon.draw(canvas)
            bitmapTemp
        }

        val notificationLayout = RemoteViews(context.packageName, R.layout.layout_download_notification)
        notificationLayout.setTextViewText(
            R.id.notification_title, context.getString(R.string.download_notification_attachment_file, appName)
        )
        notificationLayout.setTextViewText(
            R.id.notification_text, context.getString(R.string.download_notification_tips)
        )
        notificationLayout.setProgressBar(R.id.progress_bar, 100, 0, false)
        notificationLayout.setImageViewBitmap(R.id.app_icon, bitmap)
        notificationLayout.setOnClickPendingIntent(R.id.cancel_button, cancelPendingIntent)

        val notifyBuilder =
            NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.ic_app_foreground).setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(notificationLayout).setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(true).setOnlyAlertOnce(true)
                .setColor(ContextCompat.getColor(context, R.color.notification_color)).setColorized(true)
        notifyBuilderMap[moduleName] = notifyBuilder
        notificationLayoutMap[moduleName] = notificationLayout

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notifyBuilder.setCustomContentView(notificationLayout).build())
    }


    private fun updateProgress(moduleName: String, progress: Int) {
        val notificationLayout = notificationLayoutMap[moduleName] ?: return
        val notifyBuilder = notifyBuilderMap[moduleName] ?: return

        notificationLayout.setProgressBar(R.id.progress_bar, 100, progress, false)
        notifyBuilder.setCustomContentView(notificationLayout)
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notifyBuilder.setCustomContentView(notificationLayout).build())
    }

    @Synchronized
    fun shouldStop(shouldStop:Boolean){
        shouldStops = shouldStop
    }

    @Synchronized
    fun prepareDownload(downloadData: DownloadData, moduleName: String) {
        Log.d(TAG, "prepareDownload: ${downloadData.packageName}")
        initNotification(moduleName, downloadData.packageName)
        val future = executor.submit {
            val packData = downloadData.getModuleData(moduleName)
            downloadData.updateDownloadStatus(moduleName, STATUS_DOWNLOADING)
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
                startDownload(moduleName, resourceLink, destination, downloadData)
                sendBroadcastForExistingFile(context, downloadData, moduleName, dataBundle, destination)
            }
            updateProgress(moduleName, 100)
            notifyBuilderMap[moduleName]?.setOngoing(false)
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
        downloadingRecord[moduleName] = future
    }

    @Synchronized
    private fun cancelDownload(moduleName: String) {
        Log.d(TAG, "Download for module $moduleName has been canceled.")
        downloadingRecord[moduleName]?.cancel(true)
        shouldStops = true
        notifyBuilderMap[moduleName]?.setOngoing(false)
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun startDownload(moduleName: String, downloadLink: String, destinationFile: File, downloadData: DownloadData) {
        val packData = downloadData.getModuleData(moduleName)
        val uri = Uri.parse(downloadLink).toString()
        val connection = URL(uri).openConnection() as HttpURLConnection
        var bytes: Long = 0
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 20000
            connection.readTimeout = 20000
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
                        if (shouldStops) {
                            Log.d(TAG, "Download interrupted for module: $moduleName")
                            downloadData.updateDownloadStatus(moduleName, CANCELED)
                            return
                        }
                        output.write(buffer, 0, bytesRead)
                        bytes += bytesRead.toLong()
                        downloadData.incrementModuleBytesDownloaded(moduleName, bytesRead.toLong())
                        if (bytes >= 1048576) {
                            val progress = ((packData.bytesDownloaded.toDouble() / packData.totalBytesToDownload.toDouble()) * 100).toInt()
                            updateProgress(moduleName, progress)
                            sendBroadcastForExistingFile(context, downloadData, moduleName, null, null)
                            bytes = 0
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "prepareDownload: startDownload error ", e)
            downloadData.updateDownloadStatus(moduleName, STATUS_FAILED)
        } finally {
            connection.disconnect()
        }
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