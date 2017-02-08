/*
 * Copyright 2013-2016 microG Project Team
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

package org.microg.gms.gcm;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GcmPrefs implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String PREF_HEARTBEAT = "gcm_heartbeat_interval";
    public static final String PREF_FULL_LOG = "gcm_full_log";
    public static final String PREF_LAST_PERSISTENT_ID = "gcm_last_persistent_id";
    public static final String PREF_CONFIRM_NEW_APPS = "gcm_confirm_new_apps";
    public static final String PREF_ENABLE_GCM = "gcm_enable_mcs_service";

    private static GcmPrefs INSTANCE;

    public static GcmPrefs get(Context context) {
        if (INSTANCE == null) {
            if (context == null) return new GcmPrefs(null);
            INSTANCE = new GcmPrefs(context.getApplicationContext());
        }
        return INSTANCE;
    }

    private int heartbeatMs = 300000;
    private boolean gcmLogEnabled = true;
    private String lastPersistedId = "";
    private boolean confirmNewApps = false;
    private boolean gcmEnabled = false;

    private SharedPreferences defaultPreferences;

    private GcmPrefs(Context context) {
        if (context != null) {
            defaultPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            defaultPreferences.registerOnSharedPreferenceChangeListener(this);
            update();
        }
    }

    public void update() {
        heartbeatMs = Integer.parseInt(defaultPreferences.getString(PREF_HEARTBEAT, "300")) * 1000;
        gcmLogEnabled = defaultPreferences.getBoolean(PREF_FULL_LOG, true);
        lastPersistedId = defaultPreferences.getString(PREF_LAST_PERSISTENT_ID, "");
        confirmNewApps = defaultPreferences.getBoolean(PREF_CONFIRM_NEW_APPS, false);
        gcmEnabled = defaultPreferences.getBoolean(PREF_ENABLE_GCM, false);
    }

    public int getHeartbeatMs() {
        return heartbeatMs;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        update();
    }

    public boolean isGcmEnabled() {
        return gcmEnabled;
    }

    public boolean isGcmLogEnabled() {
        return gcmLogEnabled;
    }

    public boolean isConfirmNewApps() {
        return confirmNewApps;
    }

    public List<String> getLastPersistedIds() {
        if (lastPersistedId.isEmpty()) return Collections.emptyList();
        return Arrays.asList(lastPersistedId.split("\\|"));
    }

    public void extendLastPersistedId(String id) {
        if (!lastPersistedId.isEmpty()) lastPersistedId += "|";
        lastPersistedId += id;
        defaultPreferences.edit().putString(PREF_LAST_PERSISTENT_ID, lastPersistedId).apply();
    }

    public void clearLastPersistedId() {
        lastPersistedId = "";
        defaultPreferences.edit().putString(PREF_LAST_PERSISTENT_ID, lastPersistedId).apply();
    }
}
