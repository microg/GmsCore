/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import org.junit.Assert.*
import org.junit.Test
import org.microg.gms.cryptauth.proto.Header
import org.microg.gms.cryptauth.proto.HeaderAndBodyInternal
import org.microg.gms.cryptauth.proto.SecureMessage
import java.io.IOException

class D2DConnectionContextV1Test {
    @Test fun producesExactlyGooglesIndependentEncryptedFrames() {
        val random = DirectTransferTestFixture.FixedRandom(iv(bytes("server_frame_1")), iv(bytes("server_frame_2")))
        DirectTransferTestFixture.context(true, random).use {
            assertArrayEquals(bytes("server_frame_1"), it.encode(bytes("server_plain_1")))
            assertArrayEquals(bytes("server_frame_2"), it.encode(bytes("server_plain_2")))
            assertArrayEquals(bytes("client_plain_1"), it.decode(bytes("client_frame_1")))
            assertArrayEquals(bytes("client_plain_2"), it.decode(bytes("client_frame_2")))
        }
    }

    @Test fun ciphertextAndMacTamperingCloseContext() {
        val frame = bytes("client_frame_1")
        for (offset in listOf(25, frame.lastIndex)) {
            val changed = frame.clone().also { it[offset] = (it[offset].toInt() xor 1).toByte() }
            val context = DirectTransferTestFixture.context(true)
            assertThrows(IOException::class.java) { context.decode(changed) }
            assertThrows(IOException::class.java) { context.decode(frame) }
        }
    }

    @Test fun replayIsRejectedAndContextCannotBeReused() {
        val context = DirectTransferTestFixture.context(true)
        context.decode(bytes("client_frame_1"))
        assertThrows(IOException::class.java) { context.decode(bytes("client_frame_1")) }
        assertThrows(IOException::class.java) { context.decode(bytes("client_frame_2")) }
    }

    @Test fun validButOutOfOrderFrameIsRejected() {
        val context = DirectTransferTestFixture.context(true)
        assertThrows(IOException::class.java) { context.decode(bytes("client_frame_2")) }
    }

    @Test fun directionalKeysPreventReflection() {
        val context = DirectTransferTestFixture.context(true)
        assertThrows(IOException::class.java) { context.decode(bytes("server_frame_1")) }
    }

    @Test fun oversizedPayloadAndClosedContextFail() {
        val context = DirectTransferTestFixture.context(true)
        assertThrows(IOException::class.java) { context.encode(ByteArray(D2DConnectionContextV1.MAX_PAYLOAD_BYTES + 1)) }
        assertThrows(IOException::class.java) { context.encode(byteArrayOf(1)) }
    }

    private fun iv(frame: ByteArray): ByteArray {
        val body = HeaderAndBodyInternal.ADAPTER.decode(SecureMessage.ADAPTER.decode(frame).header_and_body)
        return requireNotNull(Header.ADAPTER.decode(body.header_).iv).toByteArray()
    }

    private fun bytes(name: String) = DirectTransferTestFixture.bytes(name)
}
