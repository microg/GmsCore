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
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import org.microg.gms.common.PackageUtils;

import java.io.File;
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
            PackageUtils.warnIfNotPersistentProcess(GcmPrefs.class);
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

    private final Context context;
    private SharedPreferences preferences;
    private SharedPreferences systemDefaultPreferences;

    private GcmPrefs(Context context) {
        this.context = context;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.registerOnSharedPreferenceChangeListener(this);
        try {
            systemDefaultPreferences = (SharedPreferences) Context.class.getDeclaredMethod("getSharedPreferences", File.class, int.class).invoke(context, new File("/system/etc/microg.xml"), Context.MODE_PRIVATE);
        } catch (Exception ignored) {
        }
        update();
    }

    private boolean getSettingsBoolean(String key, boolean def) {
        if (systemDefaultPreferences != null) {
            def = systemDefaultPreferences.getBoolean(key, def);
        }
        return preferences.getBoolean(key, def);
    }

    public void update() {
        gcmEnabled = getSettingsBoolean(PREF_ENABLE_GCM, false);
        gcmLogEnabled = getSettingsBoolean(PREF_FULL_LOG, true);
        confirmNewApps = getSettingsBoolean(PREF_CONFIRM_NEW_APPS, false);

        lastPersistedId = preferences.getString(PREF_LAST_PERSISTENT_ID, "");

        networkMobile = Integer.parseInt(preferences.getString(PREF_NETWORK_MOBILE, "0"));
        networkWifi = Integer.parseInt(preferences.getString(PREF_NETWORK_WIFI, "0"));
        networkRoaming = Integer.parseInt(preferences.getString(PREF_NETWORK_ROAMING, "0"));
        networkOther = Integer.parseInt(preferences.getString(PREF_NETWORK_OTHER, "0"));

        learntMobile = preferences.getInt(PREF_LEARNT_MOBILE, 300000);
        learntWifi = preferences.getInt(PREF_LEARNT_WIFI, 300000);
        learntOther = preferences.getInt(PREF_LEARNT_OTHER, 300000);
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
        return getHeartbeatMsFor(getNetworkPrefForInfo(info));
    }

    public int getMobileInterval() {
        return networkMobile;
    }

    public int getWifiInterval() {
        return networkWifi;
    }

    public int getRoamingInterval() {
        return networkRoaming;
    }

    public int getOtherInterval() {
        return networkOther;
    }

    public void setMobileInterval(int value) {
        this.networkMobile = value;
        preferences.edit().putString(PREF_NETWORK_MOBILE, Integer.toString(networkMobile)).apply();
    }

    public void setWifiInterval(int value) {
        this.networkWifi = value;
        preferences.edit().putString(PREF_NETWORK_WIFI, Integer.toString(networkWifi)).apply();
    }

    public void setRoamingInterval(int value) {
        this.networkRoaming = value;
        preferences.edit().putString(PREF_NETWORK_ROAMING, Integer.toString(networkRoaming)).apply();
    }

    public void setOtherInterval(int value) {
        this.networkOther = value;
        preferences.edit().putString(PREF_NETWORK_OTHER, Integer.toString(networkOther)).apply();
    }

    public int getHeartbeatMsFor(String pref) {
        if (PREF_NETWORK_ROAMING.equals(pref)) {
            if (networkRoaming != 0) return networkRoaming * 60000;
            else return learntMobile;
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
        preferences.edit().putInt(PREF_LEARNT_MOBILE, learntMobile).putInt(PREF_LEARNT_WIFI, learntWifi).putInt(PREF_LEARNT_OTHER, learntOther).apply();
    }

    public int getLearntMobileInterval() {
        return learntMobile;
    }

    public int getLearntWifiInterval() {
        return learntWifi;
    }

    public int getLearntOtherInterval() {
        return learntOther;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        update();
    }

    public boolean isEnabled() {
        return gcmEnabled;
    }

    public void setEnabled(boolean value) {
        boolean changed = gcmEnabled != value;
        preferences.edit().putBoolean(GcmPrefs.PREF_ENABLE_GCM, value).apply();
        if (!changed) return;
        if (!value) {
            McsService.stop(context);
        } else {
            context.sendBroadcast(new Intent(TriggerReceiver.FORCE_TRY_RECONNECT, null, context, TriggerReceiver.class));
        }
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

    public void setConfirmNewApps(boolean value) {
        confirmNewApps = value;
        preferences.edit().putBoolean(PREF_CONFIRM_NEW_APPS, value).apply();
    }

    public List<String> getLastPersistedIds() {
        if (lastPersistedId.isEmpty()) return Collections.emptyList();
        return Arrays.asList(lastPersistedId.split("\\|"));
    }

    public void extendLastPersistedId(String id) {
        if (!lastPersistedId.isEmpty()) lastPersistedId += "|";
        lastPersistedId += id;
        preferences.edit().putString(PREF_LAST_PERSISTENT_ID, lastPersistedId).apply();
    }

    public void clearLastPersistedId() {
        lastPersistedId = "";
        preferences.edit().putString(PREF_LAST_PERSISTENT_ID, lastPersistedId).apply();
    }
}
