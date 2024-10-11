/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games.ui

import android.accounts.AccountManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.R
import org.microg.gms.auth.AuthConstants
import org.microg.gms.games.EXTRA_ACCOUNT_KEY
import org.microg.gms.games.EXTRA_GAME_PACKAGE_NAME

private const val TAG = "InGameUiActivity"

class InGameUiActivity : AppCompatActivity() {

    private val clientPackageName: String?
        get() = runCatching {
            intent?.extras?.getString(EXTRA_GAME_PACKAGE_NAME)
        }.getOrNull()
    private val accountKey: String?
        get() = runCatching {
            intent.getStringExtra(EXTRA_ACCOUNT_KEY)
        }.getOrNull()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.ThemeTranslucentCommon)
        Log.d(TAG, "InGameUiActivity onCreate: clientPackageName:$clientPackageName")
        if (clientPackageName == null || accountKey == null) {
            Log.d(TAG, "InGameUiActivity finishResult: params invalid")
            finish()
            return
        }
        val account = AccountManager.get(this).accounts.filter {
            it.type == AuthConstants.DEFAULT_ACCOUNT_TYPE && Integer.toHexString(it.name.hashCode()) == accountKey
        }.getOrNull(0)
        GamesUiFragment(clientPackageName!!, account, intent).show(supportFragmentManager, "GamesUiFragment")
    }

}