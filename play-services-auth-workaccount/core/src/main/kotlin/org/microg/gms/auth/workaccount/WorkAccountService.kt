/*
 * SPDX-FileCopyrightText: 2024 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.workaccount

import android.accounts.Account
import android.accounts.AccountManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import com.google.android.gms.auth.account.IWorkAccountCallback
import com.google.android.gms.auth.account.IWorkAccountService
import com.google.android.gms.auth.account.authenticator.WorkAccountAuthenticator.Companion.KEY_ACCOUNT_CREATION_TOKEN
import com.google.android.gms.auth.account.authenticator.WorkAccountAuthenticator.Companion.WORK_ACCOUNT_CHANGED_BOARDCAST
import com.google.android.gms.auth.account.authenticator.WorkAccountAuthenticatorService
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.auth.AuthConstants
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils

private const val TAG = "GmsWorkAccountService"

class WorkAccountService : BaseService(TAG, GmsService.WORK_ACCOUNT) {
    override fun handleServiceRequest(
        callback: IGmsCallbacks,
        request: GetServiceRequest,
        service: GmsService
    ) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
        val policyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val authorized = policyManager.isDeviceAdminApp(packageName)

        if (authorized) {
            callback.onPostInitCompleteWithConnectionInfo(
                CommonStatusCodes.SUCCESS,
                WorkAccountServiceImpl(this),
                ConnectionInfo().apply {
                    features = arrayOf(Feature("work_account_client_is_whitelisted", 1))
                })
        } else {
            // Return mock response, don't tell client that it is whitelisted
            callback.onPostInitCompleteWithConnectionInfo(
                CommonStatusCodes.SUCCESS,
                UnauthorizedWorkAccountServiceImpl(),
                ConnectionInfo().apply {
                    features = emptyArray()
                })
        }
    }
}

private fun DevicePolicyManager.isDeviceAdminApp(packageName: String?): Boolean {
    if (packageName == null) return false
    return if (SDK_INT >= 21) {
        isDeviceOwnerApp(packageName) || isProfileOwnerApp(packageName)
    } else {
        isDeviceOwnerApp(packageName)
    }
}

class WorkAccountServiceImpl(val context: Context) : IWorkAccountService.Stub() {

    val packageManager: PackageManager = context.packageManager
    val accountManager: AccountManager = AccountManager.get(context)

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        Log.d(TAG, "$code, $data, $reply, $flags")
        return super.onTransact(code, data, reply, flags)
    }

    override fun setWorkAuthenticatorEnabled(enabled: Boolean) {
        Log.d(TAG, "setWorkAuthenticatorEnabled with $enabled")

        val componentName = ComponentName(
            context,
            WorkAccountAuthenticatorService::class.java
        )
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    override fun addWorkAccount(
        callback: IWorkAccountCallback?,
        token: String?
    ) {
        Log.d(TAG, "addWorkAccount with token $token")
        val future = accountManager.addAccount(
            AuthConstants.WORK_ACCOUNT_TYPE,
            null,
            null,
            Bundle().apply { putString(KEY_ACCOUNT_CREATION_TOKEN, token) },
            null,
            null,
            null
        )
        Thread {
            try {
                future.result.let { result ->
                    callback?.onAccountAdded(
                        Account(
                            result.getString(AccountManager.KEY_ACCOUNT_NAME)!!,
                            result.getString(AccountManager.KEY_ACCOUNT_TYPE)!!
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "could not add work account with error message: ${e.message}")
            }
        }.start()
    }

    override fun removeWorkAccount(
        callback: IWorkAccountCallback?,
        account: Account?
    ) {
        Log.d(TAG, "removeWorkAccount with account ${account?.name}")
        account?.let {
            if (SDK_INT >= 22) {

                val success = accountManager.removeAccountExplicitly(it)

                // Notify vending package
                context.sendBroadcast(
                    Intent(WORK_ACCOUNT_CHANGED_BOARDCAST).setPackage("com.android.vending")
                )

                callback?.onAccountRemoved(success)
            } else {
                val future = accountManager.removeAccount(it, null, null)
                Thread {
                    future.result.let { result ->
                        callback?.onAccountRemoved(result)
                    }
                }.start()
            }
        }
    }
}

class UnauthorizedWorkAccountServiceImpl : IWorkAccountService.Stub() {
    override fun setWorkAuthenticatorEnabled(enabled: Boolean) {
        throw SecurityException("client not admin, yet tried to enable work authenticator")
    }

    override fun addWorkAccount(callback: IWorkAccountCallback?, token: String?) {
        throw SecurityException("client not admin, yet tried to add work account")
    }

    override fun removeWorkAccount(callback: IWorkAccountCallback?, account: Account?) {
        throw SecurityException("client not admin, yet tried to remove work account")
    }
}