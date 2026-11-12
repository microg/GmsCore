package org.microg.gms.asterism.core.samsung

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SamsungCompositeTokenCodecTest {
    @Test fun exactKnownFixtureMatchesWireContract() {
        val raw = SamsungCompositeTokenCodec.encodeProto("iid-token", "pia-token")
        assertEquals("CglpaWQtdG9rZW4SCXBpYS10b2tlbg==", Base64.getUrlEncoder().encodeToString(raw))
    }
    @Test fun missingProducerInputReturnsNoCompositeToken() {
        assertNull(SamsungCompositeTokenCodec.encodeForImsOrNull(null, "pia"))
        assertNull(SamsungCompositeTokenCodec.encodeForImsOrNull("iid", null))
        assertNull(SamsungCompositeTokenCodec.encodeForImsOrNull("", "pia"))
        assertNull(SamsungCompositeTokenCodec.encodeForImsOrNull("iid", ""))
    }
    @Test fun longTokenLengthUsesProtobufVarint() {
        val raw = SamsungCompositeTokenCodec.encodeProto("i".repeat(130), "p")
        assertEquals(0x0a, raw[0].toInt() and 0xff)
        assertEquals(0x82, raw[1].toInt() and 0xff)
        assertEquals(0x01, raw[2].toInt() and 0xff)
        assertTrue(raw.size > 135)
    }
}
