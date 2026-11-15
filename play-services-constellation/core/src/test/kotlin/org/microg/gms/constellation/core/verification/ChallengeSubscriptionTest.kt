/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.constellation.core.verification

import android.Manifest
import android.app.Application
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.microg.gms.constellation.core.proto.Challenge
import org.microg.gms.constellation.core.proto.MoChallenge
import org.microg.gms.constellation.core.proto.PendingVerificationInfo
import org.microg.gms.constellation.core.proto.SIMAssociation
import org.microg.gms.constellation.core.proto.Verification
import org.microg.gms.constellation.core.proto.VerificationAssociation
import org.microg.gms.constellation.core.proto.VerificationMethod
import org.microg.gms.constellation.core.proto.builder.RequestBuildContext
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChallengeSubscriptionTest {
    @Test fun unknownChallengeSim_cannotSendSmsOnTheDefaultSim() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(context).grantPermissions(Manifest.permission.SEND_SMS)
        val verification = Verification(
            association = VerificationAssociation(sim = SIMAssociation(
                sim_info = SIMAssociation.SIMInfo(imsi = listOf("unknown-sim"))
            )),
            pending_verification_info = PendingVerificationInfo(challenge = Challenge(
                type = VerificationMethod.MO_SMS,
                mo_challenge = MoChallenge(proxy_number = "+15550100", sms = "challenge")
            ))
        )
        val result = withTimeout(1_000) {
            ChallengeProcessor.process(context, "session", mapOf("other-sim" to mock(SubscriptionInfo::class.java)),
                RequestBuildContext("test-iid", emptyList()), verification)
        }
        assertSame(verification, result)
        assertNull(shadowOf(context.getSystemService(SmsManager::class.java)).lastSentTextMessageParams)
    }
}
