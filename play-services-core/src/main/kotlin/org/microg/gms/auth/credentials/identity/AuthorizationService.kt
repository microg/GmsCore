/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.credentials.identity

import android.accounts.Account
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
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.microg.gms.BaseService
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.credentials.FEATURES
import org.microg.gms.auth.signin.AuthSignInActivity
import org.microg.gms.auth.signin.SignInConfigurationService
import org.microg.gms.auth.signin.checkAccountAuthStatus
import org.microg.gms.auth.signin.getOAuthManager
import org.microg.gms.auth.signin.getServerAuthTokenManager
import org.microg.gms.auth.signin.performSignIn
import org.microg.gms.auth.signin.scopeUris
import org.microg.gms.common.AccountUtils
import org.microg.gms.common.Constants
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "AuthorizationService"
private const val REVOKE_ENDPOINT = "https://oauth2.googleapis.com/revoke"

class AuthorizationService : BaseService(TAG, GmsService.AUTH_API_IDENTITY_AUTHORIZATION) {

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        Log.d(TAG, "handleServiceRequest start ")
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName) ?: throw IllegalArgumentException("Missing package name")
        val connectionInfo = ConnectionInfo().apply { features = FEATURES }
        callback.onPostInitCompleteWithConnectionInfo(
            ConnectionResult.SUCCESS, AuthorizationServiceImpl(this, packageName, this.lifecycle).asBinder(), connectionInfo
        )
    }
}

class AuthorizationServiceImpl(val context: Context, val packageName: String, override val lifecycle: Lifecycle) : IAuthorizationService.Stub(), LifecycleOwner {

