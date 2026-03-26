package org.microg.gms.constellation.core.verification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.microg.gms.constellation.core.proto.ChallengeResponse
import org.microg.gms.constellation.core.proto.MTChallenge
import org.microg.gms.constellation.core.proto.MTChallengeResponseData
import kotlin.coroutines.resume

private const val TAG = "MtSmsVerifier"

class MtSmsVerifier(private val context: Context, private val subId: Int) {
    suspend fun verify(challenge: MTChallenge?): ChallengeResponse? {
        val expectedBody = challenge?.sms?.takeIf { it.isNotEmpty() } ?: return null

        Log.d(TAG, "Waiting for MT SMS containing challenge string")

        val result = withTimeoutOrNull(300_000) {
            suspendCancellableCoroutine<Pair<String, String>?> { continuation ->
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        try {
                            if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                                val receivedSubId = intent.getIntExtra(
                                    "android.telephony.extra.SUBSCRIPTION_INDEX",
                                    intent.getIntExtra("subscription", -1)
                                )
                                if (subId != -1 && receivedSubId != -1 && receivedSubId != subId) {
                                    return
                                }
                                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                                for (msg in messages) {
                                    val body = msg.messageBody
                                    if (body != null && body.contains(expectedBody)) {
                                        Log.d(
                                            TAG,
                                            "Matching MT SMS received from ${msg.originatingAddress}"
                                        )
                                        context.unregisterReceiver(this)
                                        if (continuation.isActive) {
                                            continuation.resume(
                                                Pair(
                                                    body,
                                                    msg.originatingAddress ?: ""
                                                )
                                            )
                                        }
                                        return
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in MT SMS receiver", e)
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(
                        receiver,
                        IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION),
                        Context.RECEIVER_NOT_EXPORTED
                    )
                } else {
                    context.registerReceiver(
                        receiver,
                        IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
                    )
                }
                continuation.invokeOnCancellation {
                    try {
                        context.unregisterReceiver(receiver)
                    } catch (_: Exception) {
                    }
                }
            }
        }

        return result?.let { (body, sender) ->
            ChallengeResponse(
                mt_response = MTChallengeResponseData(
                    sms = body,
                    sender = sender
                )
            )
        }
    }
}
