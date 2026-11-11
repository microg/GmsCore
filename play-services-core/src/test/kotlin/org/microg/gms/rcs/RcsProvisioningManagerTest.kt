/*
 * SPDX-FileCopyrightText: 2024, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RcsProvisioningManagerTest {
    @Test
    fun testNormalizeE164PhoneNumber() {
        assertEquals("+14155550199", RcsProvisioningManager.normalizeE164PhoneNumber("(415) 555-0199", "1"))
        assertEquals("+447911123456", RcsProvisioningManager.normalizeE164PhoneNumber("+44 7911 123456"))
        assertNull(RcsProvisioningManager.normalizeE164PhoneNumber("123"))
    }

    @Test
    fun testBuildAcsConfigUrlFollows3gppFormat() {
        val cfg = RcsProvisioningManager.buildAcsConfigUrl(310, 260)
        assertEquals("https://config.rcs.mnc260.mcc310.pub.3gppnetwork.org/", cfg.acsUrl)
        assertEquals("ims.mnc260.mcc310.3gppnetwork.org", cfg.imsDomain)
    }

    @Test
    fun testVerifyPhoneNumberAndBuildWapProvisioningXml() {
        val res = RcsProvisioningManager.verifyPhoneNumberForRcs("+14155550199", "sim-iccid-01", 310, 260)
        assertNotNull(res)
        assertEquals(RcsProvisioningManager.STATE_PROVISIONED, res!!.provisioningState)
        assertTrue(res.verificationToken.startsWith("rcs_constellation_v1_"))

        val carrier = RcsProvisioningManager.buildAcsConfigUrl(310, 260)
        val xml = RcsProvisioningManager.buildWapProvisioningXml(res, carrier)
        assertTrue(xml.contains("<wap-provisioningdoc"))
        assertTrue(xml.contains("ap2002"))
        assertTrue(xml.contains(res.verificationToken))
    }
}
