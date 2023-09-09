/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.signin

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Service
import android.content.*
import android.os.*
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import org.microg.gms.auth.AuthConstants
import org.microg.gms.common.PackageUtils
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val PREFERENCES_NAME = "google_account_cache"
private const val DEFAULT_ACCOUNT_PREFIX = "default_google_account_"

private const val MSG_GET_DEFAULT_ACCOUNT = 1
private const val MSG_SET_DEFAULT_ACCOUNT = 2

private const val MSG_DATA_PACKAGE_NAME = "package_name"
private const val MSG_DATA_ACCOUNT = "account"

class SignInConfigurationService : Service() {
    private val preferences: SharedPreferences
        get() = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val accountManager: AccountManager
        get() = getSystemService<AccountManager>()!!

    override fun onBind(intent: Intent?): IBinder {
        return Messenger(object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                val data = when (msg.what) {
                    MSG_GET_DEFAULT_ACCOUNT -> {
                        val packageName = msg.data?.getString(MSG_DATA_PACKAGE_NAME)
                        val account = packageName?.let { getDefaultAccount(it) }
                        bundleOf(
                            MSG_DATA_PACKAGE_NAME to packageName,
                            MSG_DATA_ACCOUNT to account
                        )
                    }

                    MSG_SET_DEFAULT_ACCOUNT -> {
                        val packageName = msg.data?.getString(MSG_DATA_PACKAGE_NAME)
                        val account = msg.data?.getParcelable<Account>(MSG_DATA_ACCOUNT)
                        packageName?.let { setDefaultAccount(it, account) }
                        bundleOf(
                            MSG_DATA_PACKAGE_NAME to packageName,
                            MSG_DATA_ACCOUNT to account
                        )
                    }

                    else -> Bundle.EMPTY
                }
                msg.replyTo?.send(Message.obtain().also {
                    it.what = msg.what
                    it.data = data
                })
            }
        }).binder
    }

    private fun getPackageNameSuffix(packageName: String): String {
        return packageName + ":" + PackageUtils.firstSignatureDigest(this, packageName)
    }

    private fun getDefaultAccount(packageName: String): Account? {
        val name = preferences.getString(DEFAULT_ACCOUNT_PREFIX + getPackageNameSuffix(packageName), null)
        if (name.isNullOrBlank()) return null
        val accounts: Array<Account> = accountManager.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)
        for (account in accounts) {
            if (account.name.equals(name)) return account
        }
        return null
    }

    private fun setDefaultAccount(packageName: String, account: Account?) {
        val editor: SharedPreferences.Editor = preferences.edit()
        if (account == null || account.name == AuthConstants.DEFAULT_ACCOUNT) {
            editor.remove(DEFAULT_ACCOUNT_PREFIX + getPackageNameSuffix(packageName))
        } else {
            editor.putString(DEFAULT_ACCOUNT_PREFIX + getPackageNameSuffix(packageName), account.name)
        }
        editor.apply()
    }

    companion object {

        private suspend fun singleRequest(context: Context, message: Message) = suspendCoroutine { continuation ->
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val connection = this
                    message.replyTo = Messenger(object : Handler(Looper.myLooper() ?: Looper.getMainLooper()) {
                        override fun handleMessage(msg: Message) {
                            runCatching { continuation.resume(msg) }
                            runCatching { context.unbindService(connection) }
                        }
                    })
                    try {
                        Messenger(service).send(message)
                    } catch (e: Exception) {
                        runCatching { continuation.resumeWithException(e) }
                        runCatching { context.unbindService(connection) }
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    runCatching { continuation.resumeWithException(RuntimeException("Disconnected")) }
                }
            }
            val connected = context.bindService(Intent(context, SignInConfigurationService::class.java), connection, BIND_AUTO_CREATE or BIND_ABOVE_CLIENT)
            if (!connected) {
                runCatching { continuation.resumeWithException(RuntimeException("Connection failed")) }
                runCatching { context.unbindService(connection) }
            }
        }

        suspend fun getDefaultAccount(context: Context, packageName: String): Account? {
            return singleRequest(context, Message.obtain().apply {
                what = MSG_GET_DEFAULT_ACCOUNT
                data = bundleOf(
                    MSG_DATA_PACKAGE_NAME to packageName
                )
            }).data?.getParcelable(MSG_DATA_ACCOUNT)
        }

        suspend fun setDefaultAccount(context: Context, packageName: String, account: Account?) {
            singleRequest(context, Message.obtain().apply {
                what = MSG_SET_DEFAULT_ACCOUNT
                data = bundleOf(
                    MSG_DATA_PACKAGE_NAME to packageName,
                    MSG_DATA_ACCOUNT to account
                )
            })
        }

    }
}