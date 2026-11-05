/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import okio.ByteString.Companion.toByteString
import org.junit.Assert.*
import org.junit.Test
import org.microg.gms.smartdevice.proto.Ukey2Alert
import org.microg.gms.smartdevice.proto.Ukey2CipherCommitment
import org.microg.gms.smartdevice.proto.Ukey2ClientFinished
import org.microg.gms.smartdevice.proto.Ukey2Message
import org.microg.gms.smartdevice.proto.Ukey2MessageType
import java.io.IOException
import java.security.MessageDigest

class Ukey2ServerHandshakeTest {
    @Test fun agreesWithIndependentGoogleHandshakeAndIncomingData() {
        val server = DirectTransferTestFixture.server()
        assertArrayEquals(bytes("server_init"), server.acceptClientInit(bytes("client_init")))
        val established = server.acceptClientFinish(bytes("client_finish"))
        assertArrayEquals(bytes("verification_string"), established.verificationString.toByteArray())
        established.context.use {
            assertArrayEquals(bytes("client_plain_1"), it.decode(bytes("client_frame_1")))
            assertArrayEquals(bytes("client_plain_2"), it.decode(bytes("client_frame_2")))
        }
        assertThrows(IOException::class.java) { server.acceptClientInit(bytes("client_init")) }
    }

    @Test fun rejectsUnsupportedNegotiationWithCorrectAlertType() {
        val mutations = listOf(
            100 to DirectTransferTestFixture.clientInit { it.copy(version = 2) },
            101 to DirectTransferTestFixture.clientInit { it.copy(random = byteArrayOf(1).toByteString()) },
            103 to DirectTransferTestFixture.clientInit { it.copy(next_protocol = "plaintext") },
            102 to DirectTransferTestFixture.clientInit { it.copy(cipher_commitments = emptyList()) },
            102 to DirectTransferTestFixture.clientInit { it.copy(cipher_commitments = it.cipher_commitments + it.cipher_commitments) }
        )
        for ((code, bytes) in mutations) {
            val error = assertThrows(Ukey2ProtocolException::class.java) { DirectTransferTestFixture.server().acceptClientInit(bytes) }
            val alert = Ukey2Message.ADAPTER.decode(requireNotNull(error.alert))
            assertEquals(Ukey2MessageType.ALERT, alert.message_type)
            assertEquals(code, Ukey2Alert.ADAPTER.decode(requireNotNull(alert.message_data)).type)
        }
    }

    @Test fun mismatchedCommitmentPermanentlyRejectsSession() {
        val server = DirectTransferTestFixture.server()
        server.acceptClientInit(bytes("client_init"))
        val changed = bytes("client_finish").also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        val error = assertThrows(Ukey2ProtocolException::class.java) { server.acceptClientFinish(changed) }
        assertNull(error.alert) // The UKEY2 client does not expect another handshake message here.
        assertThrows(IOException::class.java) { server.acceptClientFinish(bytes("client_finish")) }
    }

    @Test fun invalidCurveKeyCannotPassByCommittingToIt() {
        val finished = Ukey2Message(Ukey2MessageType.CLIENT_FINISH,
            Ukey2ClientFinished(byteArrayOf(8, 1).toByteString()).encodeByteString()).encode()
        val commitment = MessageDigest.getInstance("SHA-512").digest(finished).toByteString()
        val init = DirectTransferTestFixture.clientInit {
            it.copy(cipher_commitments = listOf(Ukey2CipherCommitment(100, commitment)))
        }
        val server = DirectTransferTestFixture.server()
        server.acceptClientInit(init)
        assertThrows(IOException::class.java) { server.acceptClientFinish(finished) }
    }

    @Test fun wrongOrderMalformedAndOversizeMessagesFailClosed() {
        assertThrows(IOException::class.java) { DirectTransferTestFixture.server().acceptClientFinish(bytes("client_finish")) }
        assertThrows(IOException::class.java) { DirectTransferTestFixture.server().acceptClientInit(bytes("server_init")) }
        assertThrows(IOException::class.java) { DirectTransferTestFixture.server().acceptClientInit(byteArrayOf(-1)) }
        assertThrows(IOException::class.java) { DirectTransferTestFixture.server().acceptClientInit(ByteArray(4097)) }
    }

    private fun bytes(name: String) = DirectTransferTestFixture.bytes(name)
}
