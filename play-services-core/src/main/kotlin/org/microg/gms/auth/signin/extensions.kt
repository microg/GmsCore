/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.signin

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Base64
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.auth.AuthManager
import org.microg.gms.auth.ConsentCookiesResponse
import org.microg.gms.auth.ConsentUrlResponse
import org.microg.gms.auth.NonceWrapper
import org.microg.gms.auth.RequestOptions
import org.microg.gms.auth.consent.CONSENT_KEY_COOKIE
import org.microg.gms.auth.consent.CONSENT_MESSENGER
import org.microg.gms.auth.consent.CONSENT_RESULT
import org.microg.gms.auth.consent.CONSENT_URL
import org.microg.gms.auth.consent.ConsentSignInActivity
import org.microg.gms.games.GamesConfigurationService
import org.microg.gms.people.DatabaseHelper
import org.microg.gms.utils.toHexString
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.math.min

private const val TAG = "AuthSignInExtensions"

private val ACCEPTABLE_SCOPES = setOf(Scopes.OPENID, Scopes.EMAIL, Scopes.PROFILE, Scopes.USERINFO_EMAIL, Scopes.USERINFO_PROFILE, Scopes.GAMES_LITE)

private fun Long?.orMaxIfNegative() = this?.takeIf { it >= 0L } ?: Long.MAX_VALUE

val GoogleSignInOptions.scopeUris
    get() = scopes.orEmpty().sortedBy { it.scopeUri }

val GoogleSignInOptions.includeId
    get() = scopeUris.any { it.scopeUri == Scopes.OPENID } || scopeUris.any { it.scopeUri == Scopes.GAMES_LITE }

val GoogleSignInOptions.includeEmail
    get() = scopeUris.any { it.scopeUri == Scopes.EMAIL } || scopeUris.any { it.scopeUri == Scopes.GAMES_LITE }

val GoogleSignInOptions.includeProfile
    get() = scopeUris.any { it.scopeUri == Scopes.PROFILE }

val GoogleSignInOptions.includeUnacceptableScope
    get() = scopeUris.any { it.scopeUri !in ACCEPTABLE_SCOPES }

val GoogleSignInOptions.includeGame
    get() = scopeUris.any { it.scopeUri.contains(Scopes.GAMES) }

val consentRequestOptions: String?
    get() = runCatching {
        val sessionId = Base64.encodeToString(ByteArray(16).also { SecureRandom().nextBytes(it) }, Base64.NO_WRAP).trim()
        val requestOptions = RequestOptions().newBuilder().remote(1).version(3).sessionId(sessionId).build()
        Base64.encodeToString(requestOptions.encode(), Base64.DEFAULT)
    }.getOrNull()

fun getOAuthManager(context: Context, packageName: String, options: GoogleSignInOptions?, account: Account): AuthManager {
    val scopes = options?.scopes.orEmpty().sortedBy { it.scopeUri }
    return AuthManager(context, account.name, packageName, "oauth2:${scopes.joinToString(" ")}")
}

fun getCookiesManager(context: Context, packageName: String, account: Account): AuthManager {
    return AuthManager(context, account.name, packageName, "weblogin:url=https://accounts.google.com")
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
    serverAuthTokenManager.forceRefreshToken = options.isForceCodeForRefreshToken
    serverAuthTokenManager.setOauth2Prompt("auto")
    serverAuthTokenManager.setItCaveatTypes("2")
    return serverAuthTokenManager
}

suspend fun checkAccountAuthStatus(context: Context, packageName: String, scopeList: List<Scope>?, account: Account): Boolean {
    val scopes = scopeList.orEmpty().sortedBy { it.scopeUri }
    val authManager = AuthManager(context, account.name, packageName, "oauth2:${scopes.joinToString(" ")}")
    authManager.ignoreStoredPermission = true
    return withContext(Dispatchers.IO) { authManager.requestAuth(true) }.auth != null
}

