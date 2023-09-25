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

val GoogleSignInOptions.scopeUris
    get() = scopes.orEmpty().sortedBy { it.scopeUri }

val GoogleSignInOptions.includeId
    get() = scopeUris.any { it.scopeUri == Scopes.OPENID } || scopeUris.any { it.scopeUri == Scopes.GAMES_LITE }

val GoogleSignInOptions.includeEmail
    get() = scopeUris.any { it.scopeUri == Scopes.EMAIL } || scopeUris.any { it.scopeUri == Scopes.GAMES_LITE }

val GoogleSignInOptions.includeProfile
    get() = scopeUris.any { it.scopeUri == Scopes.PROFILE }

fun getOAuthManager(context: Context, packageName: String, options: GoogleSignInOptions?, account: Account): AuthManager {
    val scopes = options?.scopes.orEmpty().sortedBy { it.scopeUri }
    return AuthManager(context, account.name, packageName, "oauth2:${scopes.joinToString(" ")}")
}

fun getIdTokenManager(context: Context, packageName: String, options: GoogleSignInOptions?, account: Account): AuthManager? {
    if (options?.isIdTokenRequested != true || options.serverClientId == null) return null

    val idTokenManager = AuthManager(context, account.name, packageName, "audience:server:client_id:${options.serverClientId}")
    idTokenManager.includeEmail = if (options.includeEmail) "1" else "0"
    idTokenManager.includeProfile = if (options.includeProfile) "1" else "0"
    return idTokenManager
}

fun getServerAuthTokenManager(context: Context, packageName: String, options: GoogleSignInOptions?, account: Account): AuthManager? {
    if (options?.isServerAuthCodeRequested != true || options.serverClientId == null) return null

    val serverAuthTokenManager = AuthManager(context, account.name, packageName, "oauth2:server:client_id:${options.serverClientId}:api_scope:${options.scopeUris.joinToString(" ")}")
    serverAuthTokenManager.includeEmail = if (options.includeEmail) "1" else "0"
    serverAuthTokenManager.includeProfile = if (options.includeProfile) "1" else "0"
    serverAuthTokenManager.setOauth2Prompt(if (options.isForceCodeForRefreshToken) "consent" else "auto")
    serverAuthTokenManager.setItCaveatTypes("2")
    return serverAuthTokenManager
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
    val idTokenResponse = getIdTokenManager(context, packageName, options, account)?.let {
        it.isPermitted = authManager.isPermitted
        withContext(Dispatchers.IO) { it.requestAuth(true) }
    }
    val serverAuthTokenResponse = getServerAuthTokenManager(context, packageName, options, account)?.let {
        it.isPermitted = authManager.isPermitted
        withContext(Dispatchers.IO) { it.requestAuth(true) }
    }
    val googleUserId = authManager.getUserData("GoogleUserId")
    val id = if (includeId) googleUserId else null
    val tokenId = if (options?.isIdTokenRequested == true) idTokenResponse?.auth else null
    val serverAuthCode: String? = if (options?.isServerAuthCodeRequested == true) serverAuthTokenResponse?.auth else null
    val expirationTime = min(authResponse.expiry.orMaxIfNegative(), idTokenResponse?.expiry.orMaxIfNegative())
    val obfuscatedIdentifier: String = MessageDigest.getInstance("MD5").digest("$googleUserId:$packageName".encodeToByteArray()).toHexString().uppercase()
    val grantedScopes = authResponse.grantedScopes?.split(" ").orEmpty().map { Scope(it) }.toSet()
    val (givenName, familyName, displayName, photoUrl) = if (includeProfile) {
        val databaseHelper = DatabaseHelper(context)
        val cursor = databaseHelper.getOwner(account.name)
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
            databaseHelper.close()
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

fun performSignOut(context: Context, packageName: String, options: GoogleSignInOptions?, account: Account) {
    val authManager = getOAuthManager(context, packageName, options, account)
    authManager.isPermitted = false
    authManager.invalidateAuthToken()

    getIdTokenManager(context, packageName, options, account)?.let {
        it.isPermitted = false
        it.invalidateAuthToken()
    }
    getServerAuthTokenManager(context, packageName, options, account)?.let {
        it.isPermitted = false
        it.invalidateAuthToken()
    }
}