/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.signin

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.R
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInApi
import com.google.android.gms.auth.api.signin.SignInAccount
import com.google.android.gms.auth.api.signin.internal.SignInConfiguration
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import com.google.android.gms.databinding.SigninConfirmBinding
import com.google.android.gms.databinding.SigninPickerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.AuthConstants.DEFAULT_ACCOUNT
import org.microg.gms.auth.AuthConstants.DEFAULT_ACCOUNT_TYPE
import org.microg.gms.auth.login.LoginActivity
import org.microg.gms.people.PeopleManager
import org.microg.gms.utils.getApplicationLabel

private const val TAG = "AuthSignInActivity"
private const val REQUEST_CODE_ADD_ACCOUNT = 100

/**
 * TODO: Get privacy policy / terms of service links via
 *       https://clientauthconfig.googleapis.com/google.identity.clientauthconfig.v1.ClientAuthConfig/GetDisplayBrand
 */
class AuthSignInActivity : AppCompatActivity() {
    private val config: SignInConfiguration?
        get() = runCatching {
            intent?.extras?.also { it.classLoader = SignInConfiguration::class.java.classLoader }?.getParcelable<SignInConfiguration>("config")
        }.getOrNull()

    private val idNonce: String?
        get() = runCatching { intent?.extras?.getString("nonce") }.getOrNull()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(CommonStatusCodes.CANCELED)

        Log.d(TAG, "Request: $config")

        val packageName = config?.packageName
        if (packageName == null || (packageName != callingActivity?.packageName && callingActivity?.packageName != this.packageName))
            return finishResult(CommonStatusCodes.DEVELOPER_ERROR, "package name mismatch")

