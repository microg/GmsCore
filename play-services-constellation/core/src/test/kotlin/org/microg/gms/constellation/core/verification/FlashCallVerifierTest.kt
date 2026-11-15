/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.verification

import android.Manifest
import android.app.Application
import android.content.Intent
import android.os.Looper
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.microg.gms.constellation.core.proto.ChallengeResponse
import org.microg.gms.constellation.core.proto.FlashCallChallenge
import org.microg.gms.constellation.core.proto.FlashCallChallengeResponse
import org.microg.gms.constellation.core.proto.PhoneRange
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FlashCallVerifierTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    private val challenge = FlashCallChallenge(
        phone_ranges = listOf(
            PhoneRange(phone_number_prefix = "+4930", phone_number_suffix = "", country_code = "DE")
        )
    )

    @Before
    fun setUp() {
        shadowOf(application).grantPermissions(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG
        )
    }

    // Starts verification on the test thread so the receiver is registered before returning;
    // broadcasts are then delivered by idling the main looper.
    @OptIn(DelicateCoroutinesApi::class)
    private fun startVerify(timeoutMillis: Long, subId: Int = -1): Deferred<ChallengeResponse> =
        GlobalScope.async(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
            challenge.verify(application, subId, timeoutMillis)
        }

    private fun ring(number: String, subId: Int? = null) {
        val intent = Intent(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            .putExtra(TelephonyManager.EXTRA_STATE, TelephonyManager.EXTRA_STATE_RINGING)
            .putExtra(TelephonyManager.EXTRA_INCOMING_NUMBER, number)
        if (subId != null) intent.putExtra("subscription", subId)
        application.sendBroadcast(intent)
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun flashCallReceiverRegistered(): Boolean =
        shadowOf(application).registeredReceivers.any {
            it.intentFilter.hasAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        }

    @Test
    fun matchingCallerIsReported() {
        val result = startVerify(timeoutMillis = 60_000)
        assertTrue(flashCallReceiverRegistered())

        ring("+49 30 1234567")

        val response = runBlocking { result.await() }.flash_call_response
        assertEquals("+49 30 1234567", response?.caller)
        assertEquals(FlashCallChallengeResponse.Error.NO_ERROR, response?.error)
        assertFalse("receiver must be unregistered after a match", flashCallReceiverRegistered())
    }

    @Test
    fun nationalFormatCallerMatchesThroughRangeCountry() {
        val result = startVerify(timeoutMillis = 60_000)

        ring("030 1234567")

        val response = runBlocking { result.await() }.flash_call_response
        assertEquals("030 1234567", response?.caller)
        assertEquals(FlashCallChallengeResponse.Error.NO_ERROR, response?.error)
    }

    @Test
    fun nonMatchingCallerIsIgnoredUntilMatchingCallArrives() {
        val result = startVerify(timeoutMillis = 60_000)

        ring("+44 20 7946 0000")
        assertFalse("a non-matching caller must not complete the challenge", result.isCompleted)

        ring("+49 30 7654321")

        val response = runBlocking { result.await() }.flash_call_response
        assertEquals("+49 30 7654321", response?.caller)
        assertEquals(FlashCallChallengeResponse.Error.NO_ERROR, response?.error)
    }

    @Test
    fun callOnOtherSubscriptionIsIgnored() {
        val result = startVerify(timeoutMillis = 300, subId = 1)

        ring("+49 30 1234567", subId = 2)

        val response = runBlocking { result.await() }.flash_call_response
        assertEquals("", response?.caller)
        assertEquals(FlashCallChallengeResponse.Error.TIMED_OUT, response?.error)
    }

    @Test
    fun nonMatchingCallerThenTimeoutReportsTimedOut() {
        val result = startVerify(timeoutMillis = 300)

        ring("+44 20 7946 0000")

        val response = runBlocking { result.await() }.flash_call_response
        assertEquals("", response?.caller)
        assertEquals(FlashCallChallengeResponse.Error.TIMED_OUT, response?.error)
        assertFalse("receiver must be unregistered after a timeout", flashCallReceiverRegistered())
    }

    @Test
    fun missingCallLogPermissionFailsPreconditions() {
        shadowOf(application).denyPermissions(Manifest.permission.READ_CALL_LOG)

        val response = runBlocking { challenge.verify(application, -1, 60_000) }.flash_call_response

        assertEquals("", response?.caller)
        assertEquals(FlashCallChallengeResponse.Error.PRECONDITIONS_FAILED, response?.error)
        assertFalse(flashCallReceiverRegistered())
    }

    @Test
    fun challengeWithoutRangesFailsPreconditions() {
        val response = runBlocking {
            FlashCallChallenge().verify(application, -1, 60_000)
        }.flash_call_response

        assertEquals(FlashCallChallengeResponse.Error.PRECONDITIONS_FAILED, response?.error)
    }
}
