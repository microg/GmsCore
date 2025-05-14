/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.microg.gms.common.Constants

class MarketDeepLinkHandlerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val targetIntent = Intent(intent).apply {
            setPackage(Constants.VENDING_PACKAGE_NAME)
            setClassName(this@MarketDeepLinkHandlerActivity, "org.microg.vending.MarketIntentRedirect")
        }
        startActivity(targetIntent)
        finish()
    }

}