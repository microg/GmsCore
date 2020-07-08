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
import android.preference.PreferenceManager;

import androidx.fragment.app.Fragment;

import com.google.android.gms.R;

import org.microg.tools.ui.AbstractSettingsActivity;
import org.microg.tools.ui.SwitchBarResourceSettingsFragment;

import static org.microg.gms.checkin.TriggerReceiver.PREF_ENABLE_CHECKIN;

public class CheckinFragment extends SwitchBarResourceSettingsFragment {

    public CheckinFragment() {
        preferencesResource = R.xml.preferences_checkin;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        switchBar.setChecked(PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(PREF_ENABLE_CHECKIN, false));
    }

    @Override
    public void onSwitchBarChanged(boolean isChecked) {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean(PREF_ENABLE_CHECKIN, isChecked).apply();
    }


    public static class AsActivity extends AbstractSettingsActivity {
        public AsActivity() {
            showHomeAsUp = true;
        }

        @Override
        protected Fragment getFragment() {
            return new CheckinFragment();
        }
    }
}
