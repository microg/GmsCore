/*
 * Copyright 2013-2015 microG Project Team
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GcmData {
    private static final String GCM_REGISTRATION_PREF = "gcm_registrations";
    private static final String GCM_MESSAGES_PREF = "gcm_messages";

    private static final String REMOVED = "%%REMOVED%%";
    private static final String ERROR = "%%ERROR%%";

    private Context context;

    public GcmData(Context context) {
        this.context = context;
    }

    public static class AppInfo implements Comparable<AppInfo> {
        public String app = null;
        public String appSignature = null;
        public String registerID = null;

        private final int STATE_ERROR = 1;
        private final int STATE_REMOVED = 2;
        private final int STATE_REGISTERED = 3;
        private int state;

        public AppInfo(String key, String value) {
            if (ERROR.equals(value)) {
                state = STATE_ERROR;
            } else if (REMOVED.equals(value)) {
                state = STATE_REMOVED;
            } else {
                state = STATE_REGISTERED;
                registerID = value;
            }
            String[] splitKey = key.split(":");
            app = splitKey[0];
            appSignature = splitKey[1];
        }

        public boolean isRegistered() {
            return state == STATE_REGISTERED;
        }

        public boolean isRemoved() {
            return state == STATE_REMOVED;
        }

        public boolean hasUnregistrationError() {
            return state == STATE_ERROR;
        }

        @Override
        public int compareTo(AppInfo another) {
            return app.compareTo(another.app);
        }
    }

    public void app_registered(String app, String signature, String regId) {
        getInfoSharedPreferences().edit().putString(app + ":" + signature, regId).apply();
    }

    public void app_registration_error(String app, String signature) {
        getInfoSharedPreferences().edit().putString(app + ":" + signature, "-").apply();
    }

    public void app_unregistered(String app, String signature) {
        getInfoSharedPreferences().edit().putString(app + ":" + signature, REMOVED).apply();
    }

    public void app_unregistration_error(String app, String signature) {
        getInfoSharedPreferences().edit().putString(app + ":" + signature, ERROR).apply();
    }

    public void incrementAppMessageCount(String app, int i) {
        int messageCount = getStatsSharedPreferences().getInt(app, 0);
        getStatsSharedPreferences().edit().putInt(app, messageCount + i).apply();
    }

    public int getAppMessageCount(String app) {
        return getStatsSharedPreferences().getInt(app, 0);
    }

    public AppInfo getAppInfo(String app, String signature) {
        return getAppInfo(app + signature);
    }

    public List<AppInfo> getAppsInfo() {
        ArrayList<AppInfo> ret = new ArrayList<>();
        Set<String> keys = getInfoSharedPreferences().getAll().keySet();
        for (String key : keys) {
            ret.add(getAppInfo(key));
        }
        return ret;
    }

    private AppInfo getAppInfo(String key) {
        return new AppInfo(key, getInfoSharedPreferences().getString(key, ""));
    }

    private SharedPreferences getInfoSharedPreferences() {
        return context.getSharedPreferences(GCM_REGISTRATION_PREF, Context.MODE_PRIVATE);
    }

    private SharedPreferences getStatsSharedPreferences() {
        return context.getSharedPreferences(GCM_MESSAGES_PREF, Context.MODE_PRIVATE);
    }
}
