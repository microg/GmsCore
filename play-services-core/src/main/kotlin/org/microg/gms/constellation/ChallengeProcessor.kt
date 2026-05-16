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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import okio.ByteString.Companion.toByteString
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume

private const val TAG = "GmsConstellationChallenge"

/** OTP SMS data: full body + sender address. */
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
        Log.d(TAG, "SmsInbox: preparing receivers")

        silentReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                Log.d(TAG, "SILENT_SMS_RECEIVED")
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
            Log.d(TAG, "SmsInbox: receivers registered")
        } else {
            Log.d(TAG, "SmsInbox: silent receiver only (RECEIVE_SMS not granted)")
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
        Log.d(TAG, "SmsInbox: SMS received via $source")
        synchronized(lock) {
            bufferedMessages.add(sms)
            pendingFuture?.let { future ->
                if (!future.isDone) {
                    Log.d(TAG, "SmsInbox: completing pending future")
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
                Log.d(TAG, "SmsInbox: using buffered SMS")
                return first
            }
            pendingFuture = CompletableFuture<OtpSmsResult>()
        }
        Log.d(TAG, "SmsInbox: waiting ${timeoutSeconds}s for OTP")
        return try {
            pendingFuture!!.get(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS).also {
                Log.i(TAG, "SmsInbox: OTP received")
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

        Log.d(TAG, "MO SMS: sending verification SMS")

        val port = moChallenge.data_sms_info?.port ?: 0
        return kotlinx.coroutines.withTimeoutOrNull(30_000L) {
            kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                val receiver = object : android.content.BroadcastReceiver() {
                    override fun onReceive(ctx: android.content.Context, intent: android.content.Intent) {
                        if (intent.getStringExtra("message_id") != messageId) return
                        val resultCode = resultCode
                        val errorCode = intent.getIntExtra("errorCode", -1)
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
        if (challengeData.startsWith("[ts43]")) {
            Log.d(TAG, "CarrierID: [ts43] prefix, returning NOT_SUPPORTED")
            return carrierIdError(CarrierIdError.CARRIER_ID_ERROR_NOT_SUPPORTED)
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
                Log.i(TAG, "CarrierID: verification succeeded")
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
     * Verify via RegisteredSMS: hash SMS inbox entries and compare against server-provided payloads.
     * Algorithm: SHA-512(timeBucket + SHA-512(localNumber) + SHA-512(sender) + body)
     */
    fun verifyRegisteredSms(
        context: Context,
        challenge: RegisteredSMSChallenge,
        subId: Int
    ): ChallengeResponse {
        val expectedPayloads = challenge.verified_senders
            .map { it.phone_number_id.toByteArray() }
            .filter { it.isNotEmpty() }
        if (expectedPayloads.isEmpty()) {
            Log.w(TAG, "RegisteredSMS: no verified_senders in challenge")
            return ChallengeResponse(registered_sms_challenge_response = RegisteredSMSChallengeResponse(items = emptyList()))
        }

        if (context.checkCallingOrSelfPermission(android.Manifest.permission.READ_SMS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "RegisteredSMS: READ_SMS permission not granted")
            return ChallengeResponse(registered_sms_challenge_response = RegisteredSMSChallengeResponse(items = emptyList()))
        }

        val localNumber = getLocalNumber(context, subId)
        if (localNumber.isEmpty()) {
            Log.w(TAG, "RegisteredSMS: no local phone number available")
            return ChallengeResponse(registered_sms_challenge_response = RegisteredSMSChallengeResponse(items = emptyList()))
        }

        val historyWindowMs = 168L * 3600_000L // 7 days
        val granularityMs = 3600_000L / 2 // 30 min buckets
        val historyStart = System.currentTimeMillis() - historyWindowMs

        val matchedItems = mutableListOf<RegisteredSmsPayload>()
        try {
            val cursor = context.contentResolver.query(
                android.provider.Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf("date", "address", "body"),
                "date > ?",
                arrayOf(historyStart.toString()),
                "date DESC"
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val date = it.getLong(0)
                    val sender = it.getString(1) ?: continue
                    val body = it.getString(2) ?: continue
                    val bucketStart = date - (date % granularityMs)
                    val payload = computeRegisteredSmsPayload(bucketStart, localNumber, sender, body)
                    if (expectedPayloads.any { expected -> expected.contentEquals(payload) }) {
                        matchedItems += RegisteredSmsPayload(payload = payload.toByteString())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "RegisteredSMS: inbox query failed: ${e.message}")
        }

        Log.d(TAG, "RegisteredSMS: ${matchedItems.size} matches found")
        return ChallengeResponse(registered_sms_challenge_response = RegisteredSMSChallengeResponse(items = matchedItems.distinct()))
    }

    private fun computeRegisteredSmsPayload(bucketStart: Long, localNumber: String, sender: String, body: String): ByteArray {
        val digest = java.security.MessageDigest.getInstance("SHA-512")
        digest.update(bucketStart.toString().toByteArray(Charsets.UTF_8))
        digest.update(java.security.MessageDigest.getInstance("SHA-512").digest(localNumber.toByteArray(Charsets.UTF_8)))
        digest.update(java.security.MessageDigest.getInstance("SHA-512").digest(sender.toByteArray(Charsets.UTF_8)))
        digest.update(body.toByteArray(Charsets.UTF_8))
        return digest.digest()
    }

    @Suppress("DEPRECATION")
    private fun getLocalNumber(context: Context, subId: Int): String {
        try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? android.telephony.SubscriptionManager
            val info = sm?.activeSubscriptionInfoList?.find { it.subscriptionId == subId }
            val number = info?.number
            if (!number.isNullOrEmpty()) return number
            val tm = (context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager)
                ?.createForSubscriptionId(subId)
            return tm?.line1Number ?: ""
        } catch (e: Exception) {
            Log.w(TAG, "RegisteredSMS: cannot get local number: ${e.message}")
            return ""
        }
    }

    /**
     * Verify via FlashCall: wait for incoming call from a number within server-provided phone_ranges.
     * Server calls user's phone briefly (flash), client proves it saw the call from a number in range.
     */
    @Suppress("DEPRECATION")
    suspend fun verifyFlashCall(
        context: Context,
        challenge: FlashCallChallenge,
        timeoutMs: Long
    ): ChallengeResponse? {
        val ranges = challenge.phone_ranges
        if (ranges.isEmpty()) {
            Log.w(TAG, "FlashCall: no phone_ranges in challenge")
            return null
        }

        val waitMs = (timeoutMs.takeIf { it > 0 } ?: 30_000L).coerceIn(10_000, 120_000)
        Log.d(TAG, "FlashCall: waiting ${waitMs}ms for call from ${ranges.size} range(s)")

        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
        if (tm == null) {
            Log.w(TAG, "FlashCall: no TelephonyManager")
            return null
        }

        val result = withTimeoutOrNull(waitMs) {
            suspendCancellableCoroutine<ChallengeResponse?> { continuation ->
                val listener = object : android.telephony.PhoneStateListener() {
                    override fun onCallStateChanged(state: Int, incomingNumber: String?) {
                        if (state == android.telephony.TelephonyManager.CALL_STATE_RINGING && !incomingNumber.isNullOrEmpty()) {
                            val normalized = incomingNumber.replace(Regex("[^0-9+]"), "")
                            if (matchesPhoneRanges(normalized, ranges)) {
                                Log.i(TAG, "FlashCall: matched incoming call")
                                tm.listen(this, android.telephony.PhoneStateListener.LISTEN_NONE)
                                if (continuation.isActive) {
                                    continuation.resume(ChallengeResponse(
                                        flash_call_challenge_response = FlashCallChallengeResponse(caller = normalized)
                                    ))
                                }
                            }
                        }
                    }
                }
                tm.listen(listener, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
                continuation.invokeOnCancellation {
                    tm.listen(listener, android.telephony.PhoneStateListener.LISTEN_NONE)
                }
            }
        }
        if (result == null) {
            Log.w(TAG, "FlashCall: timeout after ${waitMs}ms")
        }
        return result
    }

    private fun matchesPhoneRanges(number: String, ranges: List<PhoneRange>): Boolean {
        val digits = number.removePrefix("+")
        return ranges.any { range ->
            val prefix = (range.country_code ?: "") + (range.prefix ?: "")
            if (!digits.startsWith(prefix)) return@any false
            val suffix = digits.removePrefix(prefix)
            val lower = range.lower_bound ?: return@any true
            val upper = range.upper_bound ?: lower
            suffix >= lower && suffix <= upper
        }
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
                Log.w(TAG, "TS43: empty entitlement_url")
                return ts43InternalError(ts43Challenge.ts43_type)
            }

            val ts43Client = Ts43Client(context)
            val odsaOp = ts43Challenge.client_challenge?.get_phone_number_operation
                ?: ts43Challenge.server_challenge?.acquire_temporary_token_operation
            val result = ts43Client.performEntitlementCheckResult(
                subId, phoneNumber ?: "", "", "",
                entitlementUrl, ts43Challenge.eap_aka_realm,
                ts43Challenge.service_entitlement_request,
                odsaOp,
                ts43Challenge.app_id
            )
            Log.i(TAG, "TS43: result ineligible=${result.ineligible} error=${result.isError}")

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
            Log.e(TAG, "TS43: ${e.javaClass.simpleName}: ${e.message}")
            return ts43InternalError(ts43Challenge.ts43_type)
        }
    }
}
