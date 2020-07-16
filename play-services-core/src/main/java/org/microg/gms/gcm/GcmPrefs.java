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

package org.microg.gms.gcm;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GcmPrefs implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String PREF_FULL_LOG = "gcm_full_log";
    public static final String PREF_LAST_PERSISTENT_ID = "gcm_last_persistent_id";
    public static final String PREF_CONFIRM_NEW_APPS = "gcm_confirm_new_apps";
    public static final String PREF_ENABLE_GCM = "gcm_enable_mcs_service";

    public static final String PREF_NETWORK_MOBILE = "gcm_network_mobile";
    public static final String PREF_NETWORK_WIFI = "gcm_network_wifi";
    public static final String PREF_NETWORK_ROAMING = "gcm_network_roaming";
    public static final String PREF_NETWORK_OTHER = "gcm_network_other";

    public static final String PREF_LEARNT_MOBILE = "gcm_learnt_mobile";
    public static final String PREF_LEARNT_WIFI = "gcm_learnt_wifi";
    public static final String PREF_LEARNT_OTHER = "gcm_learnt_other";

    private static final int MIN_INTERVAL = 5 * 60 * 1000; // 5 minutes
    private static final int MAX_INTERVAL = 30 * 60 * 1000; // 30 minutes

    private static GcmPrefs INSTANCE;

    public static GcmPrefs get(Context context) {
        if (INSTANCE == null) {
            if (context == null) return new GcmPrefs(null);
            INSTANCE = new GcmPrefs(context.getApplicationContext());
        }
        return INSTANCE;
    }

    private boolean gcmLogEnabled = true;
    private String lastPersistedId = "";
    private boolean confirmNewApps = false;
    private boolean gcmEnabled = false;

    private int networkMobile = 0;
    private int networkWifi = 0;
    private int networkRoaming = 0;
    private int networkOther = 0;

    private int learntWifi = 300000;
    private int learntMobile = 300000;
    private int learntOther = 300000;

    private SharedPreferences defaultPreferences;

    private GcmPrefs(Context context) {
        if (context != null) {
            defaultPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            defaultPreferences.registerOnSharedPreferenceChangeListener(this);
            update();
        }
    }

    public void update() {
        gcmLogEnabled = defaultPreferences.getBoolean(PREF_FULL_LOG, true);
        lastPersistedId = defaultPreferences.getString(PREF_LAST_PERSISTENT_ID, "");
        confirmNewApps = defaultPreferences.getBoolean(PREF_CONFIRM_NEW_APPS, false);
        gcmEnabled = defaultPreferences.getBoolean(PREF_ENABLE_GCM, true);

        networkMobile = Integer.parseInt(defaultPreferences.getString(PREF_NETWORK_MOBILE, "0"));
        networkWifi = Integer.parseInt(defaultPreferences.getString(PREF_NETWORK_WIFI, "0"));
        networkRoaming = Integer.parseInt(defaultPreferences.getString(PREF_NETWORK_ROAMING, "0"));
        networkOther = Integer.parseInt(defaultPreferences.getString(PREF_NETWORK_OTHER, "0"));

        learntMobile = defaultPreferences.getInt(PREF_LEARNT_MOBILE, 300000);
        learntWifi = defaultPreferences.getInt(PREF_LEARNT_WIFI, 300000);
        learntOther = defaultPreferences.getInt(PREF_LEARNT_OTHER, 300000);
    }

    public String getNetworkPrefForInfo(NetworkInfo info) {
        if (info == null) return PREF_NETWORK_OTHER;
        if (info.isRoaming()) return PREF_NETWORK_ROAMING;
        switch (info.getType()) {
            case ConnectivityManager.TYPE_MOBILE:
                return PREF_NETWORK_MOBILE;
            case ConnectivityManager.TYPE_WIFI:
                return PREF_NETWORK_WIFI;
            default:
                return PREF_NETWORK_OTHER;
        }
    }

    public int getHeartbeatMsFor(NetworkInfo info) {
        return getHeartbeatMsFor(getNetworkPrefForInfo(info), false);
    }

    public int getHeartbeatMsFor(String pref, boolean rawRoaming) {
        if (PREF_NETWORK_ROAMING.equals(pref) && (rawRoaming || networkRoaming != 0)) {
            return networkRoaming * 6000;
        } else if (PREF_NETWORK_MOBILE.equals(pref)) {
            if (networkMobile != 0) return networkMobile * 60000;
            else return learntMobile;
        } else if (PREF_NETWORK_WIFI.equals(pref)) {
            if (networkWifi != 0) return networkWifi * 60000;
            else return learntWifi;
        } else {
            if (networkOther != 0) return networkOther * 60000;
            else return learntOther;
        }
    }

    public int getNetworkValue(String pref) {
        switch (pref) {
            case PREF_NETWORK_MOBILE:
                return networkMobile;
            case PREF_NETWORK_ROAMING:
                return networkRoaming;
            case PREF_NETWORK_WIFI:
                return networkWifi;
            default:
                return networkOther;
        }
    }

    public void learnTimeout(String pref) {
        Log.d("GmsGcmPrefs", "learnTimeout: " + pref);
        switch (pref) {
            case PREF_NETWORK_MOBILE:
            case PREF_NETWORK_ROAMING:
                learntMobile *= 0.95;
                break;
            case PREF_NETWORK_WIFI:
                learntWifi *= 0.95;
                break;
            default:
                learntOther *= 0.95;
                break;
        }
        updateLearntValues();
    }

    public void learnReached(String pref, long time) {
        Log.d("GmsGcmPrefs", "learnReached: " + pref + " / " + time);
        switch (pref) {
            case PREF_NETWORK_MOBILE:
            case PREF_NETWORK_ROAMING:
                if (time > learntMobile / 4 * 3)
                    learntMobile *= 1.02;
                break;
            case PREF_NETWORK_WIFI:
                if (time > learntWifi / 4 * 3)
                    learntWifi *= 1.02;
                break;
            default:
                if (time > learntOther / 4 * 3)
                    learntOther *= 1.02;
                break;
        }
        updateLearntValues();
    }

    private void updateLearntValues() {
        learntMobile = Math.max(MIN_INTERVAL, Math.min(learntMobile, MAX_INTERVAL));
        learntWifi = Math.max(MIN_INTERVAL, Math.min(learntWifi, MAX_INTERVAL));
        learntOther = Math.max(MIN_INTERVAL, Math.min(learntOther, MAX_INTERVAL));
        defaultPreferences.edit().putInt(PREF_LEARNT_MOBILE, learntMobile).putInt(PREF_LEARNT_WIFI, learntWifi).putInt(PREF_LEARNT_OTHER, learntOther).apply();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        update();
    }

    public boolean isEnabled() {
        return gcmEnabled;
    }

    public boolean isEnabledFor(NetworkInfo info) {
        return isEnabled() && info != null && getHeartbeatMsFor(info) >= 0;
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
