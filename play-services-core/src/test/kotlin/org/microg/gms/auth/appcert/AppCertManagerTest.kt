package org.microg.gms.auth.appcert

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppCertManagerTest {
    @Test
    fun androidIdFallbackProducesSpatulaProto() {
        val proto = buildFallbackSpatulaHeaderProto(
            packageName = "com.google.android.gms",
            packageCertificateHash = "certificate-hash",
            androidId = 0x1122334455667788L
        )

        assertEquals("com.google.android.gms", proto.packageInfo?.packageName)
        assertEquals("certificate-hash", proto.packageInfo?.packageCertificateHash)
        assertEquals(0x1122334455667788L, proto.deviceId)
        assertNull(proto.hmac)
        assertNull(proto.keyId)
    }
}
