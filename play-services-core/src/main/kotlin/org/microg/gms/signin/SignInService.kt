/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.signin

import android.accounts.Account
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.*
import com.google.android.gms.signin.internal.*
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "SignInService"

class SignInService : BaseService(TAG, GmsService.SIGN_IN) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        val binder = SignInServiceImpl().asBinder()
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, binder, Bundle())
    }
}

class SignInServiceImpl : ISignInService.Stub() {
    override fun clearAccountFromSessionStore(sessionId: Int) {
        Log.d(TAG, "Not yet implemented: clearAccountFromSessionStore $sessionId")
    }

    override fun putAccount(sessionId: Int, account: Account?, callbacks: ISignInCallbacks?) {
        Log.d(TAG, "Not yet implemented: putAccount")
    }

    override fun saveDefaultAccount(accountAccessor: IAccountAccessor?, sessionId: Int, crossClient: Boolean) {
        Log.d(TAG, "Not yet implemented: saveDefaultAccount $sessionId $crossClient")
    }

    override fun saveConsent(request: RecordConsentRequest?, callbacks: ISignInCallbacks?) {
        Log.d(TAG, "Not yet implemented: saveConsent")
    }

    override fun getCurrentAccount(callbacks: ISignInCallbacks?) {
        Log.d(TAG, "Not yet implemented: getCurrentAccount")
    }

    override fun signIn(request: SignInRequest?, callbacks: ISignInCallbacks?) {
        Log.d(TAG, "Not yet implemented: signIn $request")
        callbacks?.onSignIn(SignInResponse().apply {
            connectionResult = ConnectionResult(ConnectionResult.INTERNAL_ERROR)
            response = ResolveAccountResponse().apply {
                connectionResult = ConnectionResult(ConnectionResult.INTERNAL_ERROR)
            }
        })
    }

    override fun setGamesHasBeenGreeted(hasGreeted: Boolean) {
        Log.d(TAG, "Not yet implemented: setGamesHasBeenGreeted")
    }

    override fun recordConsentByConsentResult(request: RecordConsentByConsentResultRequest?, callbacks: ISignInCallbacks?) {
        Log.d(TAG, "Not yet implemented: recordConsentByConsentResult")
    }

    override fun authAccount(request: AuthAccountRequest?, callbacks: ISignInCallbacks?) {
        Log.d(TAG, "Not yet implemented: authAccount")
    }

    override fun onCheckServerAuthorization(result: CheckServerAuthResult?) {
        Log.d(TAG, "Not yet implemented: onCheckServerAuthorization")
    }

    override fun onUploadServerAuthCode(sessionId: Int) {
        Log.d(TAG, "Not yet implemented: onUploadServerAuthCode")
    }

    override fun resolveAccount(request: ResolveAccountRequest?, callbacks: IResolveAccountCallbacks?) {
        Log.d(TAG, "Not yet implemented: resolveAccount")
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}