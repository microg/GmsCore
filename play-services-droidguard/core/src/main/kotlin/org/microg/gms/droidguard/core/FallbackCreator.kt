/*
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import android.content.Context
import android.util.Log

object FallbackCreator {
    private val FAST_FAIL = setOf("ad_attest", "recaptcha-frame", "federatedMachineLearningReduced", "msa-f", "ad-event-attest-token")

    @JvmStatic
    fun create(flow: String?, context: Context, map: Map<Any?, Any?>, e: Throwable): ByteArray {
        Log.w("DGFallback", "create($flow)")
        return if (flow in FAST_FAIL) {
            "ERROR : no fallback for $flow".encodeToByteArray()
        } else {
            try {
                create(map, null, flow, context, e)
            } catch (e: Throwable) {
                Log.w("DGFallback", e)
                "ERROR : $e".encodeToByteArray()
            }
        }
    }

    @JvmStatic
    fun create(map: Map<Any?, Any?>, bytes: ByteArray?, flow: String?, context: Context, e: Throwable): ByteArray {
        TODO("Not yet implemented")
    }
}
