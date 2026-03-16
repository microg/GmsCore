/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.wearable.consent

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.R

/**
 * Terms of Service Activity for WearOS device pairing.
 * 
 * This activity displays the WearOS TOS and waits for user consent.
 * It is called during the WearOS device setup process by apps like Galaxy Wearable.
 * 
 * Usage:
 * - Start this activity with startActivityForResult
 * - RESULT_OK indicates user accepted TOS
 * - RESULT_CANCELED indicates user declined or dismissed
 */
class TermsOfServiceActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "wearos_consent"
        private const val KEY_CONSENT_GIVEN = "consent_given"
        private const val KEY_CONSENT_TIMESTAMP = "consent_timestamp"

        /**
         * Check if user has already given consent
         */
        fun hasConsent(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_CONSENT_GIVEN, false)
        }

        /**
         * Get the timestamp when consent was given
         */
        fun getConsentTimestamp(context: Context): Long {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getLong(KEY_CONSENT_TIMESTAMP, 0)
        }

        /**
         * Clear stored consent (for testing or reset)
         */
        fun clearConsent(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wear_tos)

        // Check if already consented - if so, return OK immediately
        if (hasConsent(this)) {
            setResult(RESULT_OK)
            finish()
            return
        }

        setupUI()
    }

    private fun setupUI() {
        val btnAccept: Button = findViewById(R.id.btn_accept)
        val btnDecline: Button = findViewById(R.id.btn_decline)
        val tosContent: TextView = findViewById(R.id.tos_content)

        // Accept button - store consent and return OK
        btnAccept.setOnClickListener {
            saveConsent()
            setResult(RESULT_OK)
            finish()
        }

        // Decline button - return CANCELED
        btnDecline.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        // Make content more readable on small screens (WearOS)
        tosContent.textSize = 12f
    }

    private fun saveConsent() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_CONSENT_GIVEN, true)
            .putLong(KEY_CONSENT_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    override fun onBackPressed() {
        // Back press is treated as decline
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }
}
