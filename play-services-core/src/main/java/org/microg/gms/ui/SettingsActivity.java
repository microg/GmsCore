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
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;

import com.google.android.gms.R;

import org.microg.tools.selfcheck.InstalledPackagesChecks;
import org.microg.tools.selfcheck.NlpOsCompatChecks;
import org.microg.tools.selfcheck.NlpStatusChecks;
import org.microg.tools.selfcheck.PermissionCheckGroup;
import org.microg.tools.selfcheck.RomSpoofSignatureChecks;
import org.microg.tools.selfcheck.SelfCheckGroup;
import org.microg.tools.ui.AbstractAboutFragment;
import org.microg.tools.ui.AbstractSelfCheckFragment;

import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.GET_ACCOUNTS;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_wrapper, new MyPreferenceFragment())
                .commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.gms_preferences);

            findPreference(getString(R.string.self_check_title))
                    .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            getFragmentManager().beginTransaction()
                                    .addToBackStack("root")
                                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                    .replace(R.id.content_wrapper, new MySelfCheckFragment())
                                    .commit();
                            return true;
                        }
                    });
            findPreference(getString(R.string.pref_about_title))
                    .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            getFragmentManager().beginTransaction()
                                    .addToBackStack("root")
                                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                    .replace(R.id.content_wrapper, new MyAboutFragment())
                                    .commit();
                            return true;
                        }
                    });
            findPreference(getString(R.string.pref_gcm_apps))
                    .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            getFragmentManager().beginTransaction()
                                    .addToBackStack("root")
                                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                    .replace(R.id.content_wrapper, new GcmRegisteredAppsFragment())
                                    .commit();
                            return true;
                        }
                    });
        }
    }

    public static class MySelfCheckFragment extends AbstractSelfCheckFragment {

        @Override
        protected void prepareSelfCheckList(List<SelfCheckGroup> checks) {
            checks.add(new RomSpoofSignatureChecks());
            checks.add(new InstalledPackagesChecks());
            if (SDK_INT > LOLLIPOP_MR1) {
                checks.add(new PermissionCheckGroup(ACCESS_COARSE_LOCATION, WRITE_EXTERNAL_STORAGE, GET_ACCOUNTS, READ_PHONE_STATE));
            }
            checks.add(new NlpOsCompatChecks());
            checks.add(new NlpStatusChecks());
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            reset(LayoutInflater.from(getContext()));
        }
    }

    public static class MyAboutFragment extends AbstractAboutFragment {

        @Override
        protected void collectLibraries(List<Library> libraries) {
            libraries.add(new Library("org.microg.gms.api", "microG GmsApi", "Apache License 2.0 by microG Team"));
            libraries.add(new Library("org.microg.safeparcel", "microG SafeParcel", "Apache License 2.0 by microG Team"));
            libraries.add(new Library("org.microg.nlp", "microG UnifiedNlp", "Apache License 2.0 by microG Team"));
            libraries.add(new Library("org.microg.nlp.api", "microG UnifiedNlp Api", "Apache License 2.0, by microG Team"));
            libraries.add(new Library("org.microg.wearable", "microG Wearable", "Apache License 2.0 by microG Team"));
            libraries.add(new Library("de.hdodenhof.circleimageview", "CircleImageView", "Apache License 2.0 by Henning Dodenhof"));
            libraries.add(new Library("org.oscim.android", "<vector<tile>>map", "GNU LGPLv3 by Hannes Janetzek"));
            libraries.add(new Library("com.squareup.wire", "Wire Protocol Buffers", "Apache License 2.0 by Square Inc."));
        }
    }
}
