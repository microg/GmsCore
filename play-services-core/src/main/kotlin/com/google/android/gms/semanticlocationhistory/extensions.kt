/**
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory

import android.accounts.AccountManager
import android.content.Context
import com.google.android.gms.semanticlocation.PlaceCandidate
import com.google.android.gms.semanticlocationhistory.db.SemanticSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.folsom.utils.LocalKeyManager
import org.microg.gms.semanticlocationhistory.LocationHistorySegmentProto
import java.util.WeakHashMap

const val TAG = "LocationHistoryService"

const val SEGMENT_TYPE_DELETED = -1
const val SEGMENT_TYPE_UNKNOWN = 0
const val SEGMENT_TYPE_VISIT = 1
const val SEGMENT_TYPE_ACTIVITY = 2
const val SEGMENT_TYPE_PATH = 3
const val SEGMENT_TYPE_MEMORY = 4
const val SEGMENT_TYPE_PERIOD_SUMMARY = 5

const val ODLH_SECURITY_DOMAIN = "users/me/securitydomains/on_device_location_history"
const val ODLH_SECURITY_DOMAIN_SHORT = "on_device_location_history"

const val E2EE_TYPE_URL = "type.googleapis.com/geller.oneplatform.GellerE2eeElement"
const val AES_GCM_IV_SIZE = 12
const val AES_GCM_TAG_BITS = 128
const val AES_KEY_SIZE = 32

const val SEMANTIC_TYPE_HOME = 1
const val SEMANTIC_TYPE_WORK = 2
const val SEARCH_WINDOW_DAYS = 30L
const val SECONDS_PER_DAY = 86400L

suspend fun Context.requestGellerOauthToken(accountName: String, scope: String = "oauth2:https://www.googleapis.com/auth/webhistory"): String {
    val accountManager = AccountManager.get(this)
    val account = accountManager.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE).find {
        it.name == accountName
    }
    if (account == null) throw RuntimeException("account is null")
    return withContext(Dispatchers.IO) {
        accountManager.blockingGetAuthToken(account, scope, true)
    } ?: throw RuntimeException("oauthToken is null")
}

suspend fun Context.getObfuscatedGaiaId(accountName: String) = requestGellerOauthToken(accountName, AuthConstants.SCOPE_GET_ACCOUNT_ID)

private val weakCachedKeyMap = WeakHashMap<String, List<Pair<ByteArray, Int>>>()

fun loadKeyMaterials(context: Context, accountName: String): List<Pair<ByteArray, Int>> {
    val result = weakCachedKeyMap.get(accountName)
    if (!result.isNullOrEmpty()) {
        return result
    }
    val keyManager = LocalKeyManager.getInstance(context)
    val keys = keyManager.getKeysForDomain(accountName, ODLH_SECURITY_DOMAIN)
        .ifEmpty { keyManager.getKeysForDomain(accountName, ODLH_SECURITY_DOMAIN_SHORT) }
    val materials = keys.mapNotNull { key ->
        val material = key.keyMaterial?.toByteArray() ?: return@mapNotNull null
        if (material.size != AES_KEY_SIZE) return@mapNotNull null
        material to (key.keyVersion ?: 0)
    }
    weakCachedKeyMap.put(accountName, materials)
    return materials
}

fun getInferredPlace(visitSegments: List<SemanticSegment>, semanticType: Int): InferredPlace? {
    for (i in visitSegments.indices.reversed()) {
        val segment = visitSegments[i]
        val proto = LocationHistorySegmentProto.ADAPTER.decode(segment.data)
        val place = proto.segment_data?.visit?.place ?: continue
        if ((place.semantic_type?.value ?: 0) == semanticType) {
            val featureId = place.feature_id
            val location = place.location
            val inferredPlace = InferredPlace(
                PlaceCandidate.Identifier(featureId?.high ?: 0L, featureId?.low ?: 0L),
                PlaceCandidate.Point(location?.lat_e7 ?: 0, location?.lng_e7 ?: 0),
                semanticType
            )
            return inferredPlace
        }
    }
    return null
}