    companion object {
        private val nextRequestCode = AtomicInteger(0)
        private val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()
        }
    }

    override fun authorize(callback: IAuthorizationCallback?, request: AuthorizationRequest?) {
        Log.d(TAG, "authorize called, packageName=$packageName request=$request")
        lifecycleScope.launchWhenStarted {
            try {
                val result = performAuthorize(request)
                Log.d(TAG, "authorize resolved: ${if (result.pendingIntent != null) "pendingIntent" else "silent"}, grantedScopes=${result.grantedScopes.size}")
                runCatching { callback?.onAuthorized(Status.SUCCESS, result) }
            } catch (e: InvalidAccountException) {
                Log.w(TAG, "authorize: invalid account", e)
                runCatching { callback?.onAuthorized(Status(CommonStatusCodes.INVALID_ACCOUNT), null) }
            } catch (e: Exception) {
                Log.w(TAG, "authorize failed, falling back to PendingIntent", e)
                runCatching { callback?.onAuthorized(Status.SUCCESS, buildPendingIntentResult(request)) }
            }
        }
    }

    private suspend fun performAuthorize(request: AuthorizationRequest?): AuthorizationResult {
        require(request?.requestedScopes?.isNotEmpty() == true) { "requestedScopes cannot be null or empty" }

        val requestAccount = request!!.account
        val candidate = requestAccount ?: AccountUtils.get(context).getSelectedAccount(packageName) ?: SignInConfigurationService.getDefaultAccount(context, packageName)
        if (candidate == null || request.forceCodeForRefreshToken) {
            return buildPendingIntentResult(request)
        }

        val account = AccountManager.get(context).getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE).firstOrNull { it == candidate } ?: run {
            AccountUtils.get(context).removeSelectedAccount(packageName)
            return buildPendingIntentResult(request)
        }

        val hostedDomain = request.hostedDomainFilter
        if (!hostedDomain.isNullOrEmpty() && !account.name.lowercase(Locale.ROOT).endsWith("@${hostedDomain.lowercase(Locale.ROOT)}")) {
            throw InvalidAccountException("account ${account.name} does not match hostedDomainFilter=$hostedDomain")
        }

        val crossAccount = requestAccount != null && account.name != requestAccount.name
        val options = buildSignInOptions(request, crossAccount)
        val (accessToken, signInAccount) = performSignIn(context, packageName, options, account, false)
        if (accessToken == null || signInAccount == null) {
            return buildPendingIntentResult(request)
        }

        if (requestAccount != null) {
            AccountUtils.get(context).saveSelectedAccount(packageName, requestAccount)
        }

        return AuthorizationResult(
            signInAccount.serverAuthCode,
            accessToken,
            signInAccount.idToken,
            signInAccount.grantedScopes.toList().map { it.scopeUri },
            signInAccount,
            null,
        )
    }

    private fun buildSignInOptions(request: AuthorizationRequest, crossAccount: Boolean): GoogleSignInOptions {
        return GoogleSignInOptions.Builder().apply {
            request.requestedScopes?.forEach { requestScopes(it) }
            val clientId = request.serverClientId
            if (request.idTokenRequested && clientId != null) {
                if (crossAccount) requestEmail().requestProfile()
                requestIdToken(clientId)
            }
            if (request.serverAuthCodeRequested && clientId != null) {
                requestServerAuthCode(clientId, request.forceCodeForRefreshToken)
            }
        }.build()
    }

    private suspend fun buildPendingIntentResult(request: AuthorizationRequest?): AuthorizationResult {
        val defaultAccountName = SignInConfigurationService.getDefaultAccount(context, packageName)?.name
        val options = GoogleSignInOptions.Builder().apply {
            request?.requestedScopes?.forEach { requestScopes(it) }
            val clientId = request?.serverClientId
            if (request?.idTokenRequested == true && clientId != null) {
                requestEmail().requestProfile().requestIdToken(clientId)
            }
            if (request?.serverAuthCodeRequested == true && clientId != null) {
                requestServerAuthCode(clientId, request.forceCodeForRefreshToken)
            }
            defaultAccountName?.let { setAccountName(it) }
        }.build()
        val intent = Intent(context, AuthSignInActivity::class.java).apply {
            `package` = Constants.GMS_PACKAGE_NAME
            putExtra("config", SignInConfiguration(packageName, options))
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            nextRequestCode.incrementAndGet(),
            intent,
            FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE,
        )
        return AuthorizationResult(
            null, null, null,
            request?.requestedScopes.orEmpty().map { it.scopeUri },
            null,
            pendingIntent,
        )
    }

    override fun verifyWithGoogle(callback: IVerifyWithGoogleCallback?, request: VerifyWithGoogleRequest?) {
        Log.d(TAG, "verifyWithGoogle called, request=$request")
        lifecycleScope.launchWhenStarted {
            val result = runCatching { performVerify(request) }.onFailure { Log.w(TAG, "verifyWithGoogle failed", it) }.getOrNull()
            val status = if (result != null) Status.SUCCESS else Status.CANCELED
            runCatching { callback?.onVerifed(status, result) }
        }
    }

    private suspend fun performVerify(request: VerifyWithGoogleRequest?): VerifyWithGoogleResult? {
        val req = request?.takeIf { it.requestedScopes?.isNotEmpty() == true } ?: return null
        val account = AccountUtils.get(context).getSelectedAccount(packageName) ?: SignInConfigurationService.getDefaultAccount(context, packageName) ?: return null

        val options = GoogleSignInOptions.Builder().apply {
            req.requestedScopes?.forEach { requestScopes(it) }
            if (req.offlineAccess && req.serverClientId != null) {
                requestServerAuthCode(req.serverClientId)
            }
        }.build()

        if (req.offlineAccess && req.serverClientId != null) {
            val authResponse = getServerAuthTokenManager(context, packageName, options, account)?.let {
                withContext(Dispatchers.IO) { it.requestAuth(true) }
            } ?: return null
            if (authResponse.auth == null) return null
            return VerifyWithGoogleResult().apply {
                serverAuthToken = authResponse.auth
                grantedScopes = authResponse.grantedScopes?.split(" ")?.map { Scope(it) } ?: options.scopeUris.toList()
            }
        }

        val granted = checkAccountAuthStatus(context, packageName, options.scopes.toList(), account)
        if (!granted) return null
        return VerifyWithGoogleResult().apply { grantedScopes = options.scopeUris.toList() }
    }

    override fun revokeAccess(callback: IStatusCallback?, request: RevokeAccessRequest?) {
        Log.d(TAG, "revokeAccess called, request=$request")
        lifecycleScope.launchWhenStarted {
            runCatching { performRevoke(request) }.onFailure { Log.w(TAG, "revokeAccess failed", it) }
            runCatching { callback?.onResult(Status.SUCCESS) }
        }
    }

    private suspend fun performRevoke(request: RevokeAccessRequest?) {
        val account: Account? = request?.account
            ?: AccountUtils.get(context).getSelectedAccount(packageName)
            ?: SignInConfigurationService.getDefaultAccount(context, packageName)

        if (account != null) {
            val authOptions = SignInConfigurationService.getAuthOptions(context, packageName)
            for (options in authOptions) {
                val authManager = getOAuthManager(context, packageName, options, account)
                val token = authManager.peekAuthToken() ?: continue
                runCatching { revokeTokenRemotely(token) }.onFailure { Log.w(TAG, "remote revoke failed (continuing local invalidate)", it) }
                authManager.invalidateAuthToken(token)
                authManager.isPermitted = false
            }
        }

        AccountUtils.get(context).removeSelectedAccount(packageName)
        SignInConfigurationService.setAuthInfo(context, packageName, null, null)
    }

    private suspend fun revokeTokenRemotely(token: String) {
        withContext(Dispatchers.IO) {
            val body = FormBody.Builder().add("token", token).build()
            val request = Request.Builder().url(REVOKE_ENDPOINT).post(body).build()
            httpClient.newCall(request).execute().use { response ->
                Log.d(TAG, "revoke endpoint status=${response.code}")
            }
        }
    }

    override fun clearToken(callback: IStatusCallback?, request: ClearTokenRequest?) {
        Log.d(TAG, "clearToken called, request=$request")
        lifecycleScope.launchWhenStarted {
            runCatching {
                request?.token?.takeIf { it.isNotEmpty() }?.let {
                    AccountManager.get(context).invalidateAuthToken(AuthConstants.DEFAULT_ACCOUNT_TYPE, it)
                }
            }.onFailure { Log.w(TAG, "clearToken failed", it) }
            runCatching { callback?.onResult(Status.SUCCESS) }
        }
    }

    private class InvalidAccountException(message: String) : Exception(message)
}