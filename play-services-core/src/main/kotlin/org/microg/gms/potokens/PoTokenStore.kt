/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.potokens

import android.content.Context
import android.content.SharedPreferences

class PoTokenStore(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences("com.google.android.gms.potokens", Context.MODE_PRIVATE)

    fun getString(key: String, defValue: String?): String? {
        return sp.getString(key, defValue)
    }

    fun getInt(key: String, defValue: Int): Int {
        return sp.getInt(key, defValue)
    }

    fun saveUsedTokenInfo(info: IntegrityTokenInfo) {
        sp.edit().putString(KEY_USED_INTEGRITY_TOKEN_INFO, info.toJsonStr()).apply()
    }

    fun loadUsedIntegrityTokenInfo(): IntegrityTokenInfo? {
        var tokenDesc = getString(KEY_DESC, "")
        var tokenBackup = getString(KEY_BACKUP, "")
        var keySetStr = getString(KEY_SET_STR, "")
        return if (tokenDesc.isNullOrEmpty() || tokenBackup.isNullOrEmpty() || keySetStr.isNullOrEmpty()) {
            val json = sp.getString(KEY_USED_INTEGRITY_TOKEN_INFO, null) ?: return null
            parseToIntegrityTokenInfo(json)
        } else{
            IntegrityTokenInfo(keySetStr, tokenDesc, tokenBackup, System.currentTimeMillis())
        }
    }

    fun updateLastUpdateTime() {
        sp.edit().putLong(KEY_PO_TOKEN_ACCESSED_TIME, System.currentTimeMillis()).apply()
    }

    fun getLastUpdateTime(): Long {
        return sp.getLong(KEY_PO_TOKEN_ACCESSED_TIME, 0)
    }

    fun clearOldTokenInfo() {
        val edit = sp.edit()
        edit.remove(KEY_DESC).apply()
        edit.remove(KEY_BACKUP).apply()
        edit.remove(KEY_SET_STR).apply()
    }
}
