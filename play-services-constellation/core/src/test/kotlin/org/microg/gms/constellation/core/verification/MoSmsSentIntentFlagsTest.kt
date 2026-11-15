/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.verification

import android.Manifest
import android.app.Application
import android.app.PendingIntent
import android.telephony.SmsManager
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.microg.gms.constellation.core.proto.MoChallenge
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The telephony stack reports the MO send failure cause by filling in the "errorCode" extra on the
 * sent PendingIntent, and fill-in extras are dropped for an immutable PendingIntent. Pin the flags
 * so the receiver can keep reading a real sms_error_code instead of -1.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MoSmsSentIntentFlagsTest {
    @Test
    fun `sent PendingIntent is mutable so the errorCode fill-in survives`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(context).grantPermissions(Manifest.permission.SEND_SMS)
        val challenge = MoChallenge(proxy_number = "+15550100", sms = "challenge-body")

        val job = CoroutineScope(Dispatchers.Unconfined).launch {
            challenge.startSession(context, challengeId = "challenge-id", subId = -1)
        }
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            val params = shadowOf(smsManager).lastSentTextMessageParams
            assertNotNull("production code did not send the MO SMS", params)
            assertEquals("+15550100", params.destinationAddress)

            val flags = shadowOf(params.sentIntent).flags
            assertEquals(
                "sent PendingIntent must not be immutable",
                0,
                flags and PendingIntent.FLAG_IMMUTABLE
            )
            assertEquals(
                "sent PendingIntent must be explicitly mutable on S+",
                PendingIntent.FLAG_MUTABLE,
                flags and PendingIntent.FLAG_MUTABLE
            )
            assertEquals(
                "sent PendingIntent must stay one-shot",
                PendingIntent.FLAG_ONE_SHOT,
                flags and PendingIntent.FLAG_ONE_SHOT
            )
        } finally {
            job.cancel()
        }
    }
}
