/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.wearable.consent

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TermsOfServiceActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "WearTOS"
        private const val PRIVACY_POLICY_URL = "https://policies.google.com/privacy"
        private const val TERMS_OF_SERVICE_URL = "https://policies.google.com/terms"
        
        // Result codes for the calling application
        const val RESULT_TOS_ACCEPTED = -1
        const val RESULT_TOS_DECLINED = 0
    }

    private lateinit var acceptButton: Button
    private lateinit var declineButton: Button
    private lateinit var tosCheckBox: CheckBox
    private lateinit var privacyCheckBox: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "WearOS Terms of Service activity started")
        
        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        // Create main layout
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Title
        val titleTextView = TextView(this).apply {
            text = "WearOS Terms of Service"
            textSize = 24f
            setPadding(0, 0, 0, 24)
        }

        // Scrollable content
        val scrollView = ScrollView(this)
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Terms content
        val termsContent = TextView(this).apply {
            text = "To use WearOS features, you must agree to the Google Terms of Service and Privacy Policy. This allows your wearable device to sync with your phone for notifications, health data, and app functionality."
            setPadding(0, 0, 0, 24)
        }

        // Checkboxes for agreements
        tosCheckBox = CheckBox(this).apply {
            text = "I agree to the Google Terms of Service"
            setOnCheckedChangeListener { _, _ -> updateAcceptButtonState() }
        }

        privacyCheckBox = CheckBox(this).apply {
            text = "I agree to the Google Privacy Policy"
            setOnCheckedChangeListener { _, _ -> updateAcceptButtonState() }
        }

        // Links layout
        val linksLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 24)
        }

        val termsLink = Button(this).apply {
            text = "View Terms"
            setOnClickListener { openUrl(TERMS_OF_SERVICE_URL) }
        }

        val privacyLink = Button(this).apply {
            text = "View Privacy Policy"
            setOnClickListener { openUrl(PRIVACY_POLICY_URL) }
        }

        // Button layout
        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        declineButton = Button(this).apply {
            text = "Decline"
            setOnClickListener { handleDecline() }
        }

        acceptButton = Button(this).apply {
            text = "Accept"
            isEnabled = false
            setOnClickListener { handleAccept() }
        }

        // Assemble the layout
        contentLayout.addView(termsContent)
        contentLayout.addView(tosCheckBox)
        contentLayout.addView(privacyCheckBox)
        
        linksLayout.addView(termsLink)
        linksLayout.addView(privacyLink)
        contentLayout.addView(linksLayout)

        scrollView.addView(contentLayout)

        buttonLayout.addView(declineButton)
        buttonLayout.addView(acceptButton)

        mainLayout.addView(titleTextView)
        mainLayout.addView(scrollView)
        mainLayout.addView(buttonLayout)

        setContentView(mainLayout)
    }

    private fun setupClickListeners() {
        // Additional setup if needed
    }

    private fun updateAcceptButtonState() {
        acceptButton.isEnabled = tosCheckBox.isChecked && privacyCheckBox.isChecked
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot open URL: $url", e)
            // Show fallback dialog with terms text
            showTermsDialog(url)
        }
    }

    private fun showTermsDialog(url: String) {
        val message = if (url.contains("privacy")) {
            "Google Privacy Policy\n\nGoogle's Privacy Policy explains how Google collects, uses, and protects your information when you use WearOS services. This includes health data, location information, and usage patterns from your wearable device."
        } else {
            "Google Terms of Service\n\nBy using WearOS, you agree to Google's Terms of Service. These terms govern your use of Google products and services, including WearOS functionality on your wearable device."
        }

        AlertDialog.Builder(this)
            .setTitle(if (url.contains("privacy")) "Privacy Policy" else "Terms of Service")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun handleAccept() {
        Log.d(TAG, "User accepted WearOS Terms of Service")
        
        // Store the acceptance (in a real implementation, you might want to persist this)
        val prefs = getSharedPreferences("wearos_consent", MODE_PRIVATE)
        prefs.edit()
            .putBoolean("tos_accepted", true)
            .putBoolean("privacy_accepted", true)
            .putLong("acceptance_time", System.currentTimeMillis())
            .apply()

        setResult(RESULT_TOS_ACCEPTED)
        finish()
    }

    private fun handleDecline() {
        Log.d(TAG, "User declined WearOS Terms of Service")
        
        AlertDialog.Builder(this)
            .setTitle("Decline Terms?")
            .setMessage("If you decline, your wearable device will not be able to pair with your phone or access WearOS features.")
            .setPositiveButton("Decline anyway") { _, _ ->
                setResult(RESULT_TOS_DECLINED)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onBackPressed() {
        // Treat back button as decline
        handleDecline()
    }
}