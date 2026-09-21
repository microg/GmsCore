/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.appcert

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Documents the Android-ID Spatula fallback wire shape used by AppCertManager
 * when DeviceKey fetch fails (see #2994 RCS investigation).
 *
 * The fallback intentionally omits hmac / keyId / keyCert and only carries
 * packageInfo + deviceId.
 */
class SpatulaHeaderFallbackTest {

    @Test
    fun fallbackShape_roundTripsWithoutHmacFields() {
        val original = SpatulaHeaderProto(
            packageInfo = SpatulaHeaderProto.PackageInfo(
                packageName = "com.google.android.apps.messaging",
                packageCertificateHash = "cert-hash",
            ),
            deviceId = 0x1122334455667788L,
        )

        val decoded = SpatulaHeaderProto.ADAPTER.decode(original.encode())

        assertEquals("com.google.android.apps.messaging", decoded.packageInfo?.packageName)
        assertEquals("cert-hash", decoded.packageInfo?.packageCertificateHash)
        assertEquals(0x1122334455667788L, decoded.deviceId)
        assertNull(decoded.hmac)
        assertNull(decoded.keyId)
        assertNull(decoded.keyCert)
    }

    @Test
    fun fallbackShape_encodesNonEmptyPayload() {
        val encoded = SpatulaHeaderProto(
            packageInfo = SpatulaHeaderProto.PackageInfo(
                packageName = "com.google.android.apps.messaging",
                packageCertificateHash = "cert-hash",
            ),
            deviceId = 1L,
        ).encode()

        assertTrue(encoded.isNotEmpty())
    }
}
