package org.microg.gms.constellation.core.verification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.microg.gms.constellation.core.proto.ChallengeResponse
import org.microg.gms.constellation.core.proto.MTChallenge
import org.microg.gms.constellation.core.proto.MTChallengeResponseData
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

private const val TAG = "MtSmsVerifier"

suspend fun MTChallenge.verify(
    subId: Int,
    timeoutMillis: Long? = null
): ChallengeResponse? {
    val expectedBody = sms.takeIf { it.isNotEmpty() } ?: return null
    val inbox = MtSmsInboxRegistry.get(subId)
    val effectiveTimeoutMillis = timeoutMillis?.coerceIn(0L..300_000L) ?: 300_000L

    Log.d(TAG, "Waiting for MT SMS containing challenge string")

    val match = withTimeoutOrNull(effectiveTimeoutMillis) {
        inbox.awaitMatch(expectedBody)
    }

    if (match == null) {
        Log.w(TAG, "Timed out waiting for MT SMS, proceeding with empty response")
    }

    val response = match ?: ReceivedSms(body = "", sender = "")
    return ChallengeResponse(
        mt_response = MTChallengeResponseData(
            sms = response.body,
            sender = response.sender
        )
    )
}

internal object MtSmsInboxRegistry {
    private val inboxes = ConcurrentHashMap<Int, MtSmsInbox>()

    fun prepare(context: Context, subIds: Iterable<Int>) {
        dispose()

        val effectiveSubIds = subIds.distinct().ifEmpty { listOf(-1) }
        for (subId in effectiveSubIds) {
            inboxes[subId] = MtSmsInbox(context.applicationContext, subId)
        }
    }

    fun get(subId: Int): MtSmsInbox {
        return inboxes[subId]
            ?: error("MT SMS inbox for subId=$subId was not initialized")
    }

    fun dispose() {
        val currentInboxes = inboxes.values.toList()
        inboxes.clear()
        for (inbox in currentInboxes) {
            inbox.dispose()
        }
    }
}

internal data class ReceivedSms(
    val body: String,
    val sender: String
)

private data class PendingMatch(
    val expectedBody: String,
    val continuation: CancellableContinuation<ReceivedSms?>
)

internal class MtSmsInbox(
    context: Context,
    private val subId: Int
) {
    private val context = context.applicationContext
    private val lock = Any()
    private val bufferedMessages = mutableListOf<ReceivedSms>()
    private val pendingMatches = mutableListOf<PendingMatch>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

            val receivedSubId = intent.getIntExtra(
                "android.telephony.extra.SUBSCRIPTION_INDEX",
                intent.getIntExtra("subscription", -1)
            )
            if (subId != -1 && receivedSubId != subId) return

            onMessagesReceived(Telephony.Sms.Intents.getMessagesFromIntent(intent))
        }
    }

    init {
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
    }

    suspend fun awaitMatch(expectedBody: String): ReceivedSms? {
        return suspendCancellableCoroutine { continuation ->
            synchronized(lock) {
                bufferedMessages.firstOrNull { it.body.contains(expectedBody) }?.let {
                    continuation.resume(it)
                    return@suspendCancellableCoroutine
                }

                val pendingMatch = PendingMatch(expectedBody, continuation)
                pendingMatches += pendingMatch
                continuation.invokeOnCancellation {
                    synchronized(lock) {
                        pendingMatches.remove(pendingMatch)
                    }
                }
            }
        }
    }

    private fun onMessagesReceived(messages: Array<SmsMessage>) {
        val receivedMessages = messages.mapNotNull { message ->
            val body = message.messageBody ?: return@mapNotNull null
            ReceivedSms(
                body = body,
                sender = message.originatingAddress ?: ""
            )
        }
        if (receivedMessages.isEmpty()) return

        synchronized(lock) {
            bufferedMessages += receivedMessages
            for (receivedMessage in receivedMessages) {
                val iterator = pendingMatches.iterator()
                while (iterator.hasNext()) {
                    val pendingMatch = iterator.next()
                    if (!receivedMessage.body.contains(pendingMatch.expectedBody)) continue

                    iterator.remove()
                    Log.d(TAG, "Matching MT SMS received from ${receivedMessage.sender}")
                    if (pendingMatch.continuation.isActive) {
                        pendingMatch.continuation.resume(receivedMessage)
                    }
                }
            }
        }
    }

    fun dispose() {
        synchronized(lock) {
            pendingMatches.clear()
            bufferedMessages.clear()
        }
        try {
            context.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
        }
    }
}
