/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast

private const val TAG = "ShareSheetActivity"

class ShareSheetActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: action=${intent?.action}")

        val action = intent?.action
        if (Intent.ACTION_SEND == action || Intent.ACTION_SEND_MULTIPLE == action || "com.google.android.gms.SHARE_NEARBY" == action) {
            Toast.makeText(this, "Quick Share: Looking for nearby devices...", Toast.LENGTH_SHORT).show()
        }

        setResult(RESULT_OK)
        finish()
    }
}
