/*
 * Copyright (C) 2017 microG Project Team
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

package org.microg.gms.snet;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.microg.gms.common.PackageUtils;

import java.io.File;

public class SafetyNetPrefs implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String OFFICIAL_ATTEST_BASE_URL = "https://www.googleapis.com/androidcheck/v1/attestations/attest";

    public static final String PREF_SNET_DISABLED = "snet_disabled";
    public static final String PREF_SNET_OFFICIAL = "snet_official";
    public static final String PREF_SNET_THIRD_PARTY = "snet_third_party";
    public static final String PREF_SNET_CUSTOM_URL = "snet_custom_url";
    public static final String PREF_SNET_SELF_SIGNED = "snet_self_signed";

    private static SafetyNetPrefs INSTANCE;

    public static SafetyNetPrefs get(Context context) {
        if (INSTANCE == null) {
            if (!context.getPackageName().equals(PackageUtils.getProcessName())) {
                Log.w("Preferences", SafetyNetPrefs.class.getName() + " initialized outside main process", new RuntimeException());
            }
            if (context == null) return new SafetyNetPrefs(null);
            INSTANCE = new SafetyNetPrefs(context.getApplicationContext());
        }
        return INSTANCE;
    }

    private boolean disabled;
    private boolean official;
    private boolean selfSigned;
    private boolean thirdParty;
    private String customUrl;

    private SharedPreferences preferences;
    private SharedPreferences systemDefaultPreferences;

    private SafetyNetPrefs(Context context) {
        if (context != null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(context);
            preferences.registerOnSharedPreferenceChangeListener(this);
            try {
                systemDefaultPreferences = (SharedPreferences) Context.class.getDeclaredMethod("getSharedPreferences", File.class, int.class).invoke(context, new File("/system/etc/microg.xml"), Context.MODE_PRIVATE);
            } catch (Exception ignored) {
            }
            update();
        }
    }

    private boolean getSettingsBoolean(String key, boolean def) {
        if (systemDefaultPreferences != null) {
            def = systemDefaultPreferences.getBoolean(key, def);
        }
        return preferences.getBoolean(key, def);
    }

    private String getSettingsString(String key, String def) {
        if (systemDefaultPreferences != null) {
            def = systemDefaultPreferences.getString(key, def);
        }
        return preferences.getString(key, def);
    }

    public void update() {
        disabled = getSettingsBoolean(PREF_SNET_DISABLED, true);
        official = getSettingsBoolean(PREF_SNET_OFFICIAL, false);
        selfSigned = getSettingsBoolean(PREF_SNET_SELF_SIGNED, false);
        thirdParty = getSettingsBoolean(PREF_SNET_THIRD_PARTY, false);
        customUrl = getSettingsString(PREF_SNET_CUSTOM_URL, null);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        update();
    }

    public boolean isEnabled() {
        return !disabled && (official || selfSigned || thirdParty);
    }

    public void setEnabled(boolean enabled) {
        SharedPreferences.Editor edit = preferences.edit();
        edit.putBoolean(PREF_SNET_DISABLED, !enabled);
        if (enabled && !isEnabled()) {
            official = true;
            edit.putBoolean(PREF_SNET_OFFICIAL, true);
        }
        edit.commit();
    }

    public boolean isSelfSigned() {
        return selfSigned;
    }

    public boolean isOfficial() {
        return official;
    }

    public boolean isThirdParty() {
        return thirdParty;
    }

    public String getServiceUrl() {
        if (official) return OFFICIAL_ATTEST_BASE_URL;
        return customUrl;
    }
}
