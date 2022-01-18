/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard

import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest
import com.google.android.gms.droidguard.internal.IDroidGuardHandle

class DroidGuardHandle(private val handle: IDroidGuardHandle) {
    private var state = 0
    var fd: ParcelFileDescriptor? = null

    fun init(flow: String) {
        if (state != 0) throw IllegalStateException("init() already called")
        try {
            val reply = handle.initWithRequest(flow, DroidGuardResultsRequest().setOpenHandles(openHandles++).also { fd?.let { fd -> it.fd = fd } })
            if (reply != null) {
                if (reply.pfd != null && reply.`object` != null) {
                    Log.w(TAG, "DroidGuardInitReply suggests additional actions in main thread")
                    val bundle = reply.`object` as? Bundle
                    if (bundle != null) {
                        for (key in bundle.keySet()) {
                            Log.d(TAG, "reply.object[$key] = ${bundle[key]}")
                        }
                    }
                }
            }
            state = 1
        } catch (e: Exception) {
            state = -1
            throw e
        }
    }

    fun guard(map: Map<String, String>): ByteArray {
        if (state != 1) throw IllegalStateException("init() must be called before guard()")
        try {
            return handle.guard(map)
        } catch (e: Exception) {
            state = -1
            throw e
        }
    }

    fun close() {
        if (state != 1) throw IllegalStateException("init() must be called before close()")
        try {
            handle.close()
            openHandles--
            state = 2
        } catch (e: Exception) {
            state = -1
            throw e
        }
    }

    fun finalize() {
        if (state == 1) close()
    }

    companion object {
        private const val TAG = "DroidGuardHandler"
        private var openHandles = 0
    }
}
