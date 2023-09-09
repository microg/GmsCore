/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.signin

import android.accounts.Account
import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.auth.AuthManager
import org.microg.gms.people.DatabaseHelper
import org.microg.gms.utils.toHexString
import java.security.MessageDigest
import kotlin.math.min

private fun Long?.orMaxIfNegative() = this?.takeIf { it >= 0L } ?: Long.MAX_VALUE

fun getOAuthManager(context: Context, packageName: String, options: GoogleSignInOptions?, account: Account): AuthManager {
    val scopes = options?.scopes.orEmpty().sortedBy { it.scopeUri }
    return AuthManager(context, account.name, packageName, "oauth2:${scopes.joinToString(" ")}")
}

suspend fun performSignIn(context: Context, packageName: String, options: GoogleSignInOptions?, account: Account, permitted: Boolean = false): GoogleSignInAccount? {
    val authManager = getOAuthManager(context, packageName, options, account)
    if (permitted) authManager.isPermitted = true
    val authResponse = withContext(Dispatchers.IO) {
        authManager.requestAuth(true)
    }
    if (authResponse.auth == null) return null

    val scopes = options?.scopes.orEmpty().sortedBy { it.scopeUri }
    val includeId = scopes.any { it.scopeUri == Scopes.OPENID } || scopes.any { it.scopeUri == Scopes.GAMES_LITE }
    val includeEmail = scopes.any { it.scopeUri == Scopes.EMAIL } || scopes.any { it.scopeUri == Scopes.GAMES_LITE }
    val includeProfile = scopes.any { it.scopeUri == Scopes.PROFILE }
    Log.d("AuthSignIn", "id token requested: ${options?.isIdTokenRequested == true}, serverClientId = ${options?.serverClientId}")
    val idTokenResponse = if (options?.isIdTokenRequested == true && options.serverClientId != null) withContext(Dispatchers.IO) {
        val idTokenManager = AuthManager(context, account.name, packageName, "audience:server:client_id:${options.serverClientId}")
        idTokenManager.includeEmail = if (includeEmail) "1" else "0"
        idTokenManager.includeProfile = if (includeProfile) "1" else "0"
        idTokenManager.isPermitted = authManager.isPermitted
        idTokenManager.requestAuth(true)
    } else null
    val serverAuthTokenResponse = if (options?.isServerAuthCodeRequested == true && options.serverClientId != null) withContext(Dispatchers.IO) {
        val serverAuthTokenManager = AuthManager(context, account.name, packageName, "oauth2:server:client_id:${options.serverClientId}:api_scope:${scopes.joinToString(" ")}")
        serverAuthTokenManager.includeEmail = if (includeEmail) "1" else "0"
        serverAuthTokenManager.includeProfile = if (includeProfile) "1" else "0"
        serverAuthTokenManager.setOauth2Prompt(if (options.isForceCodeForRefreshToken) "consent" else "auto")
        serverAuthTokenManager.setItCaveatTypes("2")
        serverAuthTokenManager.isPermitted = authManager.isPermitted
        serverAuthTokenManager.requestAuth(true)
    } else null
    val googleUserId = authManager.getUserData("GoogleUserId")
    val id = if (includeId) googleUserId else null
    val tokenId = if (options?.isIdTokenRequested == true) idTokenResponse?.auth else null
    val serverAuthCode: String? = if (options?.isServerAuthCodeRequested == true) serverAuthTokenResponse?.auth else null
    val expirationTime = min(authResponse.expiry.orMaxIfNegative(), idTokenResponse?.expiry.orMaxIfNegative())
    val obfuscatedIdentifier: String = MessageDigest.getInstance("MD5").digest("$googleUserId:$packageName".encodeToByteArray()).toHexString().uppercase()
    val grantedScopes = authResponse.grantedScopes?.split(" ").orEmpty().map { Scope(it) }.toSet()
    val (givenName, familyName, displayName, photoUrl) = if (includeProfile) {
        val cursor = DatabaseHelper(context).getOwner(account.name)
        try {
            if (cursor.moveToNext()) {
                listOf(
                    cursor.getColumnIndex("given_name").takeIf { it >= 0 }?.let { cursor.getString(it) },
                    cursor.getColumnIndex("family_name").takeIf { it >= 0 }?.let { cursor.getString(it) },
                    cursor.getColumnIndex("display_name").takeIf { it >= 0 }?.let { cursor.getString(it) },
                    cursor.getColumnIndex("avatar").takeIf { it >= 0 }?.let { cursor.getString(it) },
                )
            } else listOf(null, null, null, null)
        } finally {
            cursor.close()
        }
    } else listOf(null, null, null, null)
    SignInConfigurationService.setDefaultAccount(context, packageName, account)
    return GoogleSignInAccount(
        id,
        tokenId,
        account.name,
        displayName,
        photoUrl?.let { Uri.parse(it) },
        serverAuthCode,
        expirationTime,
        obfuscatedIdentifier,
        grantedScopes,
        givenName,
        familyName
    )
}