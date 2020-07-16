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

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.R;

import org.microg.gms.snet.SafetyNetPrefs;
import org.microg.tools.ui.AbstractSettingsActivity;
import org.microg.tools.ui.SwitchBarResourceSettingsFragment;

public class SafetyNetFragment extends SwitchBarResourceSettingsFragment {
    private final int MENU_ADVANCED = Menu.FIRST;

    public SafetyNetFragment() {
        preferencesResource = R.xml.preferences_snet;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);
        switchBar.setChecked(SafetyNetPrefs.get(getContext()).isEnabled());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_ADVANCED, 0, R.string.menu_advanced);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ADVANCED:
                Intent intent = new Intent(getContext(), SafetyNetAdvancedFragment.AsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSwitchBarChanged(boolean isChecked) {
        SafetyNetPrefs.get(getContext()).setEnabled(isChecked);
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