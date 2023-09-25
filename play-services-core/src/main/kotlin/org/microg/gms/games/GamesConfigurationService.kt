/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games

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

private const val PREFERENCES_NAME = "games.config"
private const val PREF_ACCOUNT_GLOBAL = "account_global"
private const val PREF_ACCOUNT_PREFIX = "account_"
private const val PREF_PLAYER_PREFIX = "player_"

private const val MSG_GET_DEFAULT_ACCOUNT = 1
private const val MSG_SET_DEFAULT_ACCOUNT = 2
private const val MSG_GET_PLAYER = 3
private const val MSG_SET_PLAYER = 4

private const val MSG_DATA_PACKAGE_NAME = "package_name"
private const val MSG_DATA_ACCOUNT = "account"
private const val MSG_DATA_PLAYER = "player"

class GamesConfigurationService : Service() {
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
                        val account = getDefaultAccount(packageName)
                        bundleOf(
                            MSG_DATA_PACKAGE_NAME to packageName,
                            MSG_DATA_ACCOUNT to account
                        )
                    }

                    MSG_SET_DEFAULT_ACCOUNT -> {
                        val packageName = msg.data?.getString(MSG_DATA_PACKAGE_NAME)
                        val account = msg.data?.getParcelable<Account>(MSG_DATA_ACCOUNT)
                        setDefaultAccount(packageName, account)
                        bundleOf(
                            MSG_DATA_PACKAGE_NAME to packageName,
                            MSG_DATA_ACCOUNT to account
                        )
                    }

                    MSG_GET_PLAYER -> {
                        val packageName = msg.data?.getString(MSG_DATA_PACKAGE_NAME)
                        val account = msg.data?.getParcelable<Account>(MSG_DATA_ACCOUNT)
                        val player = if (packageName != null && account != null) getPlayer(packageName, account) else null
                        bundleOf(
                            MSG_DATA_PACKAGE_NAME to packageName,
                            MSG_DATA_ACCOUNT to account,
                            MSG_DATA_PLAYER to player
                        )
                    }

                    MSG_SET_PLAYER -> {
                        val packageName = msg.data?.getString(MSG_DATA_PACKAGE_NAME)
                        val account = msg.data?.getParcelable<Account>(MSG_DATA_ACCOUNT)
                        val player = msg.data?.getString(MSG_DATA_PLAYER)
                        if (packageName != null && account != null) setPlayer(packageName, account, player)
                        bundleOf(
                            MSG_DATA_PACKAGE_NAME to packageName,
                            MSG_DATA_ACCOUNT to account,
                            MSG_DATA_PLAYER to player
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

    private fun getGlobalDefaultAccount(): Account? {
        val name = preferences.getString(PREF_ACCOUNT_GLOBAL, null)
        val accounts: Array<Account> = accountManager.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)
        for (account in accounts) {
            if (account.name.equals(name)) return account
        }
        return null
    }

    private fun getDefaultAccount(packageName: String?): Account? {
        if (packageName == null) return getGlobalDefaultAccount()
        val name = preferences.getString(PREF_ACCOUNT_PREFIX + getPackageNameSuffix(packageName), null)
        if (name.isNullOrBlank()) return getGlobalDefaultAccount()
        val accounts: Array<Account> = accountManager.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)
        for (account in accounts) {
            if (account.name.equals(name)) return account
        }
        return getGlobalDefaultAccount()
    }

    private fun setDefaultAccount(packageName: String?, account: Account?) {
        if (account?.name == getDefaultAccount(packageName)?.name) return
        val key = if (packageName == null) PREF_ACCOUNT_GLOBAL else (PREF_ACCOUNT_PREFIX + getPackageNameSuffix(packageName))
        val editor: SharedPreferences.Editor = preferences.edit()
        if (account == null || account.name == AuthConstants.DEFAULT_ACCOUNT) {
            editor.remove(key)
        } else {
            editor.putString(key, account.name)
        }
        if (packageName != null) {
            for (key in preferences.all.keys) {
                if (key.startsWith(PREF_PLAYER_PREFIX + getPackageNameSuffix(packageName))) {
                    editor.remove(key)
                }
            }
        }
        editor.apply()
    }

    private fun getPackageAndAccountSuffix(packageName: String, account: Account): String {
        return getPackageNameSuffix(packageName) + ":" + account.name
    }

    private fun getPlayer(packageName: String, account: Account): String? {
        val player = preferences.getString(PREF_PLAYER_PREFIX + getPackageAndAccountSuffix(packageName, account), null)
        if (player.isNullOrBlank()) return null
        return player
    }

    private fun setPlayer(packageName: String, account: Account, player: String?) {
        val editor: SharedPreferences.Editor = preferences.edit()
        if (player.isNullOrBlank()) {
            editor.remove(PREF_PLAYER_PREFIX + getPackageAndAccountSuffix(packageName, account))
        } else {
            editor.putString(PREF_PLAYER_PREFIX + getPackageAndAccountSuffix(packageName, account), player)
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
            val connected = context.bindService(Intent(context, GamesConfigurationService::class.java), connection, BIND_AUTO_CREATE or BIND_ABOVE_CLIENT)
            if (!connected) {
                runCatching { continuation.resumeWithException(RuntimeException("Connection failed")) }
                runCatching { context.unbindService(connection) }
            }
        }

        suspend fun getDefaultAccount(context: Context, packageName: String?): Account? {
            return singleRequest(context, Message.obtain().apply {
                what = MSG_GET_DEFAULT_ACCOUNT
                data = bundleOf(
                    MSG_DATA_PACKAGE_NAME to packageName
                )
            }).data?.getParcelable(MSG_DATA_ACCOUNT)
        }

        suspend fun setDefaultAccount(context: Context, packageName: String?, account: Account?) {
            singleRequest(context, Message.obtain().apply {
                what = MSG_SET_DEFAULT_ACCOUNT
                data = bundleOf(
                    MSG_DATA_PACKAGE_NAME to packageName,
                    MSG_DATA_ACCOUNT to account
                )
            })
        }

        suspend fun getPlayer(context: Context, packageName: String, account: Account): String? {
            return singleRequest(context, Message.obtain().apply {
                what = MSG_GET_PLAYER
                data = bundleOf(
                    MSG_DATA_PACKAGE_NAME to packageName,
                    MSG_DATA_ACCOUNT to account
                )
            }).data?.getString(MSG_DATA_PLAYER)
        }

        suspend fun setPlayer(context: Context, packageName: String, account: Account, player: String?) {
            singleRequest(context, Message.obtain().apply {
                what = MSG_SET_PLAYER
                data = bundleOf(
                    MSG_DATA_PACKAGE_NAME to packageName,
                    MSG_DATA_ACCOUNT to account,
                    MSG_DATA_PLAYER to player
                )
            })
        }

    }
}