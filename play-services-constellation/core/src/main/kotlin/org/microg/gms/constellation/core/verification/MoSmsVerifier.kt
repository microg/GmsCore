@file:RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)

package org.microg.gms.constellation.core.verification

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.microg.gms.constellation.core.proto.ChallengeResponse
import org.microg.gms.constellation.core.proto.MOChallengeResponseData
import org.microg.gms.constellation.core.proto.MoChallenge
import java.util.UUID
import kotlin.coroutines.resume

private const val TAG = "MoSmsVerifier"
private const val ACTION_MO_SMS_SENT = "org.microg.gms.constellation.core.MO_SMS_SENT"
private const val DEFAULT_MO_POLLING_INTERVALS_MILLIS =
    "4000,1000,1000,3000,5000,5000,5000,5000,30000,30000,30000,240000,600000,300000"

data class MoSmsSession(
    val challengeId: String,
    val subId: Int,
    val response: ChallengeResponse,
    private val pollingIntervalsMillis: List<Long>,
    private var nextPollingIndex: Int = 0,
) {
    fun matches(challengeId: String, subId: Int): Boolean {
        return this.challengeId == challengeId && this.subId == subId
    }

    fun nextPollingDelayMillis(remainingMillis: Long?): Long {
        val configuredDelay = pollingIntervalsMillis.getOrNull(nextPollingIndex)
        if (configuredDelay != null) {
            nextPollingIndex += 1
        }
        val delayMillis = configuredDelay ?: remainingMillis ?: 0L
        return if (remainingMillis != null) {
            delayMillis.coerceAtMost(remainingMillis.coerceAtLeast(0L))
        } else {
            delayMillis.coerceAtLeast(0L)
        }
    }
}

suspend fun MoChallenge.startSession(
    context: Context,
    challengeId: String,
    subId: Int
): MoSmsSession {
    return MoSmsSession(
        challengeId = challengeId,
        subId = subId,
        response = sendOnce(context, subId),
        pollingIntervalsMillis = pollingIntervalsMillis()
    )
}

private suspend fun MoChallenge.sendOnce(context: Context, subId: Int): ChallengeResponse {
    if (proxy_number.isEmpty() || sms.isEmpty()) {
        return failedMoResponse()
    }
    if (context.checkCallingOrSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
        Log.w(TAG, "SEND_SMS permission missing")
        return failedMoResponse()
    }

    if (subId != -1 && !isActiveSubscription(context, subId)) {
        return ChallengeResponse(
            mo_response = MOChallengeResponseData(
                status = MOChallengeResponseData.Status.NO_ACTIVE_SUBSCRIPTION
            )
        )
    }

    val smsManager = resolveSmsManager(context, subId) ?: return ChallengeResponse(
        mo_response = MOChallengeResponseData(
            status = MOChallengeResponseData.Status.NO_SMS_MANAGER
        )
    )

    val port = data_sms_info?.destination_port ?: 0
    val action = ACTION_MO_SMS_SENT
    val messageId = UUID.randomUUID().toString()
    val sentIntent = Intent(action).apply {
        `package` = context.packageName
        putExtra("message_id", messageId)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        messageId.hashCode(),
        sentIntent,
        PendingIntent.FLAG_ONE_SHOT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
    )

    Log.d(TAG, "Sending MO SMS to $proxy_number with messageId: $messageId")

    val response = withTimeoutOrNull(30_000L) {
        suspendCancellableCoroutine { continuation ->
            val receiver = MoSmsSentReceiver(action, messageId, continuation)

            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(action),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )

            continuation.invokeOnCancellation {
                runCatching { context.unregisterReceiver(receiver) }
            }

            try {
                if (port > 0) {
                    smsManager.sendDataMessage(
                        proxy_number,
                        null,
                        port.toShort(),
                        sms.encodeToByteArray(),
                        pendingIntent,
                        null
                    )
                } else {
                    smsManager.sendTextMessage(
                        proxy_number,
                        null,
                        sms,
                        pendingIntent,
                        null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initiate MO SMS send", e)
                runCatching { context.unregisterReceiver(receiver) }
                if (continuation.isActive) {
                    continuation.resume(failedMoResponse())
                }
            }
        }
    }
    return response ?: failedMoResponse()
}

private class MoSmsSentReceiver(
    private val action: String,
    private val messageId: String,
    private val continuation: CancellableContinuation<ChallengeResponse>
) : BroadcastReceiver() {
    override fun onReceive(receiverContext: Context, intent: Intent) {
        if (intent.action != action) return
        if (intent.getStringExtra("message_id") != messageId) return

        val smsResultCode = resultCode
        val smsErrorCode = intent.getIntExtra("errorCode", -1)

        Log.d(TAG, "MO SMS sent result: $smsResultCode, error: $smsErrorCode")
        runCatching { receiverContext.unregisterReceiver(this) }
        if (!continuation.isActive) return

        continuation.resume(
            ChallengeResponse(
                mo_response = MOChallengeResponseData(
                    status = if (smsResultCode == -1) {
                        MOChallengeResponseData.Status.COMPLETED
                    } else {
                        MOChallengeResponseData.Status.FAILED_TO_SEND_MO
                    },
                    sms_result_code = smsResultCode.toLong(),
                    sms_error_code = smsErrorCode.toLong()
                )
            )
        )
    }
}

private fun MoChallenge.pollingIntervalsMillis(): List<Long> {
    val configuredIntervals = polling_intervals.parsePollingIntervals()
    return configuredIntervals.ifEmpty {
        DEFAULT_MO_POLLING_INTERVALS_MILLIS.parsePollingIntervals()
    }
}

private fun String.parsePollingIntervals(): List<Long> {
    return split(',')
        .mapNotNull { it.trim().toLongOrNull() }
        .filter { it > 0L }
}

private fun failedMoResponse(): ChallengeResponse {
    return ChallengeResponse(
        mo_response = MOChallengeResponseData(
            status = MOChallengeResponseData.Status.FAILED_TO_SEND_MO
        )
    )
}

private fun isActiveSubscription(context: Context, subId: Int): Boolean {
    if (subId == -1) return true

    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_DENIED
    ) {
        Log.e(TAG, "Permission not granted")
        return false
    }

    return try {
        val subManager = context.getSystemService<SubscriptionManager>() ?: return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            subManager.isActiveSubscriptionId(subId)
        } else {
            subManager.getActiveSubscriptionInfo(subId) != null
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to query active subscription for $subId", e)
        false
    }
}

@Suppress("DEPRECATION")
private fun resolveSmsManager(context: Context, subId: Int): SmsManager? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService<SmsManager>()
            if (subId != -1) {
                manager?.createForSubscriptionId(subId)
            } else {
                manager
            }
        } else {
            if (subId != -1) {
                SmsManager.getSmsManagerForSubscriptionId(subId)
            } else {
                SmsManager.getDefault()
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to resolve SmsManager for subId: $subId", e)
        null
    }
}
