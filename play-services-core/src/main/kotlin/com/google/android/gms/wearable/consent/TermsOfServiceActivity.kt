/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.wearable.consent

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.wearable.R

class TermsOfServiceActivity : AppCompatActivity() {

    companion object {
        const val WEARABLE_TOS_ACCEPTED = "wearable_tos_accepted"
        const val WEARABLE_TOS_PREFS = "wearable_tos_prefs"
    }

    private var resultCode: Int = RESULT_CANCELED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wearable_tos)

        findViewById<android.widget.Button>(R.id.button_accept).setOnClickListener {
            resultCode = RESULT_OK
            saveTosAccepted(true)
            finish()
        }

        findViewById<android.widget.Button>(R.id.button_decline).setOnClickListener {
            resultCode = RESULT_CANCELED
            finish()
        }
    }

    override fun finish() {
        setResult(resultCode)
        super.finish()
    }

    private fun saveTosAccepted(accepted: Boolean) {
        getSharedPreferences(WEARABLE_TOS_PREFS, MODE_PRIVATE)
            .edit()
            .putBoolean(WEARABLE_TOS_ACCEPTED, accepted)
            .apply()
    }

    fun isTosAccepted(): Boolean {
        return getSharedPreferences(WEARABLE_TOS_PREFS, MODE_PRIVATE)
            .getBoolean(WEARABLE_TOS_ACCEPTED, false)
    }
}