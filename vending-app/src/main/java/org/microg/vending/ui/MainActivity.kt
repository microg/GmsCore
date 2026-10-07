/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.android.vending.R

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                setPackage(GMS_PACKAGE_NAME)
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Log.w(TAG, "MAIN activity is not DEFAULT. Trying to resolve instead.")
                val resolveInfo = packageManager.resolveActivity(intent, 0)
                if (resolveInfo?.activityInfo != null) {
                    intent.setClassName(GMS_PACKAGE_NAME, resolveInfo.activityInfo.name)
                    startActivity(intent)
                } else {
                    throw e
                }
            }
            Toast.makeText(this, R.string.toast_installed, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.w(TAG, "Failed launching microG Settings", e)
            Toast.makeText(this, R.string.toast_not_installed, Toast.LENGTH_LONG).show()
        }
        finish()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val GMS_PACKAGE_NAME = "com.google.android.gms"
    }
}
