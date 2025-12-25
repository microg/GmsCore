/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.wearable.consent

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.R
import com.google.android.material.button.MaterialButton


class TermsOfServiceActivity : AppCompatActivity() {

    private var acceptButton: MaterialButton? = null
    private var declineButton: MaterialButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setResult(RESULT_CANCELED)
//        finish()

        // TODO: make consent list
        setContentView(R.layout.activity_wearable_tos);

        acceptButton = findViewById(R.id.terms_of_service_accept_button);
        declineButton = findViewById(R.id.terms_of_service_decline_button);

        acceptButton?.setOnClickListener { acceptConsents() }
        declineButton?.setOnClickListener { declineConsents() }

    }

    private fun acceptConsents() {
        val result = Intent().apply {
            putExtra("consents_accepted", true)
            putExtra("tos_accepted", true)
            putExtra("privacy_policy_accepted", true)
        }

        setResult(RESULT_OK, result)
        finish()
    }

    private fun declineConsents() {
        val result = Intent().apply {
            putExtra("consents_accepted", false)
        }

        setResult(RESULT_CANCELED, result)
        finish()
    }
}