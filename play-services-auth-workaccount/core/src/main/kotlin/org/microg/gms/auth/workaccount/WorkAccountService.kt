package org.microg.gms.auth.workaccount

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.os.Parcel
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
        return ObjectWrapper.wrap(null)
    }

     override fun setWorkAuthenticatorEnabled(googleApiClient: IObjectWrapper?, b: Boolean) {
         // TODO
         Log.d(TAG, "setWorkAuthenticatorEnabled with $googleApiClient, $b")
    }

     override fun setWorkAuthenticatorEnabledWithResult(googleApiClient: IObjectWrapper?, b: Boolean): IObjectWrapper {
        return ObjectWrapper.wrap(null)
    }
}
