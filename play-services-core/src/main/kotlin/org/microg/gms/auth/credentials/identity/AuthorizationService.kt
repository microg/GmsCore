/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.credentials.identity

import android.accounts.AccountManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.auth.api.identity.VerifyWithGoogleRequest
import com.google.android.gms.auth.api.identity.VerifyWithGoogleResult
import com.google.android.gms.auth.api.identity.internal.IAuthorizationCallback
import com.google.android.gms.auth.api.identity.internal.IAuthorizationService
import com.google.android.gms.auth.api.identity.internal.IVerifyWithGoogleCallback
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.internal.SignInConfiguration
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.BaseService
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.credentials.FEATURES
import org.microg.gms.auth.signin.AuthSignInActivity
import org.microg.gms.auth.signin.SignInConfigurationService
import org.microg.gms.auth.signin.getOAuthManager
import org.microg.gms.auth.signin.getServerAuthTokenManager
import org.microg.gms.auth.signin.performSignIn
import org.microg.gms.auth.signin.scopeUris
import org.microg.gms.common.AccountUtils
import org.microg.gms.common.Constants
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "AuthorizationService"

class AuthorizationService : BaseService(TAG, GmsService.AUTH_API_IDENTITY_AUTHORIZATION) {

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

    companion object{
        private val nextRequestCode = AtomicInteger(0)
    }

    override fun authorize(callback: IAuthorizationCallback?, request: AuthorizationRequest?) {
        Log.d(TAG, "Method: authorize called, packageName:$packageName request:$request")
        lifecycleScope.launchWhenStarted {
            val requestAccount = request?.account
            val account = requestAccount ?: AccountUtils.get(context).getSelectedAccount(packageName)
            val googleSignInOptions = GoogleSignInOptions.Builder().apply {
                request?.requestedScopes?.forEach { requestScopes(it) }
                if (request?.idTokenRequested == true && request.serverClientId != null) {
                    if (account?.name != requestAccount?.name) {
                        requestEmail().requestProfile()
                    }
                    requestIdToken(request.serverClientId)
                }
                if (request?.serverAuthCodeRequested == true && request.serverClientId != null) requestServerAuthCode(request.serverClientId, request.forceCodeForRefreshToken)
            }.build()
            Log.d(TAG, "authorize: account: ${account?.name}")
            val result = if (account != null) {
                val (accessToken, signInAccount) = performSignIn(context, packageName, googleSignInOptions, account, false)
                if (requestAccount != null) {
                    AccountUtils.get(context).saveSelectedAccount(packageName, requestAccount)
                }
                AuthorizationResult(
                    signInAccount?.serverAuthCode,
                    accessToken,
                    signInAccount?.idToken,
                    signInAccount?.grantedScopes?.toList().orEmpty().map { it.scopeUri },
                    signInAccount,
                    null
                )
            } else {
                val options = GoogleSignInOptions.Builder(googleSignInOptions).apply {
                    val defaultAccount = SignInConfigurationService.getDefaultAccount(context, packageName)
                    defaultAccount?.name?.let { setAccountName(it) }
                }.build()
                val intent = Intent(context, AuthSignInActivity::class.java).apply {
                    `package` = Constants.GMS_PACKAGE_NAME
                    putExtra("config", SignInConfiguration(packageName, options))
                }
                AuthorizationResult(
                    null,
                    null,
                    null,
                    request?.requestedScopes.orEmpty().map { it.scopeUri },
                    null,
                    PendingIntent.getActivity(context, nextRequestCode.incrementAndGet(), intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
                )
            }
            runCatching {
                callback?.onAuthorized(Status.SUCCESS, result.also { Log.d(TAG, "authorize: result:$it") })
            }
        }
    }

    override fun verifyWithGoogle(callback: IVerifyWithGoogleCallback?, request: VerifyWithGoogleRequest?) {
        Log.d(TAG, "unimplemented Method: verifyWithGoogle: request:$request")
        lifecycleScope.launchWhenStarted {
            val account = AccountUtils.get(context).getSelectedAccount(packageName) ?: SignInConfigurationService.getDefaultAccount(context, packageName)
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

    override fun revokeAccess(callback: IStatusCallback?, request: RevokeAccessRequest?) {
        Log.d(TAG, "Method: revokeAccess called, request:$request")
        lifecycleScope.launchWhenStarted {
            val authOptions = SignInConfigurationService.getAuthOptions(context, packageName)
            val authAccount = request?.account
            if (authOptions.isNotEmpty() && authAccount != null) {
                val authManager = getOAuthManager(context, packageName, authOptions.first(), authAccount)
                val token = authManager.peekAuthToken()
                if (token != null) {
                    // todo "https://oauth2.googleapis.com/revoke"
                    authManager.invalidateAuthToken(token)
                    authManager.isPermitted = false
                }
            }
            AccountUtils.get(context).removeSelectedAccount(packageName)
            runCatching { callback?.onResult(Status.SUCCESS) }
        }
    }

    override fun clearToken(callback: IStatusCallback?, request: ClearTokenRequest?) {
        Log.d(TAG, "Method: clearToken called, request:$request")
        request?.token?.let {
            AccountManager.get(context).invalidateAuthToken(AuthConstants.DEFAULT_ACCOUNT_TYPE, it)
        }
        runCatching { callback?.onResult(Status.SUCCESS) }
    }

}