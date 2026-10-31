/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.wearable.consent

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.R

/**
 * Shown by Wear OS / Galaxy Wearable companion apps during device setup.
 *
 * Prior to this implementation the activity immediately returned
 * [RESULT_CANCELED], which blocked pairing (#2444 / #2843).
 */
class TermsOfServiceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            .setCancelable(true)
            .show()
    }
}
