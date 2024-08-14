package org.microg.gms.auth.workaccount

import android.accounts.Account
import android.accounts.AccountManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Parcel
import android.os.UserManager
import android.util.Log
import com.google.android.gms.auth.account.IWorkAccountService
import com.google.android.gms.auth.account.IWorkAccountService.AddAccountResult
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import com.google.android.gms.dynamic.unwrap
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

private const val TAG = "GmsWorkAccountService"

class WorkAccountService : BaseService(TAG, GmsService.WORK_ACCOUNT) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, WorkAccountServiceImpl(this), ConnectionInfo().apply {
            features = arrayOf(Feature("work_account_client_is_whitelisted", 1))
        } )
    }
}

class WorkAccountServiceImpl(val context: Context) : IWorkAccountService.Stub() {
    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        Log.d(TAG, "$code, $data, $reply, $flags")
        return super.onTransact(code, data, reply, flags)
    }

    override fun addWorkAccount(googleApiClient: IObjectWrapper?, s: String?): IWorkAccountService.AddAccountResult {
        Log.d(TAG, "addWorkAccount with $googleApiClient, $s")
        return object : AddAccountResult.Stub() {
            override fun getAccount(): Account? {
                // TODO

                return AccountManager.get(context).accounts.firstOrNull()?.also { Log.d(TAG, "returning account $it") }
            }

            override fun getStatus(): IObjectWrapper {
                return ObjectWrapper.wrap(Status(CommonStatusCodes.SUCCESS)).also { Log.d(TAG, "returning status $it (${it.unwrap<Status>()})") }
            }
        }
    }

     override fun removeWorkAccount(googleApiClient: IObjectWrapper?, account: IObjectWrapper?): IObjectWrapper {
         Log.d(TAG, "removeWorkAccount")
        return ObjectWrapper.wrap(null)
    }

     override fun setWorkAuthenticatorEnabled(googleApiClient: IObjectWrapper?, b: Boolean) {
         // TODO
         Log.d(TAG, "setWorkAuthenticatorEnabled with $googleApiClient, $b")
         val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
         val userManger = context.getSystemService(Context.USER_SERVICE) as UserManager
         val sharedPreferences = context.getSharedPreferences("work_account_prefs", Context.MODE_PRIVATE)
         sharedPreferences.edit().putBoolean("enabled_by_admin", true).apply()

         val componentName = ComponentName("com.google.android.gms", "com.google.android.gms.auth.account.authenticator.WorkAccountAuthenticatorService")
         //context.packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
    }

     override fun setWorkAuthenticatorEnabledWithResult(googleApiClient: IObjectWrapper?, b: Boolean): IObjectWrapper {
        Log.d(TAG, "setWorkAuthenticatorEnabledWithResult $googleApiClient, $b")
        return ObjectWrapper.wrap(null)
    }
}
