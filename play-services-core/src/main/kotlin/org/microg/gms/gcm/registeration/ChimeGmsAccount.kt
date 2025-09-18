/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.gcm.registeration

import google.internal.notifications.v1.RegistrationStatus
import org.json.JSONObject

data class ChimeGmsAccount(
    val id: String,
    val accountName: String,
    val representativeTargetId: String?,
    val registrationStatus: RegistrationStatus?,
    val obfuscatedGaiaId: String?
) {

    fun toJson(): String {
        return JSONObject().apply {
            put(KEY_ID, id)
            put(KEY_ACCOUNT_NAME, accountName)
            put(KEY_REPRESENTATIVE_TARGET_ID, representativeTargetId)
            put(KEY_REGISTRATION_STATUS, registrationStatus?.value)
            put(KEY_OBFUSCATED_GAIA_ID, obfuscatedGaiaId)
        }.toString()
    }

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_ACCOUNT_NAME = "accountName"
        private const val KEY_REPRESENTATIVE_TARGET_ID = "representativeTargetId"
        private const val KEY_REGISTRATION_STATUS = "registrationStatus"
        private const val KEY_OBFUSCATED_GAIA_ID = "obfuscatedGaiaId"

        fun parseJson(json: String): ChimeGmsAccount {
            val jsonObject = JSONObject(json)
            return ChimeGmsAccount(
                id = jsonObject.optString(KEY_ID),
                accountName = jsonObject.optString(KEY_ACCOUNT_NAME),
                representativeTargetId = jsonObject.optString(KEY_REPRESENTATIVE_TARGET_ID),
                registrationStatus = RegistrationStatus.fromValue(jsonObject.optInt(KEY_REGISTRATION_STATUS)),
                obfuscatedGaiaId = jsonObject.optString(KEY_OBFUSCATED_GAIA_ID)
            )
        }
    }
}