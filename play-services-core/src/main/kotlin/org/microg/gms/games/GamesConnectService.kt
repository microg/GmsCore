/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Parcel
import android.util.Log
import androidx.core.app.PendingIntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.games.internal.connect.GamesSignInRequest
import com.google.android.gms.games.internal.connect.GamesSignInResponse
import com.google.android.gms.games.internal.connect.IGamesConnectCallbacks
import com.google.android.gms.games.internal.connect.IGamesConnectService
import org.microg.gms.BaseService
import org.microg.gms.auth.AuthManager
import org.microg.gms.auth.AuthPrefs
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues
import java.util.UUID

private const val TAG = "GamesConnectService"

class GamesConnectService : BaseService(TAG, GmsService.GAMES) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS,
            GamesConnectServiceImpl(this, lifecycle, packageName),
            ConnectionInfo()
        )
    }
}

class GamesConnectServiceImpl(val context: Context, override val lifecycle: Lifecycle, val packageName: String) : IGamesConnectService.Stub(), LifecycleOwner {

    override fun signIn(callback: IGamesConnectCallbacks?, request: GamesSignInRequest?) {
        Log.d(TAG, "signIn($request)")
        fun sendSignInRequired() {
            val resolution = PendingIntentCompat.getActivity(context, packageName.hashCode(), Intent(context, GamesSignInActivity::class.java).apply {
                putExtra(EXTRA_GAME_PACKAGE_NAME, packageName)
                putExtra(EXTRA_SCOPES, arrayOf(Scopes.GAMES_LITE))
            }, PendingIntent.FLAG_UPDATE_CURRENT, false)
            when (request?.signInType) {
                0 -> { // Manual sign in, provide resolution
                    callback?.onSignIn(Status(CommonStatusCodes.SIGN_IN_REQUIRED, null, resolution), null)
                }

                1 -> { // Auto sign-in on start, don't provide resolution if not
                    callback?.onSignIn(Status(CommonStatusCodes.SIGN_IN_REQUIRED), null)
                }

                else -> {
                    callback?.onSignIn(Status(CommonStatusCodes.SIGN_IN_REQUIRED), null)
                }
            }
        }
        lifecycleScope.launchWhenStarted {
            try {
                val account = request?.previousStepResolutionResult?.resultData?.getParcelableExtra<Account>(EXTRA_ACCOUNT)
                    ?: GamesConfigurationService.getDefaultAccount(context, packageName)
                    ?: return@launchWhenStarted sendSignInRequired()
                val authManager = AuthManager(context, account.name, packageName, "oauth2:${Scopes.GAMES_LITE}")
                if (!authManager.isPermitted && !AuthPrefs.isTrustGooglePermitted(context)) return@launchWhenStarted sendSignInRequired()
                val result = performGamesSignIn(context, packageName, account)
                if (result) {
                    callback?.onSignIn(Status.SUCCESS, GamesSignInResponse().apply { gameRunToken = UUID.randomUUID().toString() })
                } else {
                    sendSignInRequired()
                }
            } catch (e: Exception) {
                Log.w(TAG, e)
                return@launchWhenStarted sendSignInRequired()
            }
        }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}