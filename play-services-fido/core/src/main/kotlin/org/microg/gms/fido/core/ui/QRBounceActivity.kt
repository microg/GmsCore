/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlin.also
import kotlin.text.lowercase

class QRBounceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onNewIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        var scheme: String? = null
        val action = intent!!.action
        val data = intent.data
        if (action == null || (action != "android.intent.action.VIEW") || data == null || (data.scheme.also { scheme = it }) == null || (scheme!!.lowercase() != "fido")) {
            Log.w(TAG, "Invalid data from scanning QR Code: $data")
            finish()
            return
        }
        val targetIntent = Intent()
        targetIntent.setClassName(this, "org.microg.gms.fido.core.ui.hybrid.HybridAuthenticateActivity")
        targetIntent.setData(data)
        startActivity(targetIntent)
        finish()
    }
}