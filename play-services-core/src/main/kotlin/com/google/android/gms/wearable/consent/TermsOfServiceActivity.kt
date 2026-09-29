/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.wearable.consent

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TermsOfServiceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.wearable_tos_title)
            .setMessage(R.string.wearable_tos_message)
            .setCancelable(true)
            .setPositiveButton(R.string.allow) { _, _ -> finishWithResult(true) }
            .setNegativeButton(R.string.deny) { _, _ -> finishWithResult(false) }
            .setOnCancelListener { finishWithResult(false) }
            .show()
    }

    private fun finishWithResult(accepted: Boolean) {
        val result = Intent().apply {
            putExtra(EXTRA_CONSENTS_ACCEPTED, accepted)
            putExtra(EXTRA_TOS_ACCEPTED, accepted)
            putExtra(EXTRA_PRIVACY_POLICY_ACCEPTED, accepted)
        }
        setResult(if (accepted) RESULT_OK else RESULT_CANCELED, result)
        finish()
    }

    companion object {
        // Extras read by Wear OS / Galaxy Wearable companion setup.
        const val EXTRA_CONSENTS_ACCEPTED = "consents_accepted"
        const val EXTRA_TOS_ACCEPTED = "tos_accepted"
        const val EXTRA_PRIVACY_POLICY_ACCEPTED = "privacy_policy_accepted"
    }
}