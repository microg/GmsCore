/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.barcode

import com.google.zxing.LuminanceSource
import java.nio.ByteBuffer

class DirectLuminanceSource(width: Int, height: Int, val bytes: ByteBuffer) : LuminanceSource(width, height) {
    override fun getRow(y: Int, row: ByteArray?): ByteArray {
        val row = row?.takeIf { it.size >= width } ?: ByteArray(width)
        bytes.position(width * y)
        bytes.get(row, 0, width)
        return row
    }

    override fun getMatrix(): ByteArray {
        return bytes.array()
    }

}
