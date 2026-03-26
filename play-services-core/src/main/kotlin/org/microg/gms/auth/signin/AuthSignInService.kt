/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.microg.gms.auth.signin

import android.accounts.Account
import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.internal.ISignInCallbacks
import com.google.android.gms.auth.api.signin.internal.ISignInService
import com.google.android.gms.common.Feature
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.auth.AuthPrefs
import org.microg.gms.common.AccountUtils
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.games.GAMES_PACKAGE_NAME
import org.microg.gms.games.GamesConfigurationService
import org.microg.gms.utils.singleInstanceOf
import org.microg.gms.utils.warnOnTransactionIssues
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "AuthSignInService"

class AuthSignInService : BaseService(TAG, GmsService.AUTH_GOOGLE_SIGN_IN) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        val binder = AuthSignInServiceImpl(this, lifecycle, packageName, request.account, request.scopes.asList(), request.extras).asBinder()
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, binder, ConnectionInfo().apply {
            features = arrayOf(Feature("user_service_account_management", 1))
        })
    }
}

class AuthSignInServiceImpl(
    private val context: Context,
    override val lifecycle: Lifecycle,
    private val packageName: String,
    private val account: Account?,
    private val scopes: List<Scope>,
    private val extras: Bundle
) : ISignInService.Stub(), LifecycleOwner {
    private val queue = singleInstanceOf { Volley.newRequestQueue(context.applicationContext) }

    override fun silentSignIn(callbacks: ISignInCallbacks, options: GoogleSignInOptions?) {
        Log.d(TAG, "$packageName:silentSignIn($options)")
        fun sendResult(account: GoogleSignInAccount?, status: Status) {
            Log.d(TAG, "Result[$status]: $account")
            runCatching { callbacks.onSignIn(account, status) }
        }
        lifecycleScope.launchWhenStarted {
            try {
                var currentAccount = account ?: options?.account
                if (options?.scopes?.any { it.scopeUri.contains(Scopes.GAMES) } == true) {
                    currentAccount = currentAccount ?: GamesConfigurationService.getDefaultAccount(context, packageName)
                    if (currentAccount == null && GamesConfigurationService.loadPlayedGames(context)?.any { it == packageName } == false) {
                        currentAccount = GamesConfigurationService.getDefaultAccount(context, GAMES_PACKAGE_NAME)
                    }
                    if (currentAccount == null) {
                        sendResult(null, Status(CommonStatusCodes.SIGN_IN_REQUIRED))
                        return@launchWhenStarted
                    }
                }
                val account = currentAccount ?: SignInConfigurationService.getDefaultAccount(context, packageName)
                Log.d(TAG, "silentSignIn: account -> ${account?.name}")
                if (account != null && options?.isForceCodeForRefreshToken != true) {
                    if (getOAuthManager(context, packageName, options, account).isPermitted || AuthPrefs.isTrustGooglePermitted(context)) {
                        val (_, googleSignInAccount) = performSignIn(context, packageName, options, account)
                        if (googleSignInAccount != null) {
                            sendResult(googleSignInAccount, Status(CommonStatusCodes.SUCCESS))
                        } else {
                            sendResult(null, Status(CommonStatusCodes.DEVELOPER_ERROR))
                        }
                    } else {
                        sendResult(null, Status(CommonStatusCodes.SIGN_IN_REQUIRED))
                    }
                } else {
                    sendResult(null, Status(CommonStatusCodes.SIGN_IN_REQUIRED))
                }
            } catch (e: Exception) {
                Log.w(TAG, e)
                sendResult(null, Status.INTERNAL_ERROR)
            }
        }
    }

    override fun signOut(callbacks: ISignInCallbacks, options: GoogleSignInOptions?) {
        Log.d(TAG, "$packageName:signOut($options)")
        lifecycleScope.launchWhenStarted {
            try {
                val account = account ?: options?.account ?: SignInConfigurationService.getDefaultAccount(context, packageName)
                if (account != null) {
                    SignInConfigurationService.getAuthOptions(context, packageName).forEach {
                        Log.d(TAG, "$packageName:signOut authOption:($it)")
                        performSignOut(context, packageName, it, account)
                    }
                }
                if (options?.scopes?.any { it.scopeUri.contains(Scopes.GAMES) } == true) {
                    GamesConfigurationService.setDefaultAccount(context, packageName, null)
                }
                AccountUtils.get(context).removeSelectedAccount(packageName)
                SignInConfigurationService.setAuthInfo(context, packageName, null, null)
                runCatching { callbacks.onSignOut(Status.SUCCESS) }
            } catch (e: Exception) {
                Log.w(TAG, e)
                runCatching { callbacks.onSignIn(null, Status.INTERNAL_ERROR) }
            }
        }
    }

    override fun revokeAccess(callbacks: ISignInCallbacks, options: GoogleSignInOptions?) {
        Log.d(TAG, "$packageName:revokeAccess($options)")
        lifecycleScope.launchWhenStarted {
            val account = account ?: options?.account ?: SignInConfigurationService.getDefaultAccount(context, packageName)
            if (account != null) {
                try {
                    val authManager = getOAuthManager(context, packageName, options, account)
                    val token = authManager.peekAuthToken()
                    if (token != null) {
                        suspendCoroutine { continuation ->
                            queue.add(object : JsonObjectRequest(
                                "https://accounts.google.com/o/oauth2/revoke?token=$token",
                                { continuation.resume(it) },
                                { continuation.resumeWithException(it) }) {
                                override fun getHeaders(): MutableMap<String, String> {
                                    return hashMapOf(
                                        "Authorization" to "OAuth $token"
                                    )
                                }
                            })
                        }
                        authManager.invalidateAuthToken(token)
                        authManager.isPermitted = false
                    }
                    SignInConfigurationService.setAuthInfo(context, packageName, account, options?.toJson())
                    runCatching { callbacks.onRevokeAccess(Status.SUCCESS) }
                } catch (e: Exception) {
                    Log.w(TAG, e)
                    runCatching { callbacks.onRevokeAccess(Status.INTERNAL_ERROR) }
                }
            } else {
                runCatching { callbacks.onRevokeAccess(Status.SUCCESS) }
            }
        }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}