/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RcsEventBus - In-process event bus for decoupled components
 */

package org.microg.gms.rcs.events

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class RcsEventBus private constructor() {

    companion object {
        const val TAG = "RcsEventBus"
        
        @Volatile
        private var instance: RcsEventBus? = null
        
        fun getInstance(): RcsEventBus {
            return instance ?: synchronized(this) {
                instance ?: RcsEventBus().also { instance = it }
            }
        }
    }

    val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val eventFlows = ConcurrentHashMap<KClass<*>, MutableSharedFlow<RcsEvent>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : RcsEvent> getEventFlow(eventClass: KClass<T>): SharedFlow<T> {
        val flow = eventFlows.getOrPut(eventClass) {
            MutableSharedFlow(replay = 0, extraBufferCapacity = 64)
        }
        return flow.asSharedFlow() as SharedFlow<T>
    }

    fun <T : RcsEvent> publish(event: T) {
        coroutineScope.launch {
            val flow = eventFlows[event::class] as? MutableSharedFlow<T>
            flow?.emit(event)
            
            val baseFlow = eventFlows[RcsEvent::class] as? MutableSharedFlow<RcsEvent>
            baseFlow?.emit(event)
            
            Log.d(TAG, "Published event: ${event::class.simpleName}")
        }
    }

    inline fun <reified T : RcsEvent> subscribe(crossinline handler: suspend (T) -> Unit) {
        coroutineScope.launch {
            getEventFlow(T::class).collect { event ->
                try {
                    handler(event)
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling event ${T::class.simpleName}", e)
                }
            }
        }
    }
}

sealed class RcsEvent {
    abstract val timestamp: Long
}

data class RegistrationStateChangedEvent(
    val isRegistered: Boolean,
    val phoneNumber: String?,
    override val timestamp: Long = System.currentTimeMillis()
) : RcsEvent()

data class MessageReceivedEvent(
    val messageId: String,
    val senderPhone: String,
    val content: String,
    val contentType: String,
    override val timestamp: Long = System.currentTimeMillis()
) : RcsEvent()

data class MessageSentEvent(
    val messageId: String,
    val recipientPhone: String,
    override val timestamp: Long = System.currentTimeMillis()
) : RcsEvent()

data class MessageDeliveredEvent(
    val messageId: String,
    val recipientPhone: String,
    val deliveredAt: Long,
    override val timestamp: Long = System.currentTimeMillis()
) : RcsEvent()

data class MessageReadEvent(
    val messageId: String,
    val recipientPhone: String,
    val readAt: Long,
    override val timestamp: Long = System.currentTimeMillis()
) : RcsEvent()

data class TypingIndicatorEvent(
    val phoneNumber: String,
    val isTyping: Boolean,
    override val timestamp: Long = System.currentTimeMillis()
) : RcsEvent()

data class PresenceChangedEvent(
    val phoneNumber: String,
    val isOnline: Boolean,
    val statusMessage: String?,
    override val timestamp: Long = System.currentTimeMillis()
) : RcsEvent()

data class CapabilitiesChangedEvent(
    val phoneNumber: String,
    val isRcsEnabled: Boolean,
    val capabilities: Set<String>,
    override val timestamp: Long = System.currentTimeMillis()
) : RcsEvent()

data class FileTransferProgressEvent(
    val transferId: String,
    val bytesTransferred: Long,
    val totalBytes: Long,
    override val timestamp: Long = System.currentTimeMillis()
) : RcsEvent()

data class FileTransferCompleteEvent(
    val transferId: String,
    val isSuccessful: Boolean,
    val errorMessage: String? = null,
    override val timestamp: Long = System.currentTimeMillis()
) : RcsEvent()

data class ConnectionStateChangedEvent(
    val isConnected: Boolean,
    val networkType: String?,
    override val timestamp: Long = System.currentTimeMillis()
) : RcsEvent()

data class ErrorEvent(
    val errorCode: String,
    val errorMessage: String,
    val component: String,
    override val timestamp: Long = System.currentTimeMillis()
) : RcsEvent()
