/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.checkin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class CheckinPrefs implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String PREF_ENABLE_CHECKIN = "checkin_enable_service";
    private static CheckinPrefs INSTANCE;

    public static CheckinPrefs get(Context context) {
        if (INSTANCE == null) {
            if (context == null) return new CheckinPrefs(null);
            INSTANCE = new CheckinPrefs(context.getApplicationContext());
        }
        return INSTANCE;
    }

    private SharedPreferences preferences;
    private boolean checkinEnabled = false;

    private CheckinPrefs(Context context) {
        if (context != null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(context);
            preferences.registerOnSharedPreferenceChangeListener(this);
            update();
        }
    }

    private void update() {
        checkinEnabled = preferences.getBoolean(PREF_ENABLE_CHECKIN, false);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        update();
    }

    public boolean isEnabled() {
        return checkinEnabled;
    }

    public static void setEnabled(Context context, boolean newStatus) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(PREF_ENABLE_CHECKIN, newStatus).commit();
        if (newStatus) {
            context.sendOrderedBroadcast(new Intent(context, TriggerReceiver.class), null);
        }
    }
}
