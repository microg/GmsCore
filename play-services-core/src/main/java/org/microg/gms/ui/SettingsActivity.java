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

package org.microg.gms.ui;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.google.android.gms.R;

import org.microg.gms.gcm.GcmDatabase;
import org.microg.gms.gcm.GcmPrefs;
import org.microg.gms.snet.SafetyNetPrefs;
import org.microg.nlp.Preferences;
import org.microg.tools.ui.AbstractDashboardActivity;
import org.microg.tools.ui.ResourceSettingsFragment;

import static org.microg.gms.checkin.TriggerReceiver.PREF_ENABLE_CHECKIN;

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
        public static final String PREF_SNET = "pref_snet";
        public static final String PREF_UNIFIEDNLP = "pref_unifiednlp";
        public static final String PREF_CHECKIN = "pref_checkin";

        public FragmentImpl() {
            preferencesResource = R.xml.preferences_start;
        }

        @Override
        public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
            super.onCreatePreferencesFix(savedInstanceState, rootKey);
            updateDetails();
        }

        @Override
        public void onResume() {
            super.onResume();
            updateDetails();
        }

        private void updateDetails() {
            findPreference(PREF_ABOUT).setSummary(getString(R.string.about_version_str, AboutFragment.getSelfVersion(getContext())));
            if (GcmPrefs.get(getContext()).isEnabled()) {
                GcmDatabase database = new GcmDatabase(getContext());
                int regCount = database.getRegistrationList().size();
                database.close();
                findPreference(PREF_GCM).setSummary(getString(R.string.abc_capital_on) + " / " + getResources().getQuantityString(R.plurals.gcm_registered_apps_counter, regCount, regCount));
            } else {
                findPreference(PREF_GCM).setSummary(R.string.abc_capital_off);
            }

            if (SafetyNetPrefs.get(getContext()).isEnabled()) {
                String snet_info = "";

                if (SafetyNetPrefs.get(getContext()).isOfficial()) {
                    snet_info = getString(R.string.pref_snet_status_official_info);
                } else if (SafetyNetPrefs.get(getContext()).isSelfSigned()) {
                    snet_info = getString(R.string.pref_snet_status_self_signed_info);
                } else if (SafetyNetPrefs.get(getContext()).isThirdParty()) {
                    snet_info = getString(R.string.pref_snet_status_third_party_info);
                }

                findPreference(PREF_SNET).setSummary(getString(R.string.service_status_enabled) + " / " + snet_info);
            } else {
                findPreference(PREF_SNET).setSummary(R.string.service_status_disabled);
            }

            Preferences unifiedNlPrefs = new Preferences(getContext());
            int backendCount = TextUtils.isEmpty(unifiedNlPrefs.getLocationBackends()) ? 0 :
                    Preferences.splitBackendString(unifiedNlPrefs.getLocationBackends()).length;
            backendCount += TextUtils.isEmpty(unifiedNlPrefs.getGeocoderBackends()) ? 0 :
                    Preferences.splitBackendString(unifiedNlPrefs.getGeocoderBackends()).length;
            findPreference(PREF_UNIFIEDNLP).setSummary(getResources().getQuantityString(R.plurals.pref_unifiednlp_summary, backendCount, backendCount));

            boolean checkinEnabled = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(PREF_ENABLE_CHECKIN, false);
            findPreference(PREF_CHECKIN).setSummary(checkinEnabled ? R.string.service_status_enabled : R.string.service_status_disabled);
        }
    }
}
