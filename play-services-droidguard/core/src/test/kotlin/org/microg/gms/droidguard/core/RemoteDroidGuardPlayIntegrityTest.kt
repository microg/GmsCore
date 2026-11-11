/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteDroidGuardPlayIntegrityTest {
    @Test
    fun testUrlEncodedPairsRoundTrip() {
        val raw = "flow=pia_express&source=com.ridedott.rider&x-request-thirdPartyCallerAppPackageName=com.ridedott.rider"
        val map = RemoteDroidGuardServer.parseUrlEncodedPairs(raw)
        assertEquals("pia_express", map["flow"])
        assertEquals("com.ridedott.rider", map["source"])
        assertEquals("com.ridedott.rider", map["x-request-thirdPartyCallerAppPackageName"])
    }

    @Test
    fun testSnapshotMapEncodingPreservesKeys() {
        val data = linkedMapOf<Any?, Any?>(
            "pkg_key" to "com.ridedott.rider",
            "vc_key" to "420",
            "droid_guard_session_token_v1" to "sess_abc123",
        )
        val encoded = RemoteHandleImpl.encodeSnapshotMap(data)
        assertTrue(encoded.contains("pkg_key=com.ridedott.rider"))
        assertTrue(encoded.contains("droid_guard_session_token_v1=sess_abc123"))
    }
}
