/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.accountsettings.ui

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.content.Intent.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.microg.gms.auth.AuthConstants.DEFAULT_ACCOUNT_TYPE
import org.microg.gms.common.PackageUtils
import org.microg.tools.AccountPickerActivity

private const val TAG = "AccountSettingsLoader"

private val ALLOWED_FALLBACK_PREFIXES = setOf("https://myaccount.google.com/", "https://takeout.google.com/")
private val BROWSABLE_SCREEN_IDS = setOf(1, 200, 400, 502, 527, 10003, 10050, 12700, 12701)
private val ACCOUNT_CHOOSER_URI = Uri.parse("https://accounts.google.com/AccountChooser")

private const val QUERY_PARAM_CONTINUE = "continue"
private const val QUERY_PARAM_LANG = "hl"
private const val QUERY_PARAM_EMAIL = "Email"

private const val EXTRA_ALLOWABLE_ACCOUNT_TYPES = "allowableAccountTypes"

private const val REQUEST_ACCOUNT_PICKER = 1

class LoaderActivity : AppCompatActivity() {
    private var canAskForAccount = false

    private fun launchFallback() {
        val fallbackUrl = intent?.getStringExtra(EXTRA_FALLBACK_URL)

        if (fallbackUrl == null) {
            Log.d(TAG, "No fallback")
            finishResult(RESULT_CANCELED)
        } else if (fallbackUrl in ALLOWED_FALLBACK_PREFIXES) {
            // TODO: Error screen?
            Log.d(TAG, "Illegal fallback url")
            finishResult(RESULT_CANCELED)
        } else {
            val fallbackAuth = intent?.getBooleanExtra(EXTRA_FALLBACK_AUTH, false) ?: false
            val uri = if (fallbackAuth) {
                val builder = ACCOUNT_CHOOSER_URI.buildUpon().appendQueryParameter(QUERY_PARAM_CONTINUE, fallbackUrl)
                val accountName = intent?.getStringExtra(EXTRA_ACCOUNT_NAME)
                if (!accountName.isNullOrBlank()) {
                    builder.appendQueryParameter(QUERY_PARAM_EMAIL, accountName)
                }
                val lang = Uri.parse(fallbackUrl).getQueryParameter(QUERY_PARAM_LANG)
                if (lang != null) {
                    builder.appendQueryParameter(QUERY_PARAM_LANG, lang)
                }
                builder.build()
            } else {
                Uri.parse(fallbackUrl)
            }
            Log.d(TAG, "Opening fallback $fallbackUrl")
            // noinspection UnsafeImplicitIntentLaunch
            val intent = Intent(ACTION_VIEW, uri).apply { addCategory(CATEGORY_BROWSABLE) }
            startActivity(intent)
            finishResult(RESULT_OK)
        }
    }

    private fun launchMain() {
        val requestedAccountName = intent.getStringExtra(EXTRA_ACCOUNT_NAME)
        val ignoreAccount = intent?.getBooleanExtra(EXTRA_IGNORE_ACCOUNT, false) ?: false
        val accounts = AccountManager.get(this).getAccountsByType(DEFAULT_ACCOUNT_TYPE)
        val account = if (requestedAccountName != null) {
            val account = accounts.find { it.name == requestedAccountName }
            if (account == null) {
                // TODO: Error screen?
                Log.d(TAG, "Account not found: $requestedAccountName")
                return finishResult(RESULT_CANCELED)
            }
            account
        } else if (accounts.isEmpty()) {
            if (intent?.getStringExtra(EXTRA_FALLBACK_URL) != null) {
                return launchFallback()
            } else {
                // TODO: Error screen?
                Log.d(TAG, "No account configured")
                return finishResult(RESULT_CANCELED)
            }
        } else if (accounts.size > 1) {
            if (canAskForAccount) {
                val intent = Intent(this, AccountPickerActivity::class.java)
                intent.putExtra(EXTRA_ALLOWABLE_ACCOUNT_TYPES, arrayOf(DEFAULT_ACCOUNT_TYPE))
                startActivityForResult(intent, REQUEST_ACCOUNT_PICKER)
                canAskForAccount = false
                return
            } else {
                return finishResult(RESULT_CANCELED)
            }
        } else {
            accounts.first()
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            action = intent.action
            if (intent.hasExtra(EXTRA_THEME_CHOICE)) putExtra(EXTRA_THEME_CHOICE, intent.getIntExtra(EXTRA_THEME_CHOICE, 0))
            putExtra(EXTRA_ACCOUNT_NAME, account.name)
            if (ignoreAccount) putExtra(EXTRA_IGNORE_ACCOUNT, true)
            putExtra(EXTRA_SCREEN_ID, intent.getIntExtra(EXTRA_SCREEN_ID, 1))
            for (it in intent.extras?.keySet().orEmpty()) {
                if (it.startsWith(EXTRA_SCREEN_OPTIONS_PREFIX)) putExtra(it, intent.getStringExtra(it))
            }
            putExtra(EXTRA_CALLING_PACKAGE_NAME, callingActivity?.packageName)
            if (intent.action != ACTION_BROWSE_SETTINGS) {
                addFlags(FLAG_ACTIVITY_FORWARD_RESULT)
            }
        }

        startActivity(intent)

        if (!isFinishing && !isChangingConfigurations) {
            finishResult(RESULT_OK)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ACCOUNT_PICKER) {
            if (resultCode == RESULT_OK && data?.hasExtra(AccountManager.KEY_ACCOUNT_NAME) == true) {
                intent.putExtra(EXTRA_ACCOUNT_NAME, data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME))
                launchMain()
            } else {
                finishResult(resultCode)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        canAskForAccount = true
        val extras = intent?.extras?.also { it.keySet() }
        Log.d(TAG, "Invoked with ${intent.action} and extras $extras")

        super.onCreate(savedInstanceState)

        val isMainAllowed = if (intent == null || intent.action != ACTION_BROWSE_SETTINGS)
            PackageUtils.isGooglePackage(this, callingActivity?.packageName)
        else
            extras?.getInt(EXTRA_SCREEN_ID, -1) in BROWSABLE_SCREEN_IDS

        if (!isMainAllowed) {
            launchFallback()
        } else if (!isFinishing && !isChangingConfigurations){
            launchMain()
        }
    }

    private fun finishResult(resultCode: Int) {
        setResult(resultCode)
        finish()
    }
}