suspend fun performSignIn(context: Context, packageName: String, options: GoogleSignInOptions?, account: Account, permitted: Boolean = false, idNonce: String? = null): Pair<String?, GoogleSignInAccount?> {
    val authManager = getOAuthManager(context, packageName, options, account)
    val authResponse = withContext(Dispatchers.IO) {
        if (options?.includeUnacceptableScope == true || !permitted) {
            authManager.setTokenRequestOptions(consentRequestOptions)
        }
        if (permitted) authManager.isPermitted = true
        authManager.requestAuth(true)
    }
    var consentResult:String ?= null
    if ("remote_consent" == authResponse.issueAdvice && authResponse.resolutionDataBase64 != null){
        consentResult = performConsentView(context, packageName, account, authResponse.resolutionDataBase64)
        if (consentResult == null) return Pair(null, null)
    } else {
        if (authResponse.auth == null) return Pair(null, null)
    }
    Log.d(TAG, "id token requested: ${options?.isIdTokenRequested == true}, serverClientId = ${options?.serverClientId}, permitted = ${authManager.isPermitted}")
    val idTokenResponse = getIdTokenManager(context, packageName, options, account)?.let {
        if (idNonce != null) {
            it.setTokenRequestOptions(Base64.encodeToString(RequestOptions.build {
                remote = 1
                version = 6
                nonceWrapper = NonceWrapper.build { nonce = idNonce }
            }.encode(), Base64.DEFAULT))
        }
        it.isPermitted = authResponse.auth != null
        consentResult?.let { result -> it.putDynamicFiled(CONSENT_RESULT, result) }
        withContext(Dispatchers.IO) { it.requestAuth(true) }
    }
    val serverAuthTokenResponse = getServerAuthTokenManager(context, packageName, options, account)?.let {
        it.isPermitted = authResponse.auth != null
        consentResult?.let { result -> it.putDynamicFiled(CONSENT_RESULT, result) }
        withContext(Dispatchers.IO) { it.requestAuth(true) }
    }
    val googleUserId = authManager.getUserData("GoogleUserId")
    val id = if (options?.includeId == true) googleUserId else null
    val tokenId = if (options?.isIdTokenRequested == true) idTokenResponse?.auth else null
    val serverAuthCode: String? = if (options?.isServerAuthCodeRequested == true) serverAuthTokenResponse?.auth else null
    val expirationTime = min(authResponse.expiry.orMaxIfNegative(), idTokenResponse?.expiry.orMaxIfNegative())
    val obfuscatedIdentifier: String = MessageDigest.getInstance("MD5").digest("$googleUserId:$packageName".encodeToByteArray()).toHexString().uppercase()
    val grantedScopeList = authResponse.grantedScopes ?: idTokenResponse?.grantedScopes ?: serverAuthTokenResponse?.grantedScopes
    val grantedScopes = grantedScopeList?.split(" ")?.map { Scope(it) }?.toSet() ?: options?.scopeUris?.toSet() ?: emptySet()
    val (givenName, familyName, displayName, photoUrl) = if (options?.includeProfile == true) {
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
    if (options?.includeGame == true) {
        GamesConfigurationService.setDefaultAccount(context, packageName, account)
    }
    SignInConfigurationService.setAuthInfo(context, packageName, account, options?.toJson())
    val googleSignInAccount = GoogleSignInAccount(
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
    return Pair(authResponse.auth, googleSignInAccount)
}

suspend fun performConsentView(context: Context, packageName: String, account: Account, dataBase64: String): String? {
    Log.d(TAG, "performConsentView: $dataBase64")
    val consentResponse = ConsentUrlResponse.ADAPTER.decode(Base64.decode(dataBase64, Base64.URL_SAFE))
    Log.d(TAG, "performConsentView: consentResponse -> $consentResponse ")
    val response = getCookiesManager(context, packageName, account).let {
        it.isGmsApp = true
        withContext(Dispatchers.IO) { it.requestAuth(true) }
    }
    val cookiesAuth = response.auth ?: return null
    val cookiesResponse = ConsentCookiesResponse.ADAPTER.decode(Base64.decode(cookiesAuth, Base64.URL_SAFE))
    val cookies = arrayListOf(consentResponse.cookie).apply {
        cookiesResponse.consentCookies?.cookies?.filter { ".google.com" == it.domain || "accounts.google.com" == it.path }?.forEach { add(it) }
    }
    return withContext(Dispatchers.IO) {
        val deferred = CompletableDeferred<String?>()
        val intent = Intent(context, ConsentSignInActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(CONSENT_URL, consentResponse.consentUrl)
            putExtra(CONSENT_MESSENGER, Messenger(object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    val content = msg.data.getString(CONSENT_RESULT)
                    Log.d(TAG, "performConsentView: ConsentSignInActivity deferred content: $content")
                    deferred.complete(content)
                }
            }))
            cookies.forEachIndexed { index, cookie ->
                putExtra(CONSENT_KEY_COOKIE + index, "${cookie?.cookieName}=${cookie?.cookieValue};")
            }
        }
        Log.d(TAG, "performConsentView: start ConsentSignInActivity")
        withContext(Dispatchers.Main) { context.startActivity(intent) }
        deferred.await()
    }
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