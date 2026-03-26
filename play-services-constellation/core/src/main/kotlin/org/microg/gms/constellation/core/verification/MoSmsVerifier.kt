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
import androidx.core.content.getSystemService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.microg.gms.constellation.core.proto.ChallengeResponse
import org.microg.gms.constellation.core.proto.MOChallengeResponseData
import org.microg.gms.constellation.core.proto.MoChallenge
import java.util.UUID
import kotlin.coroutines.resume

private const val TAG = "MoSmsVerifier"
private const val ACTION_MO_SMS_SENT = "org.microg.gms.constellation.coreMO_SMS_SENT"

suspend fun MoChallenge.verify(context: Context, subId: Int): ChallengeResponse {
    if (proxy_number.isEmpty() || sms.isEmpty()) {
        return ChallengeResponse(mo_response = MOChallengeResponseData(status = MOChallengeResponseData.Status.FAILED_TO_SEND_MO))
    }
    if (context.checkCallingOrSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
        Log.w(TAG, "SEND_SMS permission missing")
        return ChallengeResponse(mo_response = MOChallengeResponseData(status = MOChallengeResponseData.Status.FAILED_TO_SEND_MO))
    }

    val smsManager = resolveSmsManager(context, subId) ?: return ChallengeResponse(
        mo_response = MOChallengeResponseData(
            status = if (subId != -1 && !isActiveSubscription(context, subId)) {
                MOChallengeResponseData.Status.NO_ACTIVE_SUBSCRIPTION
            } else {
                MOChallengeResponseData.Status.NO_SMS_MANAGER
            }
        )
    )

    val port = data_sms_info?.destination_port ?: 0
    val isBinarySms = port > 0
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

    return withTimeoutOrNull(30000) {
        suspendCancellableCoroutine { continuation ->
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == action) {
                        val receivedId = intent.getStringExtra("message_id")
                        if (receivedId != messageId) return

                        val resultCode = resultCode
                        val errorCode = intent.getIntExtra("errorCode", -1)

                        Log.d(TAG, "MO SMS sent result: $resultCode, error: $errorCode")

                        val status = when (resultCode) {
                            -1 -> MOChallengeResponseData.Status.COMPLETED
                            else -> MOChallengeResponseData.Status.FAILED_TO_SEND_MO
                        }

                        try {
                            context.unregisterReceiver(this)
                        } catch (_: Exception) {
                        }

                        if (continuation.isActive) {
                            continuation.resume(
                                ChallengeResponse(
                                    mo_response = MOChallengeResponseData(
                                        status = status,
                                        sms_result_code = resultCode.toLong(),
                                        sms_error_code = errorCode.toLong()
                                    )
                                )
                            )
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    receiver,
                    IntentFilter(action),
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(receiver, IntentFilter(action))
            }

            continuation.invokeOnCancellation {
                try {
                    context.unregisterReceiver(receiver)
                } catch (_: Exception) {
                }
            }

            try {
                if (isBinarySms) {
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
                try {
                    context.unregisterReceiver(receiver)
                } catch (_: Exception) {
                }
                if (continuation.isActive) {
                    continuation.resume(
                        ChallengeResponse(
                            mo_response = MOChallengeResponseData(
                                status = MOChallengeResponseData.Status.FAILED_TO_SEND_MO
                            )
                        )
                    )
                }
            }
        }
    } ?: ChallengeResponse(
        mo_response = MOChallengeResponseData(
            status = MOChallengeResponseData.Status.FAILED_TO_SEND_MO
        )
    )
}

private fun isActiveSubscription(context: Context, subscriptionId: Int): Boolean {
    if (subscriptionId == -1) return false
    return try {
        context.getSystemService<SubscriptionManager>()?.isActiveSubscriptionId(subscriptionId)
            ?: false
    } catch (e: Exception) {
        Log.w(TAG, "Failed to query active subscription for $subscriptionId", e)
        false
    }
}

private fun resolveSmsManager(context: Context, subId: Int): SmsManager? {
    if (subId != -1 && !isActiveSubscription(context, subId)) {
        return null
    }
    return try {
        val manager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            null
        }
        when {
            subId != -1 && manager != null -> manager.createForSubscriptionId(subId)
            manager != null -> manager
            subId != -1 -> {
                @Suppress("DEPRECATION")
                SmsManager.getSmsManagerForSubscriptionId(subId)
            }

            else -> {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to resolve SmsManager for subId: $subId", e)
        null
    }
}
