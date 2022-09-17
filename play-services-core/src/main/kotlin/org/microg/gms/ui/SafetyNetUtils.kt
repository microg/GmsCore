/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.R
import com.google.android.gms.common.api.Status
import org.json.JSONObject
import org.microg.gms.safetynet.SafetyNetRequestType

fun formatSummaryForSafetyNetResult(context: Context, result: String?, status: Status?, type: SafetyNetRequestType): Pair<String, Drawable?> {
    when (type) {
        SafetyNetRequestType.ATTESTATION -> {
            if (status?.isSuccess != true) return context.getString(R.string.pref_test_summary_failed, status?.statusMessage) to ContextCompat.getDrawable(context, R.drawable.ic_circle_error)
            if (result == null) return context.getString(R.string.pref_test_summary_failed, "No result") to ContextCompat.getDrawable(context, R.drawable.ic_circle_warn)
            val (basicIntegrity, ctsProfileMatch, advice) = try {
                JSONObject(result).let {
                    Triple(
                        it.optBoolean("basicIntegrity", false),
                        it.optBoolean("ctsProfileMatch", false),
                        it.optString("advice", "")
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, e)
                return context.getString(
                    R.string.pref_test_summary_failed,
                    "Invalid JSON"
                ) to ContextCompat.getDrawable(context, R.drawable.ic_circle_error)
            }
            val adviceText = if (advice == "") "" else "\n" + advice.split(",").map {
                when (it) {
                    "LOCK_BOOTLOADER" -> "Bootloader is not locked"
                    "RESTORE_TO_FACTORY_ROM" -> "ROM is not clean"
                    else -> it
                }
            }.joinToString("\n")
            return when {
                basicIntegrity && ctsProfileMatch -> {
                    context.getString(R.string.pref_test_summary_passed) to ContextCompat.getDrawable(context, R.drawable.ic_circle_check)
                }
                basicIntegrity -> {
                    context.getString(
                        R.string.pref_test_summary_warn,
                        "CTS profile does not match$adviceText"
                    ) to ContextCompat.getDrawable(context, R.drawable.ic_circle_warn)
                }
                else -> {
                    context.getString(
                        R.string.pref_test_summary_failed,
                        "integrity check failed$adviceText"
                    ) to ContextCompat.getDrawable(context, R.drawable.ic_circle_error)
                }
            }
        }
        SafetyNetRequestType.RECAPTCHA -> {
            if (result != null || status?.isSuccess == true) {
                return context.getString(R.string.pref_test_summary_passed) to ContextCompat.getDrawable(context, R.drawable.ic_circle_check)
            } else {
                return context.getString(R.string.pref_test_summary_failed, status?.statusMessage) to ContextCompat.getDrawable(context, R.drawable.ic_circle_error)
            }
        }
    }
}
