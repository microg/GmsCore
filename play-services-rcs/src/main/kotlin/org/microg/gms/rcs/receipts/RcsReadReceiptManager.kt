/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsReadReceiptManager - Read receipts and delivery reports
 */

package org.microg.gms.rcs.receipts

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.microg.gms.rcs.events.MessageDeliveredEvent
import org.microg.gms.rcs.events.MessageReadEvent
import org.microg.gms.rcs.events.RcsEventBus
import java.util.concurrent.ConcurrentHashMap

class RcsReadReceiptManager(private val context: Context) {

    companion object {
        private const val TAG = "RcsReceipts"
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val eventBus = RcsEventBus.getInstance()
    
    private val pendingDeliveryReports = ConcurrentHashMap<String, DeliveryState>()
    private val listeners = mutableListOf<ReceiptListener>()

    fun markAsDelivered(messageId: String, recipientPhone: String) {
        val deliveredAt = System.currentTimeMillis()
        
        pendingDeliveryReports[messageId] = DeliveryState(
            messageId = messageId,
            recipientPhone = recipientPhone,
            status = DeliveryStatus.DELIVERED,
            timestamp = deliveredAt
        )
        
        coroutineScope.launch {
            sendDeliveryReport(messageId, recipientPhone, DeliveryStatus.DELIVERED)
        }
        
        eventBus.publish(MessageDeliveredEvent(messageId, recipientPhone, deliveredAt))
        listeners.forEach { it.onDelivered(messageId, recipientPhone, deliveredAt) }
        
        Log.d(TAG, "Message $messageId marked as delivered")
    }

    fun markAsRead(messageId: String, recipientPhone: String) {
        val readAt = System.currentTimeMillis()
        
        pendingDeliveryReports[messageId] = DeliveryState(
            messageId = messageId,
            recipientPhone = recipientPhone,
            status = DeliveryStatus.READ,
            timestamp = readAt
        )
        
        coroutineScope.launch {
            sendDeliveryReport(messageId, recipientPhone, DeliveryStatus.READ)
        }
        
        eventBus.publish(MessageReadEvent(messageId, recipientPhone, readAt))
        listeners.forEach { it.onRead(messageId, recipientPhone, readAt) }
        
        Log.d(TAG, "Message $messageId marked as read")
    }

    fun onRemoteDeliveryReceived(messageId: String, senderPhone: String, status: DeliveryStatus) {
        val timestamp = System.currentTimeMillis()
        
        when (status) {
            DeliveryStatus.DELIVERED -> {
                eventBus.publish(MessageDeliveredEvent(messageId, senderPhone, timestamp))
                listeners.forEach { it.onRemoteDelivered(messageId, senderPhone, timestamp) }
            }
            DeliveryStatus.READ -> {
                eventBus.publish(MessageReadEvent(messageId, senderPhone, timestamp))
                listeners.forEach { it.onRemoteRead(messageId, senderPhone, timestamp) }
            }
            else -> {}
        }
        
        Log.d(TAG, "Remote receipt received: $messageId - $status")
    }

    private suspend fun sendDeliveryReport(
        messageId: String,
        recipientPhone: String,
        status: DeliveryStatus
    ) {
        Log.d(TAG, "Sending delivery report: $messageId -> $status")
    }

    fun addListener(listener: ReceiptListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: ReceiptListener) {
        listeners.remove(listener)
    }
}

data class DeliveryState(
    val messageId: String,
    val recipientPhone: String,
    val status: DeliveryStatus,
    val timestamp: Long
)

enum class DeliveryStatus {
    PENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

interface ReceiptListener {
    fun onDelivered(messageId: String, recipientPhone: String, timestamp: Long)
    fun onRead(messageId: String, recipientPhone: String, timestamp: Long)
    fun onRemoteDelivered(messageId: String, senderPhone: String, timestamp: Long)
    fun onRemoteRead(messageId: String, senderPhone: String, timestamp: Long)
}
