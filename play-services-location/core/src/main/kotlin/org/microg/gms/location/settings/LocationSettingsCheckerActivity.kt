/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import org.microg.gms.location.core.R

const val ACTION_LOCATION_SETTINGS_CHECKER = "com.google.android.gms.location.settings.CHECK_SETTINGS"

private const val REQUEST_CODE_LOCATION = 120

class LocationSettingsCheckerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_setting_checker)

        findViewById<TextView>(R.id.location_setting_checker_sure).setOnClickListener {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(intent, REQUEST_CODE_LOCATION)
        }
        findViewById<TextView>(R.id.location_setting_checker_cancel).setOnClickListener {
            checkerBack(RESULT_CANCELED)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_LOCATION && isLocationEnabled(this)) {
            checkerBack(RESULT_OK)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onBackPressed() {
        checkerBack(RESULT_CANCELED)
    }

    private fun checkerBack(resultCode: Int) {
        setResult(resultCode)
        finish()
    }

    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

}