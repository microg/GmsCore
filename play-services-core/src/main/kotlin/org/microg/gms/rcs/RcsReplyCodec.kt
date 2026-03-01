/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs

import android.os.IBinder
import android.os.Parcel

internal object RcsReplyCodec {
    private const val STATUS_UNAVAILABLE = 0

    fun writeConfigUnavailable(reply: Parcel?, flags: Int) {
        if (reply == null || flags and IBinder.FLAG_ONEWAY != 0) return
        reply.writeNoException()
        reply.writeInt(STATUS_UNAVAILABLE)
        reply.writeString("")
    }

    fun writeGenericUnavailable(reply: Parcel?, flags: Int) {
        if (reply == null || flags and IBinder.FLAG_ONEWAY != 0) return
        reply.writeNoException()
        reply.writeInt(STATUS_UNAVAILABLE)
    }
}

