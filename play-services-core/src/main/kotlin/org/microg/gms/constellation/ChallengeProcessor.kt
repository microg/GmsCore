/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log
import google.internal.communications.phonedeviceverification.v1.*
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume

private const val TAG = "GmsConstellationChallenge"

/** OTP SMS data: full body + sender, matching stock GMS bfqy.e(originatingAddress, messageBody) */
data class OtpSmsResult(val messageBody: String, val originatingAddress: String)

/**
 * SMS inbox that buffers messages arriving before PENDING is detected.
 * Pre-register receivers BEFORE Sync so SMS arriving during the
 * Sync RPC isn't missed.
 *
 * Lifecycle: prepare() before Sync → awaitMatch() after PENDING → dispose() in finally.
 * Uses both silent (createAppSpecificSmsToken PendingIntent) and noisy (SMS_RECEIVED) paths.
 */
object SmsInbox {
    private val lock = Any()
    private val bufferedMessages = mutableListOf<OtpSmsResult>()
    private var pendingFuture: CompletableFuture<OtpSmsResult>? = null
    private var silentReceiver: BroadcastReceiver? = null
    private var noisyReceiver: BroadcastReceiver? = null

    fun prepare(context: Context) {
        dispose(context)
        Log.i(TAG, "SmsInbox: preparing receivers before Sync")

        silentReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                Log.i(TAG, "SILENT_SMS_RECEIVED onReceive! action=${intent.action} extras=${intent.extras?.keySet()}")
                extractSmsFromIntent(intent)?.let { onSmsReceived(it, "silent") }
            }
        }
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(silentReceiver!!,
                IntentFilter(ConstellationConstants.ACTION_SILENT_SMS_RECEIVED),
                Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(silentReceiver!!,
                IntentFilter(ConstellationConstants.ACTION_SILENT_SMS_RECEIVED))
        }

        val hasReceiveSms = context.checkSelfPermission(android.Manifest.permission.RECEIVE_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasReceiveSms) {
            noisyReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
                        extractSmsFromIntent(intent)?.let { onSmsReceived(it, "noisy") }
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(noisyReceiver!!,
                    IntentFilter("android.provider.Telephony.SMS_RECEIVED").apply { priority = 1000 },
                    Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(noisyReceiver!!,
                    IntentFilter("android.provider.Telephony.SMS_RECEIVED").apply { priority = 1000 })
            }
            Log.i(TAG, "SmsInbox: noisy + silent receivers registered")
        } else {
            Log.i(TAG, "SmsInbox: silent-only (RECEIVE_SMS not granted)")
        }
    }

    private fun extractSmsFromIntent(intent: Intent): OtpSmsResult? {
        @Suppress("DEPRECATION")
        val pdus = intent.extras?.get("pdus") as? Array<*>
        if (pdus != null) {
            for (pdu in pdus) {
                val msg = if (Build.VERSION.SDK_INT >= 23) {
                    SmsMessage.createFromPdu(pdu as ByteArray, intent.extras?.getString("format"))
                } else {
                    @Suppress("DEPRECATION")
                    SmsMessage.createFromPdu(pdu as ByteArray)
                }
                val body = msg?.messageBody ?: continue
                val sender = msg.originatingAddress ?: ""
                return OtpSmsResult(body, sender)
            }
        }
        // Fallback for silent path without PDUs
        val body = intent.getStringExtra("message_body")
            ?: intent.getStringExtra("body")
            ?: intent.getStringExtra("sms_body")
        val sender = intent.getStringExtra("originating_address")
            ?: intent.getStringExtra("address")
            ?: ""
        if (body != null) return OtpSmsResult(body, sender)
        // Last resort: token matched by Android (PendingIntent fired = correct SMS)
        return OtpSmsResult("TOKEN_MATCHED_SMS", sender)
    }

    private fun onSmsReceived(sms: OtpSmsResult, source: String) {
        Log.d(TAG, "SmsInbox: SMS from ${sms.originatingAddress} via $source, length=${sms.messageBody.length}")
        Log.i(TAG, "SmsInbox: body='${sms.messageBody}'")
        synchronized(lock) {
            bufferedMessages.add(sms)
            pendingFuture?.let { future ->
                if (!future.isDone) {
                    Log.i(TAG, "SmsInbox: completing pending future via $source")
                    future.complete(sms)
                }
            }
        }
    }

    /**
     * Wait for OTP SMS. Checks buffer first (SMS may have arrived during Sync),
     * then blocks until new SMS arrives or timeout.
     * Silent path accepts unconditionally (Android pre-verified via token).
     * Noisy path: all SMS are buffered - caller can filter if needed.
     */
    fun awaitMatch(timeoutSeconds: Long = 120): OtpSmsResult? {
        synchronized(lock) {
            if (bufferedMessages.isNotEmpty()) {
                val first = bufferedMessages.first()
                Log.i(TAG, "SmsInbox: found buffered SMS from ${first.originatingAddress}")
                return first
            }
            pendingFuture = CompletableFuture<OtpSmsResult>()
        }
        Log.i(TAG, "SmsInbox: waiting ${timeoutSeconds}s for OTP SMS...")
        return try {
            pendingFuture!!.get(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS).also {
                Log.i(TAG, "SmsInbox: OTP received from ${it.originatingAddress}")
            }
        } catch (e: java.util.concurrent.TimeoutException) {
            Log.w(TAG, "SmsInbox: OTP timeout after ${timeoutSeconds}s")
            null
        } catch (e: Exception) {
            Log.e(TAG, "SmsInbox: error waiting for SMS: ${e.message}")
            null
        }
    }

    fun dispose(context: Context) {
        synchronized(lock) {
            bufferedMessages.clear()
            pendingFuture = null
        }
        silentReceiver?.let { r -> try { context.unregisterReceiver(r) } catch (_: Exception) {} }
        noisyReceiver?.let { r -> try { context.unregisterReceiver(r) } catch (_: Exception) {} }
        silentReceiver = null
        noisyReceiver = null
    }
}

