package org.microg.gms.auth.workaccount

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import com.google.android.gms.auth.account.IWorkAccountCallback
import com.google.android.gms.auth.account.IWorkAccountService
import com.google.android.gms.auth.account.authenticator.WorkAccountAuthenticator.Companion.KEY_ACCOUNT_CREATION_TOKEN
import com.google.android.gms.auth.account.authenticator.WorkAccountAuthenticator.Companion.WORK_ACCOUNT_TYPE
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "GmsWorkAccountService"

class WorkAccountService : BaseService(TAG, GmsService.WORK_ACCOUNT) {
    override fun handleServiceRequest(
        callback: IGmsCallbacks,
        request: GetServiceRequest,
        service: GmsService
    ) {
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS,
            WorkAccountServiceImpl(this),
            ConnectionInfo().apply {
                features = arrayOf(Feature("work_account_client_is_whitelisted", 1))
            })
    }
}

class WorkAccountServiceImpl(context: Context) : IWorkAccountService.Stub() {

    val packageManager: PackageManager = context.packageManager
    val accountManager: AccountManager = AccountManager.get(context)

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        Log.d(TAG, "$code, $data, $reply, $flags")
        return super.onTransact(code, data, reply, flags)
    }

    override fun setWorkAuthenticatorEnabled(enabled: Boolean) {
        Log.d(TAG, "setWorkAuthenticatorEnabled with $enabled")

        val componentName = ComponentName(
            "com.google.android.gms",
            "com.google.android.gms.auth.account.authenticator.WorkAccountAuthenticatorService"
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
            WORK_ACCOUNT_TYPE,
            null,
            null,
            Bundle().apply { putString(KEY_ACCOUNT_CREATION_TOKEN, token) },
            null,
            null,
            null
        )
        Thread {
            future.result.let { result ->
                callback?.onAccountAdded(
                    Account(
                        result.getString(AccountManager.KEY_ACCOUNT_NAME),
                        result.getString(AccountManager.KEY_ACCOUNT_TYPE)
                    )
                )
            }
        }.start()
    }

    override fun removeWorkAccount(
        callback: IWorkAccountCallback?,
        account: Account?
    ) {
        Log.d(TAG, "removeWorkAccount with account ${account?.name}")
        account?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val success = accountManager.removeAccountExplicitly(it)
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
