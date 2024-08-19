/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.vending

import android.accounts.AccountManager
import android.accounts.AuthenticatorException
import android.accounts.OperationCanceledException
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import com.google.android.phonesky.header.PayloadsProtoStore
import com.google.android.phonesky.header.PhoneskyHeaderValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.microg.vending.billing.DEFAULT_ACCOUNT_TYPE
import org.microg.vending.billing.FINSKY_REGULAR
import org.microg.vending.billing.FINSKY_STABLE
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val accountManager = AccountManager.get(this)
        val accounts = accountManager.getAccountsByType(DEFAULT_ACCOUNT_TYPE)

        if (isMainProcess() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && accounts.isNotEmpty()) {
            GlobalScope.launch(Dispatchers.IO) {
                val payloads = PayloadsProtoStore.readCache(applicationContext)
                if (payloads == null || payloads.mvalue.isEmpty()) PayloadsProtoStore.cachePayload(accounts[0], applicationContext)
                for (account in accounts) {
                    var token : String?
                    val accountName = account.name
                    val future = accountManager.getAuthToken(
                        account,
                        "oauth2:https://www.googleapis.com/auth/experimentsandconfigs",
                        null,
                        false,
                        null,
                        null
                    )
                    try {
                        val bundle = future.getResult(2, TimeUnit.SECONDS)
                        token = bundle.getString(AccountManager.KEY_AUTHTOKEN)
                    } catch (e: Exception) {
                        return@launch
                    }

                    if (token == null) {
                        Log.d("MainApplication", "onCreate token is null")
                        return@launch
                    }

                    ExperimentAndConfigs.postRequest(
                        ExperimentAndConfigs.buildRequestData(this@MainApplication), this@MainApplication, accountName, token)
                    ExperimentAndConfigs.postRequest(
                        ExperimentAndConfigs.buildRequestData(this@MainApplication, "NEW_APPLICATION_SYNC", FINSKY_REGULAR, account), this@MainApplication, accountName, token)
                    ExperimentAndConfigs.postRequest(
                        ExperimentAndConfigs.buildRequestData(this@MainApplication, "NEW_USER_SYNC_ALL_ACCOUNT", null, null), this@MainApplication, "", token)
                    ExperimentAndConfigs.buildExperimentsFlag(this@MainApplication, accountName, FINSKY_REGULAR)
                    ExperimentAndConfigs.buildExperimentsFlag(this@MainApplication, "", FINSKY_REGULAR)
                    ExperimentAndConfigs.buildExperimentsFlag(this@MainApplication, accountName, FINSKY_STABLE)
                }
                try {
                    PhoneskyHeaderValue.getPhoneskyHeader(this@MainApplication, accounts[0])
                } catch (e: IOException) {
                    throw RuntimeException(e)
                } catch (e: IllegalAccessException) {
                    throw RuntimeException(e)
                } catch (e: AuthenticatorException) {
                    throw RuntimeException(e)
                } catch (e: OperationCanceledException) {
                    throw RuntimeException(e)
                }
            }
        }
    }

    private fun isMainProcess(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processInfo = am.runningAppProcesses ?: return false
        val mainProcessName = packageName
        val myPid = Process.myPid()
        return processInfo.any { it.pid == myPid && it.processName == mainProcessName }
    }
}
