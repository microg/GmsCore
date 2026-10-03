/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class SourceTransferTransportTest {
    @Test fun sourceIsServerAndVerifiesPeerBeforeAcknowledging() {
        var verified = false
        val source = SourceTransferTransport({
            assertArrayEquals(bytes("verification_string"), it.toByteArray())
            verified = true
            true
        }, DirectTransferTestFixture.server())
        assertArrayEquals(byteArrayOf(8), source.begin(100))
        assertNull(source.receiveHandshake(byteArrayOf(8), 101))
        assertArrayEquals(bytes("server_init"), source.receiveHandshake(bytes("client_init"), 102))
        assertFalse(verified)
        assertArrayEquals(byteArrayOf(1, 1, 2, 3, 5, 8), source.receiveHandshake(bytes("client_finish"), 103))
        assertTrue(verified)
        assertEquals(SourceTransferTransport.State.ENCRYPTED, source.state)
        assertArrayEquals(bytes("client_plain_1"), source.decode(bytes("client_frame_1"), 104))
        source.close()
    }

    @Test fun unverifiedPeerCannotGetAcknowledgementOrTransferData() {
        val source = SourceTransferTransport({ false }, DirectTransferTestFixture.server())
        source.begin(0)
        source.receiveHandshake(byteArrayOf(8), 1)
        source.receiveHandshake(bytes("client_init"), 2)
        assertThrows(IOException::class.java) { source.receiveHandshake(bytes("client_finish"), 3) }
        assertEquals(SourceTransferTransport.State.CLOSED, source.state)
        assertThrows(IOException::class.java) { source.encode(byteArrayOf(1), 4) }
    }

    @Test fun refusesDowngradeAndDoesNotMistakeJsonForHandshake() {
        for (frame in listOf(byteArrayOf(1), byteArrayOf(9), byteArrayOf(8, 0), "{}".toByteArray())) {
            val source = SourceTransferTransport({ error("Verification must not run") })
            source.begin(0)
            assertThrows(IOException::class.java) { source.receiveHandshake(frame, 1) }
            assertEquals(SourceTransferTransport.State.CLOSED, source.state)
        }
    }

    @Test fun handshakeHasAbsoluteDeadlineRatherThanResetOnEachMessage() {
        val source = SourceTransferTransport({ true }, DirectTransferTestFixture.server(), handshakeTimeoutMillis = 10)
        source.begin(100)
        source.receiveHandshake(byteArrayOf(8), 109)
        assertThrows(IOException::class.java) { source.receiveHandshake(bytes("client_init"), 110) }
    }

    @Test fun inactivityAndClockRollbackCloseEncryptedSession() {
        for (expiredAt in listOf(102L, 1L)) {
            val source = SourceTransferTransport({ true }, DirectTransferTestFixture.server(), idleTimeoutMillis = 100)
            source.begin(0)
            source.receiveHandshake(byteArrayOf(8), 0)
            source.receiveHandshake(bytes("client_init"), 1)
            source.receiveHandshake(bytes("client_finish"), 2)
            assertThrows(IOException::class.java) { source.checkTimeout(expiredAt) }
            assertEquals(SourceTransferTransport.State.CLOSED, source.state)
        }
    }

    @Test fun cancelBeforeFinishCannotReopenSession() {
        val source = SourceTransferTransport({ true }, DirectTransferTestFixture.server())
        source.begin(0)
        source.receiveHandshake(byteArrayOf(8), 1)
        source.close()
        assertThrows(IOException::class.java) { source.receiveHandshake(bytes("client_init"), 2) }
        assertThrows(IOException::class.java) { source.begin(3) }
    }

    @Test fun peerVerifierCannotReopenSessionAfterCancellingIt() {
        lateinit var source: SourceTransferTransport
        source = SourceTransferTransport({ source.close(); true }, DirectTransferTestFixture.server())
        source.begin(0)
        source.receiveHandshake(byteArrayOf(8), 1)
        source.receiveHandshake(bytes("client_init"), 2)
        assertThrows(IOException::class.java) { source.receiveHandshake(bytes("client_finish"), 3) }
        assertEquals(SourceTransferTransport.State.CLOSED, source.state)
    }

    @Test fun backwardsClockDuringHandshakeFails() {
        val source = SourceTransferTransport({ true }, DirectTransferTestFixture.server())
        source.begin(0)
        source.receiveHandshake(byteArrayOf(8), 2)
        assertThrows(IOException::class.java) { source.receiveHandshake(bytes("client_init"), 1) }
    }

    private fun bytes(name: String) = DirectTransferTestFixture.bytes(name)
}
