/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** The direct-transfer pipe is a sequence of big-endian int32 lengths and byte arrays. */
internal object DirectTransferFrameCodec {
    // Local resource limit, not a claim that peers support arbitrarily large transfers.
    const val MAX_FRAME_BYTES = 1024 * 1024

    @Throws(IOException::class)
    fun read(input: InputStream, maximum: Int = MAX_FRAME_BYTES): ByteArray {
        require(maximum in 1..MAX_FRAME_BYTES)
        val data = DataInputStream(input)
        val length = data.readInt()
        if (length !in 1..maximum) throw IOException("Invalid direct-transfer frame length")
        return ByteArray(length).also { data.readFully(it) }
    }

    @Throws(IOException::class)
    fun write(output: OutputStream, bytes: ByteArray, maximum: Int = MAX_FRAME_BYTES) {
        require(maximum in 1..MAX_FRAME_BYTES)
        if (bytes.size !in 1..maximum) throw IOException("Invalid direct-transfer frame length")
        DataOutputStream(output).apply {
            writeInt(bytes.size)
            write(bytes)
            flush()
        }
    }
}
