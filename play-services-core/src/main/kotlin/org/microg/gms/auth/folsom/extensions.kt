/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.folsom

import android.accounts.AccountManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.core.app.PendingIntentCompat
import com.google.android.gms.common.api.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.folsom.ui.GenericActivity
import org.microg.gms.auth.folsom.ui.GenericActivity.Companion.EXTRA_ACCOUNT_NAME
import org.microg.gms.auth.folsom.ui.GenericActivity.Companion.EXTRA_OFFER_RESET
import org.microg.gms.auth.folsom.ui.GenericActivity.Companion.EXTRA_OPERATION
import org.microg.gms.auth.folsom.ui.GenericActivity.Companion.EXTRA_SECURITY_DOMAIN
import org.microg.gms.auth.folsom.ui.GenericActivity.Companion.EXTRA_SESSION_ID
import org.microg.gms.utils.createGrpcClient

const val ERROR_CODE_NO_KEYS = 38500
const val ERROR_CODE_SECURITY_DOMAIN_NOT_SET = 38501
const val SECURITY_WEB_BASE_URL = "https://accounts.google.com/encryption/unlock/android"
const val SECURITY_DOMAIN_BASE_URL = "https://securitydomain-pa.googleapis.com/"
const val SERVICE_SECURITY_DOMAIN_SCOPE = "oauth2:https://www.googleapis.com/auth/cryptauth"
const val USERS = "users/me"
const val MEMBERS = "/members/"
const val SECURITY_DOMAINS = "/securitydomains/"

enum class DomainStatus(val code: Int) {
    /** Unknown error occurred */
    UNKNOWN_ERROR(1),

    /** Domain state is unknown */
    UNKNOWN(2),

    /** Domain is not recoverable */
    NOT_RECOVERABLE(3),

    /** Recovery is pending */
    PENDING_RECOVERY(4),

    /** Domain is recoverable */
    RECOVERABLE(5),

    /** Recovery is in progress */
    RECOVERY_IN_PROGRESS(6),

    /** No keys available in domain */
    NO_KEYS(7);
}

fun buildKeyDeliveryInfo(sessionId: String, securityDomain: String, offerReset: Boolean): ByteArray =
    KeyDeliveryInfo(
        operationType = KeyDeliveryOperationType.START_KEY_RETRIEVAL,
        keyRetrieval = StartKeyRetrievalRequest(domain = securityDomain, reset = offerReset),
        sessionId = sessionId
    ).encode()

fun serializeDomainStateResponse(state: Int): ByteArray {
    if (state < 128) {
        return byteArrayOf(0x08, state.toByte())
    }
    val result = mutableListOf<Byte>(0x08)
    var v = state
    while (v >= 128) {
        result.add(((v and 0x7F) or 0x80).toByte())
        v = v ushr 7
    }
    result.add(v.toByte())
    return result.toByteArray()
}

fun Context.buildKeyRetrievalStatus(
    accountName: String,
    domainId: String,
    operationType: Int,
    sessionId: String,
    offerReset: Boolean,
    block: (PendingIntent?) -> Status
): Status {
    val intent = Intent(this, GenericActivity::class.java).apply {
        putExtra(EXTRA_ACCOUNT_NAME, accountName)
        putExtra(EXTRA_SECURITY_DOMAIN, domainId)
        putExtra(EXTRA_OPERATION, operationType)
        putExtra(EXTRA_SESSION_ID, sessionId)
        putExtra(EXTRA_OFFER_RESET, offerReset)
    }
    return block(PendingIntentCompat.getActivity(this, 0, intent, FLAG_UPDATE_CURRENT, false))
}

fun computeMemberName(publicKeyBytes: ByteArray): String {
    val encoded = Base64.encodeToString(publicKeyBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    return "$USERS$MEMBERS$encoded"
}

fun Context.requestOauthToken(accountName: String, scope: String = SERVICE_SECURITY_DOMAIN_SCOPE): String {
    val accountManager = AccountManager.get(this)
    val account = accountManager.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE).find {
        it.name == accountName
    }
    if (account == null) throw RuntimeException("account is null")
    return accountManager.blockingGetAuthToken(account, scope, true)
        ?: throw RuntimeException("oauthToken is null")
}

suspend fun loadSecurityDomainMembers(context: Context, accountName: String, sessionId: String, domainId: String) =
    withContext(Dispatchers.IO) {
        createGrpcClient<SecurityDomainServiceClient>(SECURITY_DOMAIN_BASE_URL, context.requestOauthToken(accountName))
            .ListSecurityDomainMembers()
            .executeBlocking(
                ListSecurityDomainMembersRequest(
                    parent = USERS,
                    view = emptyList(),
                    filter = listOf(USERS + SECURITY_DOMAINS + domainId),
                    pageSize = 3,
                    pageToken = "",
                    filterOptions = FilterOptions(includeDeleted = true),
                    requestId = sessionId
                )
            )
    }

suspend fun getSecurityDomain(context: Context, accountName: String, sessionId: String, domainId: String) =
    withContext(Dispatchers.IO) {
        createGrpcClient<SecurityDomainServiceClient>(SECURITY_DOMAIN_BASE_URL, context.requestOauthToken(accountName))
            .GetSecurityDomain()
            .executeBlocking(
                GetSecurityDomainRequest(
                    name = USERS + SECURITY_DOMAINS + domainId,
                    view = 2,
                    requestId = sessionId
                )
            )
    }

suspend fun getSecurityDomainMember(context: Context, accountName: String, sessionId: String, domainId: String) =
    withContext(Dispatchers.IO) {
        createGrpcClient<SecurityDomainServiceClient>(SECURITY_DOMAIN_BASE_URL, context.requestOauthToken(accountName))
            .GetSecurityDomainMember()
            .executeBlocking(
                GetSecurityDomainMemberRequest(
                    name = USERS + MEMBERS + domainId,
                    view = 2,
                    filterOptions = FilterOptions(includeDeleted = true),
                    requestId = sessionId
                )
            )
    }