// ======== CHALLENGE VERIFIERS ========

object ChallengeProcessor {

    private fun moFailedToSend() = ChallengeResponse(
        mo_challenge_response = MOChallengeResponse(
            status = MOChallengeStatus.MO_STATUS_FAILED_TO_SEND
        )
    )

    private fun ts43InternalError(ts43Type: Ts43Type?) = ChallengeResponse(
        ts43_challenge_response = Ts43ChallengeResponse(
            ts43_type = ts43Type,
            error = Error(error_type = ErrorType.ERROR_TYPE_INTERNAL_ERROR)
        )
    )

    /**
     * Send MO SMS to proxy_number from server challenge. Returns ChallengeResponse.
     * Sends SMS and waits for delivery confirmation.
     */
    suspend fun sendMoSms(
        context: Context,
        moChallenge: MOChallenge,
        subId: Int
    ): ChallengeResponse {
        val proxyNumber = moChallenge.proxy_number
        val smsText = moChallenge.sms
        if (proxyNumber.isEmpty() || smsText.isEmpty()) {
            Log.w(TAG, "MO SMS: empty proxy_number or sms text")
            return moFailedToSend()
        }
        if (context.checkCallingOrSelfPermission(android.Manifest.permission.SEND_SMS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "MO SMS: SEND_SMS permission not granted")
            return moFailedToSend()
        }

        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(android.telephony.SmsManager::class.java)
                ?.let { if (subId > 0) it.createForSubscriptionId(subId) else it }
        } else {
            @Suppress("DEPRECATION")
            if (subId > 0) android.telephony.SmsManager.getSmsManagerForSubscriptionId(subId)
            else android.telephony.SmsManager.getDefault()
        }
        if (smsManager == null) {
            Log.e(TAG, "MO SMS: cannot resolve SmsManager for subId=$subId")
            return ChallengeResponse(
                mo_challenge_response = MOChallengeResponse(
                    status = MOChallengeStatus.MO_STATUS_NO_SMS_MANAGER
                )
            )
        }

        val messageId = java.util.UUID.randomUUID().toString()
        val action = "org.microg.gms.constellation.MO_SMS_SENT"
        val sentIntent = android.content.Intent(action).apply {
            `package` = context.packageName
            putExtra("message_id", messageId)
        }
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context, messageId.hashCode(), sentIntent,
            android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        Log.i(TAG, "MO SMS: sending to $proxyNumber (messageId=$messageId, subId=$subId)")

