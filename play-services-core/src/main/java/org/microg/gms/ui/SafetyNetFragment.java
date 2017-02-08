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

package org.microg.gms.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.preference.Preference;

import com.google.android.gms.R;

import org.microg.tools.ui.AbstractSettingsActivity;
import org.microg.tools.ui.RadioButtonPreference;
import org.microg.tools.ui.ResourceSettingsFragment;

import static org.microg.gms.snet.SafetyNetPrefs.PREF_SNET_DISABLED;
import static org.microg.gms.snet.SafetyNetPrefs.PREF_SNET_OFFICIAL;
import static org.microg.gms.snet.SafetyNetPrefs.PREF_SNET_THIRD_PARTY;

public class SafetyNetFragment extends ResourceSettingsFragment {

    public SafetyNetFragment() {
        preferencesResource = R.xml.preferences_snet;
    }

    private RadioButtonPreference radioDisabled;
    private RadioButtonPreference radioOfficial;
    private RadioButtonPreference radioThirdParty;

    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferencesFix(savedInstanceState, rootKey);

        radioDisabled = (RadioButtonPreference) findPreference(PREF_SNET_DISABLED);
        radioOfficial = (RadioButtonPreference) findPreference(PREF_SNET_OFFICIAL);
        radioThirdParty = (RadioButtonPreference) findPreference(PREF_SNET_THIRD_PARTY);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == radioDisabled) {
            radioDisabled.setChecked(true);
            radioOfficial.setChecked(false);
            radioThirdParty.setChecked(false);
            return true;
        } else if (preference == radioOfficial) {
            radioDisabled.setChecked(false);
            radioOfficial.setChecked(true);
            radioThirdParty.setChecked(false);
            return true;
        } else if (preference == radioThirdParty) {
            radioDisabled.setChecked(false);
            radioOfficial.setChecked(false);
            radioThirdParty.setChecked(true);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    public static class AsActivity extends AbstractSettingsActivity {
        public AsActivity() {
            showHomeAsUp = true;
        }

        @Override
        protected Fragment getFragment() {
            return new SafetyNetFragment();
        }
    }
}