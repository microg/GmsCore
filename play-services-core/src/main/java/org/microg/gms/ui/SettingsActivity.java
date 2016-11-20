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

package org.microg.gms.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.preference.PreferenceManager;

import com.google.android.gms.R;

import org.microg.gms.gcm.GcmDatabase;
import org.microg.gms.gcm.GcmPrefs;
import org.microg.tools.ui.AbstractDashboardActivity;
import org.microg.tools.ui.ResourceSettingsFragment;

public class SettingsActivity extends AbstractDashboardActivity {

    public SettingsActivity() {
        preferencesResource = R.xml.preferences_start;
        addCondition(Conditions.GCM_BATTERY_OPTIMIZATIONS);
        addCondition(Conditions.PERMISSIONS);
    }

    @Override
    protected Fragment getFragment() {
        return new FragmentImpl();
    }

    public static class FragmentImpl extends ResourceSettingsFragment {

        public static final String PREF_ABOUT = "pref_about";
        public static final String PREF_GCM = "pref_gcm";

        public FragmentImpl() {
            preferencesResource = R.xml.preferences_start;
        }

        @Override
        public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
            super.onCreatePreferencesFix(savedInstanceState, rootKey);
            PreferenceManager prefs = getPreferenceManager();
            prefs.findPreference(PREF_ABOUT).setSummary(getString(R.string.about_version_str, AboutFragment.getSelfVersion(getContext())));
            if (GcmPrefs.get(getContext()).isGcmEnabled()) {
                GcmDatabase database = new GcmDatabase(getContext());
                int regCount = database.getRegistrationList().size();
                database.close();
                prefs.findPreference(PREF_GCM).setSummary(getString(R.string.v7_preference_on) + " / " + getContext().getString(R.string.gcm_registered_apps_counter, regCount));
            } else {
                prefs.findPreference(PREF_GCM).setSummary(R.string.v7_preference_off);
            }
        }
    }
}
