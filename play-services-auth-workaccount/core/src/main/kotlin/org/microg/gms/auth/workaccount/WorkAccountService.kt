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
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.AuthRequest
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils

private const val TAG = "GmsWorkAccountService"

const val WORK_ACCOUNT_CHANGED_BROADCAST = "org.microg.vending.WORK_ACCOUNT_CHANGED"

class WorkAccountService : BaseService(TAG, GmsService.WORK_ACCOUNT_API) {
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
            "org.microg.gms.auth.WorkAccountAuthenticatorService"
        )
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    /**
     * @return `null` if account creation fails, the newly created account otherwise
     */
    fun addAccountInternal(
        accountCreationToken: String
    ): Account? {

        if (!WorkProfileSettings(context).allowCreateWorkAccount) {
            // TODO: communicate error to user (use `R.string.auth_work_authenticator_disabled_error`)
            Log.w(TAG, "creating a work account is disabled in microG settings")
            return null
        }

        return try {
            val authResponse = AuthRequest().fromContext(context)
                .appIsGms()
                .callerIsGms()
                .service("ac2dm")
                .token(accountCreationToken).isAccessToken()
                .addAccount()
                .getAccountId()
                .droidguardResults("null") // TODO
                .response

            val accountManager = AccountManager.get(context)
            val account = Account(authResponse.email, AuthConstants.WORK_ACCOUNT_TYPE)
            val accountAdded = accountManager.addAccountExplicitly(
                account,
                authResponse.token, Bundle().apply {
                    // Work accounts have no SID / LSID ("BAD_COOKIE") and no first/last name.
                    if (authResponse.accountId.isNotBlank()) {
                        putString(AuthConstants.GOOGLE_USER_ID, authResponse.accountId)
                    }
                    putString(AuthConstants.KEY_ACCOUNT_CAPABILITIES, authResponse.capabilities)
                    putString(AuthConstants.KEY_ACCOUNT_SERVICES, authResponse.services)
                    if (authResponse.services != "android") {
                        Log.i(
                            TAG,
                            "unexpected 'services' value ${authResponse.services} (usually 'android')"
                        )
                    }
                })

            if (accountAdded) {

                // Notify vending package
                context.sendBroadcast(
                    Intent(WORK_ACCOUNT_CHANGED_BROADCAST).setPackage("com.android.vending")
                )

                // Report successful creation to caller
                account
            } else null
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to add work account.", exception)
            null
        }
    }

    override fun addWorkAccount(
        callback: IWorkAccountCallback?,
        token: String
    ) {
        Log.d(TAG, "addWorkAccount with token $token")
        Thread {
            addAccountInternal(token)?.let {
                callback?.onAccountAdded(it)
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
                    Intent(WORK_ACCOUNT_CHANGED_BROADCAST).setPackage("com.android.vending")
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