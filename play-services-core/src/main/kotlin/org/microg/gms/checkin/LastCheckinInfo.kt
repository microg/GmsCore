/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.microg.gms.checkin

import android.content.Context
import org.microg.gms.settings.SettingsContract
import org.microg.gms.settings.SettingsContract.CheckIn

data class LastCheckinInfo(
    val lastCheckin: Long,
    val androidId: Long,
    val securityToken: Long,
    val digest: String,
    val versionInfo: String,
    val deviceDataVersionInfo: String,
) {

    constructor(r: CheckinResponse) : this(
        lastCheckin = r.timeMs ?: 0L,
        androidId = r.androidId ?: 0L,
        securityToken = r.securityToken ?: 0L,
        digest = r.digest ?: CheckIn.INITIAL_DIGEST,
        versionInfo = r.versionInfo ?: "",
        deviceDataVersionInfo = r.deviceDataVersionInfo ?: "",
    )

    companion object {
        @JvmStatic
        fun read(context: Context): LastCheckinInfo {
            val projection = arrayOf(
                CheckIn.ANDROID_ID,
                CheckIn.DIGEST,
                CheckIn.LAST_CHECK_IN,
                CheckIn.SECURITY_TOKEN,
                CheckIn.VERSION_INFO,
                CheckIn.DEVICE_DATA_VERSION_INFO,
            )
            return SettingsContract.getSettings(context, CheckIn.getContentUri(context), projection) { c ->
                LastCheckinInfo(
                    androidId = c.getLong(0),
                    digest = c.getString(1),
                    lastCheckin = c.getLong(2),
                    securityToken = c.getLong(3),
                    versionInfo = c.getString(4),
                    deviceDataVersionInfo = c.getString(5),
                )
            }
        }

        @JvmStatic
        fun clear(context: Context) = SettingsContract.setSettings(context, CheckIn.getContentUri(context)) {
            put(CheckIn.ANDROID_ID, 0L)
            put(CheckIn.DIGEST, CheckIn.INITIAL_DIGEST)
            put(CheckIn.LAST_CHECK_IN, 0L)
            put(CheckIn.SECURITY_TOKEN, 0L)
            put(CheckIn.VERSION_INFO, "")
            put(CheckIn.DEVICE_DATA_VERSION_INFO, "")
        }
    }

    fun write(context: Context) = SettingsContract.setSettings(context, CheckIn.getContentUri(context)) {
        put(CheckIn.ANDROID_ID, androidId)
        put(CheckIn.DIGEST, digest)
        put(CheckIn.LAST_CHECK_IN, lastCheckin)
        put(CheckIn.SECURITY_TOKEN, securityToken)
        put(CheckIn.VERSION_INFO, versionInfo)
        put(CheckIn.DEVICE_DATA_VERSION_INFO, deviceDataVersionInfo)
    }
}
