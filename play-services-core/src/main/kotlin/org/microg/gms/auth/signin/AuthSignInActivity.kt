/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.signin

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.R
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.SignInAccount
import com.google.android.gms.auth.api.signin.internal.SignInConfiguration
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.databinding.SigninConfirmBinding
import com.google.android.gms.databinding.SigninPickerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.auth.AuthConstants.DEFAULT_ACCOUNT
import org.microg.gms.auth.AuthConstants.DEFAULT_ACCOUNT_TYPE
import org.microg.gms.auth.login.LoginActivity
import org.microg.gms.people.DatabaseHelper
import org.microg.gms.people.PeopleManager
import org.microg.gms.utils.getApplicationLabel

const val REQUEST_CODE_SIGN_IN = 100
const val REQUEST_CODE_PICK_ACCOUNT = 101

/**
 * TODO: Get privacy policy / terms of service links via
 *       https://clientauthconfig.googleapis.com/google.identity.clientauthconfig.v1.ClientAuthConfig/GetDisplayBrand
 */
class AuthSignInActivity : AppCompatActivity() {
    private val config: SignInConfiguration?
        get() = runCatching {
            intent?.extras?.also { it.classLoader = SignInConfiguration::class.java.classLoader }?.getParcelable<SignInConfiguration>("config")
        }.getOrNull()

    private val Int.px: Int get() = (this * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageName = config?.packageName
        if (packageName == null || packageName != callingActivity?.packageName) return finishResult(CommonStatusCodes.DEVELOPER_ERROR)
        val accountManager = getSystemService<AccountManager>() ?: return finishResult(CommonStatusCodes.DEVELOPER_ERROR)

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
        startActivityForResult(Intent(this, LoginActivity::class.java), REQUEST_CODE_SIGN_IN)
    }

    private fun getDisplayName(account: Account): String? {
        val cursor = DatabaseHelper(this).getOwner(account.name)
        return try {
            if (cursor.moveToNext()) {
                cursor.getColumnIndex("display_name").takeIf { it >= 0 }?.let { cursor.getString(it) }.takeIf { !it.isNullOrBlank() }
            } else null
        } finally {
            cursor.close()
        }
    }

    private fun bindAccountRow(root: View, account: Account, updateAction: (ImageView, Bitmap) -> Unit) {
        val photoView = root.findViewById<ImageView>(R.id.account_photo)
        val displayNameView = root.findViewById<TextView>(R.id.account_display_name)
        val emailView = root.findViewById<TextView>(R.id.account_email)
        if (account.name != DEFAULT_ACCOUNT) {
            val photo = PeopleManager.getOwnerAvatarBitmap(this@AuthSignInActivity, account.name, false)
            if (photo == null) {
                lifecycleScope.launchWhenStarted {
                    val bitmap = withContext(Dispatchers.IO) {
                        PeopleManager.getOwnerAvatarBitmap(this@AuthSignInActivity, account.name, true)
                    }
                    updateAction(photoView, bitmap)
                }
            }
            val displayName = getDisplayName(account)
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
                    signIn(accounts[position])
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
                signIn(account)
            }
        }
        setContentView(binding.root)
    }

    private suspend fun signIn(account: Account) {
        val googleSignInAccount = performSignIn(this, config?.packageName!!, config?.options, account, true)
        if (googleSignInAccount != null) {
            finishResult(CommonStatusCodes.SUCCESS, googleSignInAccount)
        } else {
            finishResult(CommonStatusCodes.CANCELED)
        }
    }

    private fun finishResult(statusCode: Int? = null, account: GoogleSignInAccount? = null) {
        val data = Intent()
        data.putExtra("googleSignInStatus", statusCode?.let { Status(it) })
        data.putExtra("googleSignInAccount", account)
        if (account != null) {
            data.putExtra("signInAccount", SignInAccount().apply {
                email = account.email
                googleSignInAccount = account
                userId = account.id
            })
        }
        setResult(RESULT_OK, data)
        val extras = data.extras
        extras?.keySet()
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SIGN_IN -> {
                val accountManager = getSystemService<AccountManager>() ?: return finish()
                val accounts = accountManager.getAccountsByType(DEFAULT_ACCOUNT_TYPE)
                if (accounts.isNotEmpty()) {
                    openAccountPicker(config?.packageName!!)
                } else {
                    finishResult(CommonStatusCodes.CANCELED)
                }
            }

            REQUEST_CODE_PICK_ACCOUNT -> {
                val accountName = data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                if (resultCode == RESULT_OK && accountName != null) {
                    val account = Account(accountName, DEFAULT_ACCOUNT_TYPE)
                    lifecycleScope.launchWhenStarted {
                        signIn(account)
                    }
                } else {
                    finishResult(CommonStatusCodes.CANCELED)
                }
            }
        }
    }
}