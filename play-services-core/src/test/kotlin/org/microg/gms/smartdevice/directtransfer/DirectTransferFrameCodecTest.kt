/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException

class DirectTransferFrameCodecTest {
    @Test fun writesKnownBigEndianFramesWithoutClosingStream() {
        val stream = ByteArrayOutputStream()
        DirectTransferFrameCodec.write(stream, byteArrayOf(8))
        DirectTransferFrameCodec.write(stream, byteArrayOf(1, 1, 2, 3, 5, 8))
        assertArrayEquals(byteArrayOf(0, 0, 0, 1, 8, 0, 0, 0, 6, 1, 1, 2, 3, 5, 8), stream.toByteArray())
        val input = ByteArrayInputStream(stream.toByteArray())
        assertArrayEquals(byteArrayOf(8), DirectTransferFrameCodec.read(input))
        assertArrayEquals(byteArrayOf(1, 1, 2, 3, 5, 8), DirectTransferFrameCodec.read(input))
    }

    @Test fun readsFragmentedPipe() {
        val input = object : ByteArrayInputStream(byteArrayOf(0, 0, 0, 3, 7, 8, 9)) {
            override fun read(bytes: ByteArray, offset: Int, length: Int) = super.read(bytes, offset, minOf(1, length))
        }
        assertArrayEquals(byteArrayOf(7, 8, 9), DirectTransferFrameCodec.read(input))
    }

    @Test fun rejectsInvalidLengthsBeforeReadingBody() {
        for (bytes in listOf(byteArrayOf(-1, -1, -1, -1), byteArrayOf(0, 0, 0, 0), byteArrayOf(0, 16, 0, 1))) {
            assertThrows(IOException::class.java) { DirectTransferFrameCodec.read(ByteArrayInputStream(bytes)) }
        }
        assertThrows(IOException::class.java) {
            DirectTransferFrameCodec.read(ByteArrayInputStream(byteArrayOf(0, 0, 0, 2)), 1)
        }
    }

    @Test fun truncatedHeaderOrBodyFails() {
        for (bytes in listOf(byteArrayOf(0, 0, 0), byteArrayOf(0, 0, 0, 2, 1))) {
            assertThrows(EOFException::class.java) { DirectTransferFrameCodec.read(ByteArrayInputStream(bytes)) }
        }
    }

    @Test fun invalidWriteDoesNotEmitPartialFrame() {
        val output = ByteArrayOutputStream()
        assertThrows(IOException::class.java) { DirectTransferFrameCodec.write(output, ByteArray(0)) }
        assertThrows(IOException::class.java) { DirectTransferFrameCodec.write(output, byteArrayOf(1, 2), 1) }
        assertEquals(0, output.size())
    }
}
