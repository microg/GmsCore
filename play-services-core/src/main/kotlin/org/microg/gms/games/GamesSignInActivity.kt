/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games

import android.accounts.Account
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.internal.SignInConfiguration
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.Status
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.signin.AuthSignInActivity

private const val TAG = "GamesSignIn"

private const val REQUEST_CODE_GOOGLE_SIGN_IN = 200

class GamesSignInActivity : AppCompatActivity() {
    val gamePackageName: String?
        get() = intent?.getStringExtra(EXTRA_GAME_PACKAGE_NAME)
    val account: Account?
        get() = IntentCompat.getParcelableExtra(intent, EXTRA_ACCOUNT, Account::class.java)
    val popupGravity: Int
        get() = intent?.getIntExtra(EXTRA_POPUP_GRAVITY, Gravity.TOP or Gravity.CENTER_HORIZONTAL) ?: (Gravity.TOP or Gravity.CENTER_HORIZONTAL)
    val scopes: List<Scope>
        get() = IntentCompat.getParcelableArrayExtra(intent, EXTRA_SCOPES, Scope::class.java)?.filterIsInstance<Scope>().orEmpty().realScopes

    private val Int.px: Int get() = (this * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (gamePackageName == null || (gamePackageName != callingActivity?.packageName && callingActivity?.packageName != packageName)) return finish()

        window.setGravity(popupGravity)
        startActivityForResult(Intent(this, AuthSignInActivity::class.java).apply {
            Log.d(TAG, "Sign in for $gamePackageName account:${account?.name} scopes:$scopes")
            putExtra("config", SignInConfiguration().apply {
                packageName = gamePackageName
                options = GoogleSignInOptions.Builder().apply {
                    for (scope in scopes) {
                        requestScopes(scope)
                    }
                    account?.name?.takeIf { it != AuthConstants.DEFAULT_ACCOUNT }?.let { setAccountName(it) }
                }.build()
            })
            Log.d(TAG, "Redirect to GOOGLE_SIGN_IN using $this")
        }, REQUEST_CODE_GOOGLE_SIGN_IN)
    }

    private suspend fun signIn(account: Account) {
        Log.d(TAG, "Sign in as ${account.name}")
        if (performGamesSignIn(this, gamePackageName!!, account, permitted = true, scopes = scopes)) {
            GamesConfigurationService.setDefaultAccount(this, gamePackageName, account)
        }
        setResult(RESULT_OK, Intent().apply {
            putExtra(EXTRA_ACCOUNT, account)
        })
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_GOOGLE_SIGN_IN) {
            val status = data?.extras?.also { it.classLoader = Status::class.java.classLoader }?.getParcelable<Status>("googleSignInStatus")
            if (status?.isSuccess == true) {
                val account = data.extras?.also { it.classLoader = GoogleSignInAccount::class.java.classLoader }
                        ?.getParcelable<GoogleSignInAccount>("googleSignInAccount")?.account
                if (account != null) {
                    lifecycleScope.launchWhenStarted {
                        signIn(account)
                    }
                    return
                }
            }
            finish()
        }
    }
}