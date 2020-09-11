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

package org.microg.gms.checkin;

import android.content.Context;
import android.content.SharedPreferences;

public class LastCheckinInfo {
    public static final String PREFERENCES_NAME = "checkin";
    public static final String PREF_ANDROID_ID = "androidId";
    public static final String PREF_DIGEST = "digest";
    public static final String PREF_LAST_CHECKIN = "lastCheckin";
    public static final String PREF_SECURITY_TOKEN = "securityToken";
    public static final String PREF_VERSION_INFO = "versionInfo";
    public static final String PREF_DEVICE_DATA_VERSION_INFO = "deviceDataVersionInfo";
    public static final String INITIAL_DIGEST = "1-929a0dca0eee55513280171a8585da7dcd3700f8";
    public long lastCheckin;
    public long androidId;
    public long securityToken;
    public String digest;
    public String versionInfo;
    public String deviceDataVersionInfo;

    public static LastCheckinInfo read(Context context) {
        LastCheckinInfo info = new LastCheckinInfo();
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        info.androidId = preferences.getLong(PREF_ANDROID_ID, 0);
        info.digest = preferences.getString(PREF_DIGEST, INITIAL_DIGEST);
        info.lastCheckin = preferences.getLong(PREF_LAST_CHECKIN, 0);
        info.securityToken = preferences.getLong(PREF_SECURITY_TOKEN, 0);
        info.versionInfo = preferences.getString(PREF_VERSION_INFO, "");
        info.deviceDataVersionInfo = preferences.getString(PREF_DEVICE_DATA_VERSION_INFO, "");
        return info;
    }

    public void write(Context context) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                .putLong(PREF_ANDROID_ID, androidId)
                .putString(PREF_DIGEST, digest)
                .putLong(PREF_LAST_CHECKIN, lastCheckin)
                .putLong(PREF_SECURITY_TOKEN, securityToken)
                .putString(PREF_VERSION_INFO, versionInfo)
                .putString(PREF_DEVICE_DATA_VERSION_INFO, deviceDataVersionInfo)
                .commit();
    }
}
