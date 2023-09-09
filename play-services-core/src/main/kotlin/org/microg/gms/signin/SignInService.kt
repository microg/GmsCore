/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.signin

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
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
        val binder = SignInServiceImpl(this, lifecycle, packageName, request.scopes).asBinder()
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, binder, Bundle())
    }
}

class SignInServiceImpl(val context: Context, private val lifecycle: Lifecycle, val packageName: String, val scopes: Array<Scope>) : ISignInService.Stub(),
    LifecycleOwner {
    override fun getLifecycle(): Lifecycle = lifecycle

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
        Log.d(TAG, "signIn($request)")
        val account = request?.request?.account
        val result = if (account == null || context.getSystemService<AccountManager>()?.getAccountsByType(account.type)?.contains(account) != true)
            ConnectionResult(ConnectionResult.SIGN_IN_REQUIRED) else ConnectionResult(ConnectionResult.SUCCESS)
        runCatching {
            callbacks?.onSignIn(SignInResponse().apply {
                connectionResult = result
                response = ResolveAccountResponse().apply {
                    connectionResult = result
                    if (account != null) {
                        accountAccessor = object : IAccountAccessor.Stub() {
                            override fun getAccount(): Account {
                                return account
                            }
                        }
                    }
                }
            })
        }
//        fun sendError() {
//            runCatching {
//                callbacks?.onSignIn(SignInResponse().apply {
//                    connectionResult = ConnectionResult(ConnectionResult.INTERNAL_ERROR)
//                    response = ResolveAccountResponse().apply {
//                        connectionResult = ConnectionResult(ConnectionResult.INTERNAL_ERROR)
//                    }
//                })
//            }
//        }
//        Log.d(TAG, "Not yet implemented: signIn $request with $scopes")
//        val account = request?.request?.account ?: return sendError()
//        val authManager = AuthManager(context, account.name, packageName, "oauth2:${scopes.joinToString(" ") { it.scopeUri }}")
//        authManager.setItCaveatTypes("2")
//        if (!authManager.isPermitted && !AuthPrefs.isTrustGooglePermitted(context)) return sendError()
//        lifecycleScope.launchWhenStarted {
//            val authResponse = withContext(Dispatchers.IO) {
//                authManager.requestAuth(true)
//            }
//            if (authResponse.auths == null) return@launchWhenStarted sendError()
//            runCatching {
//                callbacks?.onSignIn(SignInResponse().apply {
//                    connectionResult = ConnectionResult(ConnectionResult.SUCCESS)
//                    response = ResolveAccountResponse().apply {
//                        connectionResult = ConnectionResult(ConnectionResult.SUCCESS)
//                    }
//                })
//            }
//        }
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

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}