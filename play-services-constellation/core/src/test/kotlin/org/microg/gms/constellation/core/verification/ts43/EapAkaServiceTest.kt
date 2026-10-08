/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.verification.ts43

import android.telephony.TelephonyManager
import android.util.Base64
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EapAkaServiceTest {
    private val res = ByteArray(8) { (0xA0 + it).toByte() }
    private val ck = ByteArray(16) { (0x10 + it).toByte() }
    private val ik = ByteArray(16) { (0x30 + it).toByte() }

    private fun challengePacket(
        id: Byte,
        identity: String,
        includeMac: Boolean = true,
        corruptMac: Boolean = false,
    ): String {
        val rand = ByteArray(16) { 0x55 }
        val autn = ByteArray(16) { 0x66 }
        val attrs = byteArrayOf(1, 5, 0, 0) + rand + byteArrayOf(2, 5, 0, 0) + autn
        val macAttr = if (includeMac) byteArrayOf(11, 5, 0, 0) + ByteArray(16) else byteArrayOf()
        val len = 8 + attrs.size + macAttr.size
        val header = byteArrayOf(1, id, (len shr 8).toByte(), len.toByte(), 23, 1, 0, 0)
        val packet = header + attrs + macAttr
        if (includeMac) {
            val kAut = Fips186Prf.deriveKeys(identity.toByteArray(Charsets.UTF_8), ik, ck)["K_aut"]!!
            val tag = Mac.getInstance("HmacSHA1").apply {
                init(SecretKeySpec(kAut, "HmacSHA1"))
            }.doFinal(packet).copyOf(16)
            System.arraycopy(tag, 0, packet, packet.size - tag.size, tag.size)
            if (corruptMac) {
                packet[packet.lastIndex] = (packet.last().toInt() xor 1).toByte()
            }
        }
        return Base64.encodeToString(packet, Base64.NO_WRAP)
    }

    private fun simWithAkaSuccess(): TelephonyManager {
        val sim = byteArrayOf(0xDB.toByte(), res.size.toByte()) + res +
                byteArrayOf(ck.size.toByte()) + ck + byteArrayOf(ik.size.toByte()) + ik
        val tm = Mockito.mock(TelephonyManager::class.java)
        Mockito.`when`(tm.getIccAuthentication(anyInt(), anyInt(), anyString()))
            .thenReturn(Base64.encodeToString(sim, Base64.NO_WRAP))
        return tm
    }

    private fun macVerifies(packet: ByteArray, identity: String): Boolean {
        val kAut = Fips186Prf.deriveKeys(identity.toByteArray(Charsets.UTF_8), ik, ck)["K_aut"]!!
        // AT_MAC is the trailing 20-byte attribute; its 16-byte value is computed over the packet with the value zeroed
        val macOffset = packet.size - 16
        val received = packet.copyOfRange(macOffset, packet.size)
        val zeroed = packet.copyOf().also { it.fill(0, macOffset, packet.size) }
        val expected = Mac.getInstance("HmacSHA1").apply { init(SecretKeySpec(kAut, "HmacSHA1")) }
            .doFinal(zeroed).copyOf(16)
        return expected.contentEquals(received)
    }

    @Test
    fun rejectsMissingOrTamperedMandatoryServerChallengeMac() {
        val service = EapAkaService(simWithAkaSuccess())
        val eapId = service.buildEapId("310260", "310260123456789")
        assertNull(service.performSimAkaAuth(challengePacket(0x42, eapId, includeMac = false), eapId))
        assertNull(service.performSimAkaAuth(challengePacket(0x42, eapId, corruptMac = true), eapId))
        val wrongIdentity = service.buildEapId("310260", "310260123456789", "carrier.example")
        assertNull(service.performSimAkaAuth(challengePacket(0x42, eapId), wrongIdentity))
    }

    @Test
    fun rejectsPacketWhoseAdvertisedEapLengthDoesNotMatchBody() {
        val service = EapAkaService(simWithAkaSuccess())
        val eapId = service.buildEapId("310260", "310260123456789")
        val packet = Base64.decode(challengePacket(0x42, eapId), Base64.DEFAULT)
        packet[3] = (packet[3].toInt() - 1).toByte()
        assertNull(service.performSimAkaAuth(Base64.encodeToString(packet, Base64.NO_WRAP), eapId))
    }

    @Test
    fun challengeResponse_macIsKeyedOnAnnouncedIdentityWithCarrierRealm() {
        val service = EapAkaService(simWithAkaSuccess())
        val eapId = service.buildEapId("310260", "310260123456789", "nai.epc.carrier.example")
        assertEquals("0310260123456789@nai.epc.carrier.example", eapId)

        val response = service.performSimAkaAuth(challengePacket(0x42, eapId), eapId)
        assertNotNull(response)
        val packet = Base64.decode(response, Base64.DEFAULT)

        assertEquals(2, packet[0].toInt())
        assertEquals(0x42, packet[1].toInt())
        assertEquals(packet.size, ((packet[2].toInt() and 0xFF) shl 8) or (packet[3].toInt() and 0xFF))
        assertEquals(3, packet[8].toInt()) // AT_RES
        assertEquals(res.size * 8, ((packet[10].toInt() and 0xFF) shl 8) or (packet[11].toInt() and 0xFF))
        assertArrayEquals(res, packet.copyOfRange(12, 12 + res.size))

        assert(macVerifies(packet, eapId)) { "AT_MAC must verify under K_aut derived from the EAP_ID sent to the server" }
        val defaultRealmId = service.buildEapId("310260", "310260123456789")
        assertFalse(macVerifies(packet, defaultRealmId))
    }
}
