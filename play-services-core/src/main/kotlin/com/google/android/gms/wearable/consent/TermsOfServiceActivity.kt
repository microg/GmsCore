/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.wearable.consent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class TermsOfServiceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // microG does not require the user to accept a Terms of Service to pair a WearOS device.
        setResult(RESULT_OK)
        finish()
    }
}