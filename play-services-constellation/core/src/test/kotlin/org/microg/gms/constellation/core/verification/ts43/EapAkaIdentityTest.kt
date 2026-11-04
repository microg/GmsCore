/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.verification.ts43

import org.junit.Assert.assertEquals
import org.junit.Test

class EapAkaIdentityTest {
    @Test
    fun buildEapIdentity_usesDefaultRealm() {
        assertEquals(
            "0234107813240779@nai.epc.mnc010.mcc234.3gppnetwork.org",
            buildEapIdentity("23410", "234107813240779")
        )
    }

    @Test
    fun buildEapIdentity_zeroPadsTwoDigitMnc() {
        assertEquals(
            "0310123456789012@nai.epc.mnc010.mcc310.3gppnetwork.org",
            buildEapIdentity("31010", "310123456789012")
        )
    }

    @Test
    fun buildEapIdentity_preservesChallengeRealm() {
        assertEquals(
            "0234107813240779@wlan.mnc010.mcc234.3gppnetwork.org",
            buildEapIdentity(
                "23410",
                "234107813240779",
                "wlan.mnc010.mcc234.3gppnetwork.org"
            )
        )
    }

    @Test
    fun buildEapIdentity_expandsBareNaiEpcRealm() {
        assertEquals(
            "0234107813240779@nai.epc.mnc010.mcc234.3gppnetwork.org",
            buildEapIdentity("23410", "234107813240779", "nai.epc")
        )
    }
}
