package com.google.android.gms.credential.manager

import android.accounts.AccountManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.auth.AuthConstants
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

const val PASSWORD_MANAGER_CLASS_NAME = "com.google.android.gms.credential.manager.PasswordManagerActivity"

const val EXTRA_KEY_ACCOUNT_NAME = "pwm.DataFieldNames.accountName"

private const val TAG = "PasswordManagerActivity"

private const val PSW_MANAGER_PATH = "https://passwords.google.com/"

class PasswordManagerActivity : AppCompatActivity() {

    private val accountName: String?
        get() = runCatching { intent?.getStringExtra(EXTRA_KEY_ACCOUNT_NAME) }.getOrNull()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: start")

        val accounts = AccountManager.get(this).getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)
        val account = accounts.find { it.name.equals(accountName) }

        Log.d(TAG, "realAccountName: ${account?.name}")

        lifecycleScope.launchWhenCreated {
            val service = "weblogin:continue=" + URLEncoder.encode(PSW_MANAGER_PATH, "utf-8")
            val auth = withContext(Dispatchers.IO) {
                val bundle = AccountManager.get(this@PasswordManagerActivity).getAuthToken(account, service, null, null, null, null)
                    .getResult(10, TimeUnit.SECONDS)
                bundle.getString(AccountManager.KEY_AUTHTOKEN)
            }
            val targetIntent = Intent(Intent.ACTION_VIEW, Uri.parse(auth))
            val resolveInfoList = packageManager.queryIntentActivities(targetIntent, 0)
            Log.d(TAG, "resolveInfoList: $resolveInfoList")
            if (resolveInfoList.isNotEmpty()) {
                startActivity(targetIntent)
            }
            finish()
        }
    }

}