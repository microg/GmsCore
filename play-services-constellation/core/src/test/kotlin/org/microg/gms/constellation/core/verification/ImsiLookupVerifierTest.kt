/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.verification

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito
import org.microg.gms.constellation.core.proto.Challenge
import org.microg.gms.constellation.core.proto.ChallengeID
import org.microg.gms.constellation.core.proto.ChallengeResponse
import org.microg.gms.constellation.core.proto.VerificationMethod

class ImsiLookupVerifierTest {

    /** Context is only ever handed to the IMSI reader, which the fakes ignore. */
    private val context: Context = Mockito.mock(Context::class.java)

    private val challenge = Challenge(
        challenge_id = ChallengeID(id = "challenge-1"),
        type = VerificationMethod.IMSI_LOOKUP,
    )

    private fun reader(result: String?) = ImsiReader { _, _ -> result }

    @Test
    fun matchingImsi_returnsProceedResponse() {
        val response = challenge.verifyImsiLookup(
            context,
            expectedImsi = "310260123456789",
            subId = 3,
            readImsi = reader("310260123456789"),
        )

        assertEquals(ChallengeResponse(), response)
    }

    @Test
    fun unreadableImsi_returnsNoResponse() {
        // PrivilegedImsiReader yields null when the privileged read is refused;
        // the verifier must refuse to proceed rather than send an empty confirm.
        val response = challenge.verifyImsiLookup(
            context,
            expectedImsi = "310260123456789",
            subId = 3,
            readImsi = reader(null),
        )

        assertNull(response)
    }

    @Test
    fun noActiveSubscription_returnsNoResponse() {
        var consulted = false
        val response = challenge.verifyImsiLookup(
            context,
            expectedImsi = "310260123456789",
            subId = -1,
            readImsi = ImsiReader { _, _ -> consulted = true; "310260123456789" },
        )

        assertNull(response)
        assertTrue("IMSI must not be read without a matching subscription", !consulted)
    }

    @Test
    fun imsiNoLongerMatchesAssociation_returnsNoResponse() {
        val response = challenge.verifyImsiLookup(
            context,
            expectedImsi = "310260123456789",
            subId = 3,
            readImsi = reader("310260999999999"),
        )

        assertNull(response)
    }

    @Test
    fun privilegedReader_withoutTelephonyManager_returnsNull() {
        // The mock Context has no TelephonyManager, which is the one branch of
        // the production reader reachable without a real platform.
        assertNull(PrivilegedImsiReader.read(context, 3))
    }

    @Test
    fun associationWithoutImsi_returnsNoResponse() {
        val response = challenge.verifyImsiLookup(
            context,
            expectedImsi = null,
            subId = 3,
            readImsi = reader("310260123456789"),
        )

        assertNull(response)
    }
}
