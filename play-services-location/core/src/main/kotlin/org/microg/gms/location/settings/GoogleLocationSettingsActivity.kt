/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.settings

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class GoogleLocationSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching { startActivity(Intent("android.settings.LOCATION_SOURCE_SETTINGS")) }
        finish()
    }
}