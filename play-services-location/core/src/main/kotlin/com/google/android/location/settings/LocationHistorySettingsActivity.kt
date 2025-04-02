/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.location.settings

import android.accounts.Account
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class LocationHistorySettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent != null) {
            val account = intent.extras?.getParcelable<Account>("account")
            val settingIntent = Intent("com.google.android.gms.accountsettings.ACCOUNT_PREFERENCES_SETTINGS")
            settingIntent.putExtra("extra.accountName", account?.name)
            settingIntent.putExtra("extra.screenId", 227)
            startActivity(settingIntent)
        }
        finish()
    }
}