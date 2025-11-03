/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.wearable.consent

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import org.microg.gms.base.core.R

class TermsOfServiceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wearable_tos)

        val webView: WebView = findViewById(R.id.webview)
        webView.loadUrl("https://policies.google.com/terms")

        val pendingIntent = intent.getParcelableExtra<PendingIntent>("pendingIntent")

        val acceptButton: Button = findViewById(R.id.accept_button)
        acceptButton.setOnClickListener {
            try {
                val intent = Intent().putExtra("resultCode", RESULT_OK)
                pendingIntent?.send(this, 0, intent)
            } catch (e: PendingIntent.CanceledException) {
                e.printStackTrace()
            }
            finish()
        }

        val declineButton: Button = findViewById(R.id.decline_button)
        declineButton.setOnClickListener {
            try {
                val intent = Intent().putExtra("resultCode", RESULT_CANCELED)
                pendingIntent?.send(this, 0, intent)
            } catch (e: PendingIntent.CanceledException) {
                e.printStackTrace()
            }
            finish()
        }
    }
}
