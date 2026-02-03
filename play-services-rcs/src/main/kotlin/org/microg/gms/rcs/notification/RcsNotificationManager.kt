/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsNotificationManager - Rich notification handling for RCS messages
 */

package org.microg.gms.rcs.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat

class RcsNotificationManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID_MESSAGES = "rcs_messages"
        private const val CHANNEL_ID_SERVICE = "rcs_service"
        private const val CHANNEL_ID_URGENT = "rcs_urgent"
        
        private const val GROUP_KEY_MESSAGES = "rcs_message_group"
        
        private var notificationId = 1000
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val messageChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                "RCS Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming RCS chat messages"
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }
            
            val serviceChannel = NotificationChannel(
                CHANNEL_ID_SERVICE,
                "RCS Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "RCS service status notifications"
                setShowBadge(false)
            }
            
            val urgentChannel = NotificationChannel(
                CHANNEL_ID_URGENT,
                "Urgent Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Priority RCS messages"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
            }
            
            notificationManager.createNotificationChannels(listOf(
                messageChannel, serviceChannel, urgentChannel
            ))
        }
    }

    fun showMessageNotification(
        senderName: String,
        senderPhone: String,
        messageContent: String,
        conversationId: String,
        timestamp: Long = System.currentTimeMillis()
    ): Int {
        val id = notificationId++
        
        val person = Person.Builder()
            .setName(senderName)
            .setKey(senderPhone)
            .build()
        
        val style = NotificationCompat.MessagingStyle(person)
            .addMessage(messageContent, timestamp, person)
            .setConversationTitle(senderName)
        
        val replyIntent = createReplyPendingIntent(conversationId, id)
        val markReadIntent = createMarkReadPendingIntent(conversationId, id)
        
        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Reply",
            replyIntent
        ).addRemoteInput(
            androidx.core.app.RemoteInput.Builder("reply_text")
                .setLabel("Type a message")
                .build()
        ).build()
        
        val markReadAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_view,
            "Mark as Read",
            markReadIntent
        ).build()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(senderName)
            .setContentText(messageContent)
            .setWhen(timestamp)
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_MESSAGES)
            .addAction(replyAction)
            .addAction(markReadAction)
            .setContentIntent(createOpenConversationIntent(conversationId))
            .build()
        
        notificationManager.notify(id, notification)
        
        updateSummaryNotification()
        
        return id
    }

    fun showFileTransferNotification(
        fileName: String,
        senderName: String,
        progress: Int,
        transferId: String
    ): Int {
        val id = transferId.hashCode()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SERVICE)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Receiving file from $senderName")
            .setContentText(fileName)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()
        
        notificationManager.notify(id, notification)
        return id
    }

    fun showFileTransferComplete(
        fileName: String,
        senderName: String,
        transferId: String
    ) {
        val id = transferId.hashCode()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("File received from $senderName")
            .setContentText(fileName)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(id, notification)
    }

    fun showServiceNotification(message: String): Int {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SERVICE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("RCS Service")
            .setContentText(message)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        val id = 1
        notificationManager.notify(id, notification)
        return id
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    fun cancelAll() {
        notificationManager.cancelAll()
    }

    private fun updateSummaryNotification() {
        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("New messages")
            .setGroup(GROUP_KEY_MESSAGES)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(0, summaryNotification)
    }

    private fun createReplyPendingIntent(conversationId: String, notificationId: Int): PendingIntent {
        val intent = Intent("org.microg.gms.rcs.REPLY").apply {
            putExtra("conversation_id", conversationId)
            putExtra("notification_id", notificationId)
        }
        return PendingIntent.getBroadcast(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private fun createMarkReadPendingIntent(conversationId: String, notificationId: Int): PendingIntent {
        val intent = Intent("org.microg.gms.rcs.MARK_READ").apply {
            putExtra("conversation_id", conversationId)
            putExtra("notification_id", notificationId)
        }
        return PendingIntent.getBroadcast(
            context, notificationId + 10000, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createOpenConversationIntent(conversationId: String): PendingIntent {
        val intent = Intent("org.microg.gms.rcs.OPEN_CONVERSATION").apply {
            putExtra("conversation_id", conversationId)
        }
        return PendingIntent.getActivity(
            context, conversationId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