        val port = moChallenge.data_sms_info?.port ?: 0
        return kotlinx.coroutines.withTimeoutOrNull(30_000L) {
            kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                val receiver = object : android.content.BroadcastReceiver() {
                    override fun onReceive(ctx: android.content.Context, intent: android.content.Intent) {
                        if (intent.getStringExtra("message_id") != messageId) return
                        val resultCode = resultCode
                        val errorCode = intent.getIntExtra("errorCode", -1)
                        Log.d(TAG, "MO SMS sent result: code=$resultCode error=$errorCode")
                        try { ctx.unregisterReceiver(this) } catch (_: Exception) {}
                        if (continuation.isActive) {
                            val status = if (resultCode == -1) // Activity.RESULT_OK
                                MOChallengeStatus.MO_STATUS_COMPLETED
                            else
                                MOChallengeStatus.MO_STATUS_FAILED_TO_SEND
                            continuation.resume(ChallengeResponse(
                                mo_challenge_response = MOChallengeResponse(
                                    status = status,
                                    sms_result_code = resultCode.toLong(),
                                    sms_error_code = errorCode.toLong()
                                )
                            ))
                        }
                    }
                }
                androidx.core.content.ContextCompat.registerReceiver(
                    context, receiver, android.content.IntentFilter(action),
                    androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
                )
                continuation.invokeOnCancellation {
                    try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
                }
                try {
                    if (port > 0) {
                        smsManager.sendDataMessage(proxyNumber, null, port.toShort(), smsText.toByteArray(), pendingIntent, null)
                    } else {
                        smsManager.sendTextMessage(proxyNumber, null, smsText, pendingIntent, null)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "MO SMS send failed: ${e.message}")
                    try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
                    if (continuation.isActive) {
                        continuation.resume(moFailedToSend())
                    }
                }
            }
        } ?: moFailedToSend()
    }

    /**
     * Verify via Carrier ID: send SIM ISIM challenge to TelephonyManager.getIccAuthentication().
     * Pure SIM-level crypto, no DG needed.
     */
    fun verifyCarrierId(
        context: Context,
        challenge: CarrierIDChallenge,
        subId: Int
    ): ChallengeResponse {
        val challengeData = challenge.isim_request.takeIf { it.isNotEmpty() }
        if (challengeData == null) {
            Log.w(TAG, "CarrierID: empty isim_request")
            return carrierIdError(CarrierIdError.CARRIER_ID_ERROR_UNKNOWN_ERROR)
        }
        if (subId <= 0) {
            Log.w(TAG, "CarrierID: invalid subId=$subId")
            return carrierIdError(CarrierIdError.CARRIER_ID_ERROR_NO_SIM)
        }
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            ?: return carrierIdError(CarrierIdError.CARRIER_ID_ERROR_NOT_SUPPORTED)
        val targetTm = tm.createForSubscriptionId(subId)
        val appType = challenge.app_type.takeIf { it != 0 } ?: android.telephony.TelephonyManager.APPTYPE_USIM

        return try {
            val response = targetTm.getIccAuthentication(appType, challenge.auth_type, challengeData)
            if (response.isNullOrEmpty()) {
                Log.w(TAG, "CarrierID: null/empty ISIM response")
                carrierIdError(CarrierIdError.CARRIER_ID_ERROR_NULL_RESPONSE)
            } else {
                Log.i(TAG, "CarrierID: ISIM response ${response.length} chars")
                ChallengeResponse(
                    carrier_id_challenge_response = CarrierIDChallengeResponse(
                        isim_response = response,
                        carrier_id_error = CarrierIdError.CARRIER_ID_ERROR_NO_ERROR
                    )
                )
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "CarrierID: SecurityException - ${e.message}")
            carrierIdError(CarrierIdError.CARRIER_ID_ERROR_UNABLE_TO_READ_SUBSCRIPTION)
        } catch (e: Exception) {
            Log.e(TAG, "CarrierID: failed - ${e.message}")
            carrierIdError(CarrierIdError.CARRIER_ID_ERROR_REFLECTION_ERROR)
        }
    }

    private fun carrierIdError(error: CarrierIdError): ChallengeResponse {
        return ChallengeResponse(
            carrier_id_challenge_response = CarrierIDChallengeResponse(
                isim_response = "",
                carrier_id_error = error
            )
        )
    }

    /**
     * Handle TS.43 challenge from server. Server provides entitlement_url + eap_aka_realm.
     * Delegates to Ts43Client for EAP-AKA authentication with the SIM.
     * Returns Ts43ChallengeResponse with auth token or error.
     */
    fun handleTs43Challenge(
        context: Context,
        ts43Challenge: Ts43Challenge,
        subId: Int,
        phoneNumber: String?
    ): ChallengeResponse? {
        try {
            val entitlementUrl = ts43Challenge.entitlement_url
            if (entitlementUrl.isNullOrEmpty()) {
                Log.w(TAG, "  TS43: empty entitlement_url in challenge")
                return ts43InternalError(ts43Challenge.ts43_type)
            }

            val ts43Client = Ts43Client(context)
            val result = ts43Client.performEntitlementCheckResult(
                subId, phoneNumber ?: "", "", "",
                entitlementUrl, ts43Challenge.eap_aka_realm
            )
            Log.i(TAG, "  TS43: result ineligible=${result.ineligible} error=${result.isError} token=${result.token?.length ?: 0} chars")

            if (result.isError) {
                return ts43InternalError(ts43Challenge.ts43_type)
            }

            // Route result to correct proto field based on challenge type
            val responseToken = result.token ?: ""
            val ts43Response = if (ts43Challenge.server_challenge != null) {
                // Server challenge: return in server_challenge_response
                Ts43ChallengeResponse(
                    ts43_type = ts43Challenge.ts43_type,
                    server_challenge_response = ServerChallengeResponse(
                        acquire_temporary_token_response = responseToken
                    )
                )
            } else {
                // Client challenge (default): return in client_challenge_response
                Ts43ChallengeResponse(
                    ts43_type = ts43Challenge.ts43_type,
                    client_challenge_response = ClientChallengeResponse(
                        get_phone_number_response = responseToken
                    )
                )
            }
            return ChallengeResponse(
                ts43_challenge_response = ts43Response
            )
        } catch (e: Exception) {
            Log.e(TAG, "  TS43: exception: ${e.javaClass.simpleName}: ${e.message}")
            return ts43InternalError(ts43Challenge.ts43_type)
        }
    }
}
