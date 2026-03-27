@file:RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)

package org.microg.gms.constellation.core.verification

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import okio.ByteString.Companion.toByteString
import org.microg.gms.constellation.core.VerificationSettingsPhenotypes
import org.microg.gms.constellation.core.proto.ChallengeResponse
import org.microg.gms.constellation.core.proto.RegisteredSmsChallenge
import org.microg.gms.constellation.core.proto.RegisteredSmsChallengeResponse
import org.microg.gms.constellation.core.proto.RegisteredSmsChallengeResponseItem
import org.microg.gms.constellation.core.proto.RegisteredSmsChallengeResponsePayload
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

private const val TAG = "RegisteredSmsVerifier"

fun RegisteredSmsChallenge.verify(context: Context, subId: Int): ChallengeResponse {
    val expectedPayloads = verified_senders
        .map { it.phone_number_id.toByteArray() }
        .filter { it.isNotEmpty() }
    if (expectedPayloads.isEmpty()) return ChallengeResponse(
        registered_sms_response = RegisteredSmsChallengeResponse(
            items = emptyList()
        )
    )

    val localNumbers = getLocalNumbers(context, subId)
    if (localNumbers.isEmpty()) {
        Log.w(TAG, "No local phone numbers available for registered SMS verification")
        return ChallengeResponse(registered_sms_response = RegisteredSmsChallengeResponse(items = emptyList()))
    }

    val historyStart = System.currentTimeMillis() -
            TimeUnit.HOURS.toMillis(VerificationSettingsPhenotypes.A2P_HISTORY_WINDOW_HOURS)
    val bucketSizeMillis = (TimeUnit.HOURS.toMillis(
        VerificationSettingsPhenotypes.A2P_SMS_SIGNAL_GRANULARITY_HRS
    ) / 2).coerceAtLeast(1L)

    val responseItems = mutableListOf<RegisteredSmsChallengeResponseItem>()

    try {
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf("date", "address", "body", "sub_id"),
            "date > ?",
            arrayOf(historyStart.toString()),
            "date DESC"
        ) ?: return ChallengeResponse(
            registered_sms_response = RegisteredSmsChallengeResponse(
                items = emptyList()
            )
        )

        cursor.use {
            while (it.moveToNext()) {
                val date = it.getLong(it.getColumnIndexOrThrow("date"))
                val sender = it.getString(it.getColumnIndexOrThrow("address")) ?: continue
                val body = it.getString(it.getColumnIndexOrThrow("body")) ?: continue
                val messageSubId = runCatching {
                    it.getInt(it.getColumnIndexOrThrow("sub_id"))
                }.getOrDefault(-1)

                val candidateNumbers = if (subId != -1 && messageSubId == subId) {
                    localNumbers
                } else if (messageSubId == -1) {
                    localNumbers
                } else {
                    getLocalNumbers(context, messageSubId).ifEmpty { localNumbers }
                }

                val bucketStart = date - (date % bucketSizeMillis)
                for (localNumber in candidateNumbers) {
                    val payload = computePayload(bucketStart, localNumber, sender, body)
                    if (expectedPayloads.any { expected -> expected.contentEquals(payload) }) {
                        responseItems += RegisteredSmsChallengeResponseItem(
                            payload = RegisteredSmsChallengeResponsePayload(
                                payload = payload.toByteString()
                            )
                        )
                    }
                }
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Registered SMS verification failed", e)
        return ChallengeResponse(registered_sms_response = RegisteredSmsChallengeResponse(items = emptyList()))
    }

    return ChallengeResponse(
        registered_sms_response = RegisteredSmsChallengeResponse(items = responseItems.distinct())
    )
}

@SuppressLint("HardwareIds")
private fun getLocalNumbers(context: Context, targetSubId: Int): List<String> {
    val numbers = linkedSetOf<String>()
    val subscriptionManager =
        context.getSystemService<SubscriptionManager>()
    val telephonyManager = context.getSystemService<TelephonyManager>()

    val hasState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    val isCarrier = telephonyManager?.hasCarrierPrivileges() == true
    val hasNumbers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED
    } else {
        hasState
    }

    if (!isCarrier && (!hasState || !hasNumbers)) {
        Log.e(TAG, "Permission not granted")
        return emptyList()
    }

    try {
        subscriptionManager?.activeSubscriptionInfoList.orEmpty().forEach { info ->
            if (targetSubId != -1 && info.subscriptionId != targetSubId) return@forEach
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && subscriptionManager != null) {
                numbers += subscriptionManager.getPhoneNumber(
                    info.subscriptionId,
                    SubscriptionManager.PHONE_NUMBER_SOURCE_CARRIER
                )
                numbers += subscriptionManager.getPhoneNumber(
                    info.subscriptionId,
                    SubscriptionManager.PHONE_NUMBER_SOURCE_UICC
                )
                numbers += subscriptionManager.getPhoneNumber(
                    info.subscriptionId,
                    SubscriptionManager.PHONE_NUMBER_SOURCE_IMS
                )
            }
            val targetManager =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    telephonyManager?.createForSubscriptionId(info.subscriptionId)
                } else {
                    telephonyManager
                }
            numbers += targetManager?.line1Number.orEmpty()
            numbers += info.number.orEmpty()
        }
    } catch (e: Exception) {
        Log.w(TAG, "Unable to collect local phone numbers", e)
    }

    return numbers.filter { it.isNotBlank() }.distinct()
}

private fun computePayload(
    bucketStart: Long,
    localNumber: String,
    sender: String,
    body: String
): ByteArray {
    val digest = MessageDigest.getInstance("SHA-512")
    digest.update(bucketStart.toString().toByteArray(StandardCharsets.UTF_8))
    digest.update(hashUtf8(localNumber))
    digest.update(hashUtf8(sender))
    digest.update(body.toByteArray(StandardCharsets.UTF_8))
    return digest.digest()
}

private fun hashUtf8(value: String): ByteArray {
    return MessageDigest.getInstance("SHA-512")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
}
