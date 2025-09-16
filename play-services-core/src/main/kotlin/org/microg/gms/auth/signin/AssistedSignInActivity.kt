/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.signin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.R
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.internal.SignInConfiguration
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import org.microg.gms.auth.AuthConstants
import org.microg.gms.common.Constants

const val ACTION_ASSISTED_SIGN_IN = "com.google.android.gms.auth.api.credentials.ASSISTED_SIGNIN"
const val GET_SIGN_IN_INTENT_REQUEST = "get_sign_in_intent_request"
const val BEGIN_SIGN_IN_REQUEST = "begin_sign_in_request"
const val CLIENT_PACKAGE_NAME = "client_package_name"
const val GOOGLE_SIGN_IN_OPTIONS = "google_sign_in_options"

private const val TAG = "AssistedSignInActivity"
private const val REQUEST_CODE_SIGN_IN = 120

class AssistedSignInActivity : AppCompatActivity() {

    private val googleSignInOptions: GoogleSignInOptions?
        get() = runCatching {
            intent?.extras?.takeIf { it.containsKey(GOOGLE_SIGN_IN_OPTIONS) }?.getByteArray(GOOGLE_SIGN_IN_OPTIONS)
                ?.let {
                    return SafeParcelableSerializer.deserializeFromBytes(it, GoogleSignInOptions.CREATOR)
                }
        }.getOrNull()

    private val signInIntentRequest: GetSignInIntentRequest?
        get() = runCatching {
            intent?.extras?.takeIf { it.containsKey(GET_SIGN_IN_INTENT_REQUEST) }
                ?.getByteArray(GET_SIGN_IN_INTENT_REQUEST)?.let {
                    return SafeParcelableSerializer.deserializeFromBytes(it, GetSignInIntentRequest.CREATOR)
                }
        }.getOrNull()

    private val beginSignInRequest: BeginSignInRequest?
        get() = runCatching {
            intent?.extras?.takeIf { it.containsKey(BEGIN_SIGN_IN_REQUEST) }?.getByteArray(BEGIN_SIGN_IN_REQUEST)?.let {
                return SafeParcelableSerializer.deserializeFromBytes(it, BeginSignInRequest.CREATOR)
            }
        }.getOrNull()

    private val clientPackageName: String?
        get() = runCatching {
            intent?.extras?.takeIf { it.containsKey(CLIENT_PACKAGE_NAME) }?.getString(CLIENT_PACKAGE_NAME)
        }.getOrNull()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.ThemeTranslucentCommon)
        Log.d(TAG, "onCreate: packageName:$packageName clientPackageName:$clientPackageName")
        if (packageName == null || clientPackageName == null) return errorResult(
            Status(
                CommonStatusCodes.ERROR, "Invalid calling package."
            )
        )
        if (googleSignInOptions == null) return errorResult(Status(CommonStatusCodes.ERROR, "request params invalid."))

        if (signInIntentRequest != null) {
            Log.d(TAG, "signInIntentRequest start")
            prepareSignIn()
            return
        }

        if (beginSignInRequest != null) {
            Log.d(TAG, "beginSignInRequest start")
            val fragment = supportFragmentManager.findFragmentByTag(AssistedSignInFragment.TAG)
            if (fragment != null) {
                val assistedSignInFragment = fragment as AssistedSignInFragment
                assistedSignInFragment.cancelLogin(true)
            } else {
                AssistedSignInFragment.newInstance(clientPackageName!!, googleSignInOptions!!, beginSignInRequest!!)
                    .show(supportFragmentManager, AssistedSignInFragment.TAG)
            }
            return
        }

        errorResult(Status(CommonStatusCodes.ERROR, "Intent data corrupted."))
    }

    private fun prepareSignIn() {
        Log.d(TAG, "prepareSignIn options:$googleSignInOptions")
        val signInConfiguration = SignInConfiguration().apply {
            options = googleSignInOptions
            packageName = clientPackageName
        }
        val intent = Intent(this, AuthSignInActivity::class.java).apply {
            `package` = Constants.GMS_PACKAGE_NAME
            putExtra("config", signInConfiguration)
        }
        startActivityForResult(intent, REQUEST_CODE_SIGN_IN)
    }

    fun errorResult(status: Status) {
        Log.d(TAG, "errorResult: $status")
        setResult(RESULT_CANCELED, Intent().apply {
            putExtra(AuthConstants.STATUS, SafeParcelableSerializer.serializeToBytes(status))
        })
        finish()
    }

    fun loginResult(googleSignInAccount: GoogleSignInAccount?) {
        if (googleSignInAccount == null) {
            errorResult(Status(CommonStatusCodes.CANCELED, "User cancelled."))
            return
        }
        Log.d(TAG, "loginResult: googleSignInAccount: $googleSignInAccount")
        val credential = SignInCredential(
            googleSignInAccount.email,
            googleSignInAccount.displayName,
            googleSignInAccount.givenName,
            googleSignInAccount.familyName,
            null,
            null,
            googleSignInAccount.idToken,
            null,
            null
        )
        setResult(RESULT_OK, Intent().apply {
            putExtra(AuthConstants.SIGN_IN_CREDENTIAL, SafeParcelableSerializer.serializeToBytes(credential))
            putExtra(AuthConstants.STATUS, SafeParcelableSerializer.serializeToBytes(Status.SUCCESS))
        })
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            val googleSignInAccount =
                data?.getParcelableExtra<GoogleSignInAccount>(AuthConstants.GOOGLE_SIGN_IN_ACCOUNT)
            loginResult(googleSignInAccount)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent intent:$intent")
        val fragment = supportFragmentManager.findFragmentByTag(AssistedSignInFragment.TAG)
        if (fragment != null) {
            val assistedSignInFragment = fragment as AssistedSignInFragment
            assistedSignInFragment.cancelLogin(true)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        val fragment = supportFragmentManager.findFragmentByTag(AssistedSignInFragment.TAG)
        if (fragment != null) {
            val assistedSignInFragment = fragment as AssistedSignInFragment
            assistedSignInFragment.initView()
        }
    }
}