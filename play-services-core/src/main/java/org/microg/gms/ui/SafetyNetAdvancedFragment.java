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

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.google.android.gms.R;

import org.microg.tools.ui.AbstractSettingsActivity;
import org.microg.tools.ui.RadioButtonPreference;
import org.microg.tools.ui.ResourceSettingsFragment;

import static org.microg.gms.snet.SafetyNetPrefs.PREF_SNET_OFFICIAL;
import static org.microg.gms.snet.SafetyNetPrefs.PREF_SNET_SELF_SIGNED;
import static org.microg.gms.snet.SafetyNetPrefs.PREF_SNET_THIRD_PARTY;

public class SafetyNetAdvancedFragment extends ResourceSettingsFragment {

    public SafetyNetAdvancedFragment() {
        preferencesResource = R.xml.preferences_snet_advanced;
    }

    private RadioButtonPreference radioOfficial;
    private RadioButtonPreference radioSelfSigned;
    private RadioButtonPreference radioThirdParty;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);

        radioOfficial = (RadioButtonPreference) findPreference(PREF_SNET_OFFICIAL);
        radioSelfSigned = (RadioButtonPreference) findPreference(PREF_SNET_SELF_SIGNED);
        radioThirdParty = (RadioButtonPreference) findPreference(PREF_SNET_THIRD_PARTY);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == radioOfficial) {
            radioOfficial.setChecked(true);
            radioSelfSigned.setChecked(false);
            radioThirdParty.setChecked(false);
            return true;
        } else if (preference == radioSelfSigned) {
            radioOfficial.setChecked(false);
            radioSelfSigned.setChecked(true);
            radioThirdParty.setChecked(false);
            return true;
        } else if (preference == radioThirdParty) {
            radioOfficial.setChecked(false);
            radioSelfSigned.setChecked(false);
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
            return new SafetyNetAdvancedFragment();
        }
    }
}
