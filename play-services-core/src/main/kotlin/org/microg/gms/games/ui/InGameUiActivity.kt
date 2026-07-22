/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.R
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

    @SuppressLint("CommitTransaction")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.ThemeTranslucentCommon)
        Log.d(TAG, "InGameUiActivity onCreate: clientPackageName:$clientPackageName")
        if (clientPackageName == null || accountKey == null) {
            Log.d(TAG, "InGameUiActivity finishResult: params invalid")
            finish()
            return
        }

        val fragment = supportFragmentManager.findFragmentByTag(GamesUiFragment.TAG)
        if (fragment == null) {
            Log.d(TAG, "supportFragmentManager show")
            val gamesUiFragment = GamesUiFragment.newInstance(clientPackageName!!, accountKey!!, intent)
            supportFragmentManager.beginTransaction()
                .add(gamesUiFragment, GamesUiFragment.TAG)
                .commitAllowingStateLoss()
        }
    }

}