/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.potokens.utils

object PoTokenConstants {
    const val PO_TOKENS = "PO_TOKENS"
    const val TOKEN_URL =
        "https://deviceintegritytokens-pa.googleapis.com/v1/getPoIntegrityToken?alt=proto&key=AIzaSyBtL0AK6Hzgr69rQyeyhi-V1lmtsPGZd1M"
    const val TYPE_URL = "type.googleapis.com/google.crypto.tink.AesGcmKey"
    const val KEY_TOKEN = "po-token-fast"
    const val KEY_FAST = "po-fast-key"
    const val KEY_FALLBACK = "extraKeysRetainedInFallback"
    const val KEY_DESC = "tokenDesc"
    const val KEY_BACKUP = "tokenBackup"
    const val KEY_SET_STR = "keySetStr"
}