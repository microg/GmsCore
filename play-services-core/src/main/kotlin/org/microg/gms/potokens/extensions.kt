/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.potokens

import org.json.JSONObject

const val TAG = "PoTokens"

private const val GMS_PO_TOKENS_SERVER_URL = "https://deviceintegritytokens-pa.googleapis.com"
private const val GMS_PO_TOKENS_API = "AIzaSyBtL0AK6Hzgr69rQyeyhi-V1lmtsPGZd1M"

const val PO_INTEGRITY_TOKEN_SERVER_URL = "$GMS_PO_TOKENS_SERVER_URL/v1/getPoIntegrityToken?alt=proto&key=$GMS_PO_TOKENS_API"

const val TYPE_URL = "type.googleapis.com/google.crypto.tink.AesGcmKey"
const val KEY_TOKEN = "po-token-fast"
const val KEY_FAST = "po-fast-key"
const val KEY_FALLBACK = "extraKeysRetainedInFallback"
const val KEY_PO_TOKEN_ACCESSED_TIME = "po_token_access_time"

const val KEY_DESC = "tokenDesc"
const val KEY_BACKUP = "tokenBackup"
const val KEY_SET_STR = "keySetStr"
const val KEY_UPDATE_TIME = "update_time"

const val KEY_USED_INTEGRITY_TOKEN_INFO = "used_integrity_token_info"

const val PO_TOKEN_ACCESS_LIMIT_TIME = 15 * 1000L
const val PO_TOKEN_ACCESS_LIMIT_COUNT = 2

data class IntegrityTokenInfo(val key: String?, val token: String?, val tokenBackUp: String?, val updateTime: Long) {
    fun toJsonStr(): String {
        return JSONObject()
            .put(KEY_SET_STR, key)
            .put(KEY_DESC, token)
            .put(KEY_BACKUP, tokenBackUp)
            .put(KEY_UPDATE_TIME, updateTime)
            .toString()
    }

    override fun toString(): String {
        return "IntegrityTokenInfo(key='$key', token='$token', tokenBackUp='$tokenBackUp', updateTime=$updateTime"
    }
}

fun parseToIntegrityTokenInfo(json: String): IntegrityTokenInfo {
    val jsonObject = JSONObject(json)
    return IntegrityTokenInfo(
        jsonObject.optString(KEY_SET_STR),
        jsonObject.optString(KEY_DESC),
        jsonObject.optString(KEY_BACKUP),
        jsonObject.optLong(KEY_UPDATE_TIME)
    )
}