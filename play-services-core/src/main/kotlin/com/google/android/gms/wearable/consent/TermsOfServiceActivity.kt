/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.wearable.consent

import android.app.AlertDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.R
import org.microg.gms.wearable.WearablePreferences

class TermsOfServiceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (WearablePreferences.isAutoAcceptTosEnabled(this)) {
            // User has opted-in to auto-accept; proceed immediately.
            setResult(RESULT_OK)
            finish()
            return
        }

        // Show an explicit dialog so the user can make an informed choice.
        AlertDialog.Builder(this)
            .setTitle(R.string.wearable_tos_dialog_title)
            .setMessage(R.string.wearable_tos_dialog_message)
            .setPositiveButton(R.string.wearable_tos_accept) { _, _ ->
                setResult(RESULT_OK)
                finish()
            }
            .setNegativeButton(R.string.wearable_tos_decline) { _, _ ->
                setResult(RESULT_CANCELED)
                finish()
            }
            .setOnCancelListener {
                setResult(RESULT_CANCELED)
                finish()
            }
            .show()
    }
}