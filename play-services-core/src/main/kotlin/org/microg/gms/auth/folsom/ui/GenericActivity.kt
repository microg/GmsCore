/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.folsom.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.R

class GenericActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ACCOUNT_NAME = "account_name"
        const val EXTRA_SECURITY_DOMAIN = "security_domain"
        const val EXTRA_OPERATION = "operation"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_OFFER_RESET = "offer_reset"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val extras = intent.extras
        val accountName = extras?.getString(EXTRA_ACCOUNT_NAME)
        val domain = extras?.getString(EXTRA_SECURITY_DOMAIN)
        if (accountName.isNullOrEmpty() || domain.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.backup_disabled), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        if (savedInstanceState == null) {
            FolsomWebFragment.newInstance(
                accountName = accountName,
                securityDomain = domain,
                operation = extras.getInt(EXTRA_OPERATION, 0),
                sessionId = extras.getString(EXTRA_SESSION_ID, ""),
                offerReset = extras.getBoolean(EXTRA_OFFER_RESET, false)
            ).also { fragment ->
                supportFragmentManager.beginTransaction()
                    .add(android.R.id.content, fragment)
                    .commit()
            }
        }
    }
}