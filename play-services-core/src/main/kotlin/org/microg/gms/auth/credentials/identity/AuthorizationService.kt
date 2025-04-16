/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.credentials.identity

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.VerifyWithGoogleRequest
import com.google.android.gms.auth.api.identity.VerifyWithGoogleResult
import com.google.android.gms.auth.api.identity.internal.IAuthorizationCallback
import com.google.android.gms.auth.api.identity.internal.IAuthorizationService
import com.google.android.gms.auth.api.identity.internal.IVerifyWithGoogleCallback
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.BaseService
import org.microg.gms.auth.credentials.FEATURES
import org.microg.gms.auth.signin.SignInConfigurationService
import org.microg.gms.auth.signin.getServerAuthTokenManager
import org.microg.gms.auth.signin.performSignIn
import org.microg.gms.auth.signin.scopeUris
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils

private const val TAG = "AuthorizationService"

class AuthorizationService : BaseService(TAG, GmsService.AUTHORIZATION) {

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        Log.d(TAG, "handleServiceRequest start ")
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
                ?: throw IllegalArgumentException("Missing package name")
        val connectionInfo = ConnectionInfo()
        connectionInfo.features = FEATURES
        callback.onPostInitCompleteWithConnectionInfo(
            ConnectionResult.SUCCESS, AuthorizationServiceImpl(this, packageName, this.lifecycle).asBinder(), connectionInfo
        )
    }
}

class AuthorizationServiceImpl(val context: Context, val packageName: String, override val lifecycle: Lifecycle) : IAuthorizationService.Stub(), LifecycleOwner {

    override fun authorize(callback: IAuthorizationCallback?, request: AuthorizationRequest?) {
        Log.d(TAG, "Method: authorize called, request:$request")
        lifecycleScope.launchWhenStarted {
            val account = request?.account ?: SignInConfigurationService.getDefaultAccount(context, packageName)
            if (account == null) {
                Log.d(TAG, "Method: authorize called, but account is null")
                callback?.onAuthorized(Status.CANCELED, null)
                return@launchWhenStarted
            }
            val googleSignInOptions = GoogleSignInOptions.Builder().apply {
                request?.requestedScopes?.forEach { requestScopes(it) }
                if (request?.idTokenRequested == true && request.serverClientId != null) requestIdToken(request.serverClientId)
                if (request?.serverAuthCodeRequested == true && request.serverClientId != null) requestServerAuthCode(request.serverClientId)
            }.build()
            val signInAccount = performSignIn(context, packageName, googleSignInOptions, account, false)
            callback?.onAuthorized(Status.SUCCESS, AuthorizationResult().apply {
                serverAuthToken = signInAccount?.serverAuthCode
                idToken = signInAccount?.idToken
                grantedScopes = signInAccount?.grantedScopes?.toList()
                googleSignInAccount = signInAccount
            }.also { Log.d(TAG, "authorize: result:$it") })
        }
    }

    override fun verifyWithGoogle(callback: IVerifyWithGoogleCallback?, request: VerifyWithGoogleRequest?) {
        Log.d(TAG, "unimplemented Method: verifyWithGoogle: request:$request")
        lifecycleScope.launchWhenStarted {
            val account = SignInConfigurationService.getDefaultAccount(context, packageName)
            if (account == null) {
                Log.d(TAG, "Method: authorize called, but account is null")
                callback?.onVerifed(Status.CANCELED, null)
                return@launchWhenStarted
            }
            if (request?.offlineAccess == true && request.serverClientId != null) {
                val googleSignInOptions = GoogleSignInOptions.Builder().apply {
                    request.requestedScopes?.forEach { requestScopes(it) }
                    requestServerAuthCode(request.serverClientId)
                }.build()
                val authResponse = getServerAuthTokenManager(context, packageName, googleSignInOptions, account)?.let {
                    withContext(Dispatchers.IO) { it.requestAuth(true) }
                }
                callback?.onVerifed(Status.SUCCESS, VerifyWithGoogleResult().apply {
                    serverAuthToken = authResponse?.auth
                    grantedScopes = authResponse?.grantedScopes?.split(" ")?.map { Scope(it) }?.toList() ?: googleSignInOptions.scopeUris.toList()
                })
                return@launchWhenStarted
            }
            callback?.onVerifed(Status.CANCELED, null)
        }
    }

}