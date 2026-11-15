/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.verification

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import org.microg.gms.constellation.core.proto.ChallengeResponse
import org.microg.gms.constellation.core.proto.FlashCallChallenge
import org.microg.gms.constellation.core.proto.FlashCallChallengeResponse
import org.microg.gms.constellation.core.proto.PhoneRange
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "FlashCallVerifier"
private val DEFAULT_FLASH_CALL_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(2)

/**
 * Waits for an incoming call from one of the server-provided [FlashCallChallenge.phone_ranges] and
 * reports the observed caller number. The call itself is never answered or ended; only the
 * platform phone state broadcast is observed.
 */
suspend fun FlashCallChallenge.verify(
    context: Context,
    subId: Int,
    timeoutMillis: Long? = null
): ChallengeResponse {
    if (phone_ranges.isEmpty()) {
        Log.w(TAG, "Flash call challenge has no phone ranges")
        return flashCallResponse(error = FlashCallChallengeResponse.Error.PRECONDITIONS_FAILED)
    }
    if (!hasFlashCallPermissions(context)) {
        Log.w(TAG, "Permission not granted")
        return flashCallResponse(error = FlashCallChallengeResponse.Error.PRECONDITIONS_FAILED)
    }

    val countryIso = context.getSystemService<TelephonyManager>()
        ?.let { tm ->
            if (subId != -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                tm.createForSubscriptionId(subId)
            } else {
                tm
            }
        }
        ?.let { it.simCountryIso.takeUnless { iso -> iso.isNullOrEmpty() } ?: it.networkCountryIso }
        ?.uppercase(Locale.ROOT)
        .orEmpty()

    val matchedCaller = CompletableDeferred<String>()
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
            if (intent.getStringExtra(TelephonyManager.EXTRA_STATE) != TelephonyManager.EXTRA_STATE_RINGING) return

            val receivedSubId = intent.getIntExtra(
                "android.telephony.extra.SUBSCRIPTION_INDEX",
                intent.getIntExtra("subscription", -1)
            )
            if (subId != -1 && receivedSubId != -1 && receivedSubId != subId) return

            @Suppress("DEPRECATION")
            val caller = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            if (caller.isNullOrEmpty()) return
            if (phone_ranges.none { it.matches(caller, countryIso) }) {
                Log.d(TAG, "Ignoring incoming call that does not match the flash call ranges")
                return
            }
            matchedCaller.complete(caller)
        }
    }

    ContextCompat.registerReceiver(
        context,
        receiver,
        IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED),
        ContextCompat.RECEIVER_EXPORTED
    )
    try {
        val effectiveTimeoutMillis = timeoutMillis?.coerceAtLeast(0L) ?: DEFAULT_FLASH_CALL_TIMEOUT_MILLIS
        Log.d(TAG, "Waiting up to ${effectiveTimeoutMillis}ms for flash call")
        val caller = withTimeoutOrNull(effectiveTimeoutMillis) { matchedCaller.await() }
        if (caller == null) {
            Log.w(TAG, "Timed out waiting for flash call")
            return flashCallResponse(error = FlashCallChallengeResponse.Error.TIMED_OUT)
        }
        Log.d(TAG, "Matching flash call received")
        return flashCallResponse(caller = caller)
    } finally {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
        }
    }
}

private fun hasFlashCallPermissions(context: Context): Boolean {
    val required = buildList {
        add(Manifest.permission.READ_PHONE_STATE)
        // Since Android 9 the incoming number is only included for holders of READ_CALL_LOG.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) add(Manifest.permission.READ_CALL_LOG)
    }
    return required.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

internal fun PhoneRange.matches(caller: String, defaultCountryIso: String): Boolean {
    val prefix = phone_number_prefix.digitsOnly()
    val suffix = phone_number_suffix.digitsOnly()
    if (prefix.isEmpty() && suffix.isEmpty()) return false

    val regionIso = country_code.takeIf { it.length == 2 && it.all(Char::isLetter) }
        ?.uppercase(Locale.ROOT)
        ?: defaultCountryIso
    val candidates = linkedSetOf(caller.digitsOnly())
    // formatNumberToE164 is API 21; constellation-core minSdk is 19. On KitKat, compare digits only.
    if (regionIso.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        PhoneNumberUtils.formatNumberToE164(caller, regionIso)?.let { candidates += it.digitsOnly() }
    }
    return candidates.any { it.isNotEmpty() && it.startsWith(prefix) && it.endsWith(suffix) }
}

private fun String.digitsOnly(): String = filter(Char::isDigit)

private fun flashCallResponse(
    caller: String = "",
    error: FlashCallChallengeResponse.Error = FlashCallChallengeResponse.Error.NO_ERROR
) = ChallengeResponse(
    flash_call_response = FlashCallChallengeResponse(caller = caller, error = error)
)