        initView(packageName)
    }

    private fun initView(packageName: String) {
        val accountManager = getSystemService<AccountManager>() ?: return finishResult(CommonStatusCodes.INTERNAL_ERROR, "No account manager")
        val accounts = accountManager.getAccountsByType(DEFAULT_ACCOUNT_TYPE)
        if (accounts.isNotEmpty()) {
            val account = config?.options?.account
            if (account != null) {
                if (account in accounts) {
                    showSignInConfirm(packageName, account)
                } else {
                    finishResult(CommonStatusCodes.INVALID_ACCOUNT)
                }
            } else {
                openAccountPicker(packageName)
            }
        } else {
            openAddAccount()
        }
    }

    private fun openAddAccount() {
        startActivityForResult(Intent(this, LoginActivity::class.java), REQUEST_CODE_ADD_ACCOUNT)
    }

    private fun bindAccountRow(root: View, account: Account, updateAction: (ImageView, Bitmap) -> Unit) {
        val photoView = root.findViewById<ImageView>(R.id.account_photo)
        val displayNameView = root.findViewById<TextView>(R.id.account_display_name)
        val emailView = root.findViewById<TextView>(R.id.account_email)
        if (account.name != DEFAULT_ACCOUNT) {
            val photo = PeopleManager.getOwnerAvatarBitmap(this@AuthSignInActivity, account.name, false)
            if (photo == null) {
                lifecycleScope.launchWhenStarted {
                    withContext(Dispatchers.IO) {
                        PeopleManager.getOwnerAvatarBitmap(this@AuthSignInActivity, account.name, true)
                    }?.let {
                        updateAction(photoView, it)
                    }
                }
            }
            val displayName = PeopleManager.getDisplayName(this@AuthSignInActivity, account.name)
            photoView.setImageBitmap(photo)
            if (displayName != null) {
                displayNameView.text = displayName
                emailView.text = account.name
                emailView.visibility = View.VISIBLE
            } else {
                displayNameView.text = account.name
                emailView.visibility = View.GONE
            }
        } else {
            photoView.setImageResource(R.drawable.ic_add_account_alt)
            displayNameView.setText(R.string.signin_picker_add_account_label)
            emailView.visibility = View.GONE
        }
    }

    private fun openAccountPicker(packageName: String) {
        val binding = SigninPickerBinding.inflate(layoutInflater)
        binding.appName = packageManager.getApplicationLabel(packageName).toString()
        binding.appIcon = packageManager.getApplicationIcon(packageName)
        val accounts = getSystemService<AccountManager>()!!.getAccountsByType(DEFAULT_ACCOUNT_TYPE) + Account(DEFAULT_ACCOUNT, DEFAULT_ACCOUNT_TYPE)
        binding.pickerList.adapter = object : ArrayAdapter<Account>(this, 0, accounts) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = convertView ?: layoutInflater.inflate(R.layout.signin_account_row, parent, false)
                getItem(position)?.let { bindAccountRow(v, it) { _, _ -> notifyDataSetChanged() } }
                return v
            }
        }
        binding.pickerList.setOnItemClickListener { parent, view, position, id ->
            binding.listProgressSpinner = true
            if (accounts[position].name == DEFAULT_ACCOUNT) {
                openAddAccount()
            } else {
                lifecycleScope.launchWhenStarted {
                    try {
                        signIn(accounts[position])
                    } catch (e: Exception) {
                        Log.w(TAG, e)
                        finishResult(CommonStatusCodes.INTERNAL_ERROR)
                    }
                }
            }
        }
        setContentView(binding.root)
    }

    private fun showSignInConfirm(packageName: String, account: Account) {
        val binding = SigninConfirmBinding.inflate(layoutInflater)
        binding.appName = packageManager.getApplicationLabel(packageName).toString()
        binding.appIcon = packageManager.getApplicationIcon(packageName)
        bindAccountRow(binding.root, account) { view, bitmap -> view.setImageBitmap(bitmap) }
        binding.button2.setOnClickListener {
            finishResult(CommonStatusCodes.CANCELED)
        }
        binding.button1.setOnClickListener {
            binding.button1.isEnabled = false
            binding.button2.isEnabled = false
            lifecycleScope.launchWhenStarted {
                try {
                    signIn(account)
                } catch (e: Exception) {
                    Log.w(TAG, e)
                    finishResult(CommonStatusCodes.INTERNAL_ERROR)
                }
            }
        }
        setContentView(binding.root)
    }

    private suspend fun signIn(account: Account) {
        val (_, googleSignInAccount) = performSignIn(this, config?.packageName!!, config?.options, account, true, idNonce)
        if (googleSignInAccount != null) {
            finishResult(CommonStatusCodes.SUCCESS, account = account, googleSignInAccount = googleSignInAccount)
        } else {
            finishResult(CommonStatusCodes.INTERNAL_ERROR, "Sign in failed")
        }
    }

    private fun finishResult(statusCode: Int, message: String? = null, account: Account? = null, googleSignInAccount: GoogleSignInAccount? = null) {
        val data = Intent()
        if (statusCode != CommonStatusCodes.SUCCESS) data.putExtra(AuthConstants.ERROR_CODE, statusCode)
        data.putExtra(AuthConstants.GOOGLE_SIGN_IN_STATUS, Status(statusCode, message))
        data.putExtra(AuthConstants.GOOGLE_SIGN_IN_ACCOUNT, googleSignInAccount)
        val bundle = Bundle()
        if (googleSignInAccount != null) {
            val authorizationResult = AuthorizationResult(
                googleSignInAccount.serverAuthCode,
                googleSignInAccount.idToken,
                googleSignInAccount.idToken,
                googleSignInAccount.grantedScopes.map { it.scopeUri },
                googleSignInAccount,
                null
            )
            data.putExtra(AuthConstants.GOOGLE_SIGN_IN_AUTHORIZATION_RESULT, SafeParcelableSerializer.serializeToBytes(authorizationResult))
            val signInAccount = SignInAccount().apply {
                email = googleSignInAccount.email ?: account?.name
                this.googleSignInAccount = googleSignInAccount
                userId = googleSignInAccount.id ?: getSystemService<AccountManager>()?.getUserData(
                    account,
                    AuthConstants.GOOGLE_USER_ID
                )
            }
            data.putExtra(GoogleSignInApi.EXTRA_SIGN_IN_ACCOUNT, signInAccount)
            val credential = SignInCredential(
                googleSignInAccount.email,
                googleSignInAccount.displayName,
                googleSignInAccount.familyName,
                googleSignInAccount.givenName,
                null, null,
                googleSignInAccount.idToken,
                null, null
            )
            val credentialToBytes = SafeParcelableSerializer.serializeToBytes(credential)
            bundle.putByteArray(AuthConstants.SIGN_IN_CREDENTIAL, credentialToBytes)
            bundle.putByteArray(AuthConstants.STATUS, SafeParcelableSerializer.serializeToBytes(Status.SUCCESS))
        } else {
            bundle.putByteArray(AuthConstants.STATUS, SafeParcelableSerializer.serializeToBytes(Status.CANCELED))
        }
        data.putExtras(bundle)
        Log.d(TAG, "Result: ${data.extras?.also { it.keySet() }}")
        setResult(RESULT_OK, data)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ADD_ACCOUNT) {
            val accountManager = getSystemService<AccountManager>() ?: return finish()
            val accounts = accountManager.getAccountsByType(DEFAULT_ACCOUNT_TYPE)
            if (accounts.isNotEmpty()) {
                openAccountPicker(config?.packageName!!)
            } else {
                finishResult(CommonStatusCodes.CANCELED, "No account and creation cancelled")
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        config?.packageName?.let { initView(it) }
    }
}