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

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.widget.SwitchCompat;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.R;

import org.microg.gms.gcm.GcmDatabase;
import org.microg.gms.gcm.GcmPrefs;
import org.microg.gms.gcm.McsService;
import org.microg.tools.ui.AbstractSettingsActivity;
import org.microg.tools.ui.DimmableIconPreference;
import org.microg.tools.ui.ResourceSettingsFragment;
import org.microg.tools.ui.SwitchBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.text.format.DateUtils.FORMAT_SHOW_TIME;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static android.text.format.DateUtils.WEEK_IN_MILLIS;

public class GcmFragment extends ResourceSettingsFragment implements SwitchBar.OnSwitchChangeListener {

    public static final String PREF_GCM_STATUS = "pref_gcm_status";
    public static final String PREF_GCM_APPS = "gcm_apps";

    private GcmDatabase database;

    private SwitchBar switchBar;
    private SwitchCompat switchCompat;
    private boolean listenerSetup = false;

    private final int MENU_ADVANCED = Menu.FIRST;

    public GcmFragment() {
        preferencesResource = R.xml.preferences_gcm;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AbstractSettingsActivity activity = (AbstractSettingsActivity) getActivity();

        setHasOptionsMenu(true);
        switchBar = activity.getSwitchBar();
        switchBar.show();
        switchCompat = switchBar.getSwitch();
        switchBar.setChecked(GcmPrefs.get(getContext()).isGcmEnabled());
    }

    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferencesFix(savedInstanceState, rootKey);

        database = new GcmDatabase(getContext());

        updateContent();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        switchBar.hide();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!listenerSetup) {
            switchBar.addOnSwitchChangeListener(this);
            listenerSetup = true;
        }
        updateContent();
    }

    @Override
    public void onPause() {
        if (listenerSetup) {
            switchBar.removeOnSwitchChangeListener(this);
            listenerSetup = false;
        }
        super.onPause();
        database.close();
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
                Intent intent = new Intent(getContext(), AdvancedAsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSwitchChanged(SwitchCompat switchView, boolean isChecked) {
        if (switchView == switchCompat) {
            getPreferenceManager().getSharedPreferences().edit().putBoolean(GcmPrefs.PREF_ENABLE_GCM, isChecked).apply();
        }
    }

    private static void addPreferencesSorted(List<Preference> prefs, PreferenceGroup container) {
        // If there's some items to display, sort the items and add them to the container.
        Collections.sort(prefs, new Comparator<Preference>() {
            @Override
            public int compare(Preference lhs, Preference rhs) {
                return lhs.getTitle().toString().compareTo(rhs.getTitle().toString());
            }
        });
        for (Preference entry : prefs) {
            container.addPreference(entry);
        }
    }

    private void updateContent() {
        PreferenceScreen root = getPreferenceScreen();

        if (McsService.isConnected()) {
            root.findPreference(PREF_GCM_STATUS).setSummary(getString(R.string.gcm_state_connected, DateUtils.getRelativeTimeSpanString(McsService.getStartTimestamp(), System.currentTimeMillis(), 0)));
        } else {
            root.findPreference(PREF_GCM_STATUS).setSummary(getString(R.string.gcm_state_disconnected));
        }

        PreferenceCategory appList = (PreferenceCategory) root.findPreference(PREF_GCM_APPS);
        appList.removeAll();
        List<GcmDatabase.App> list = database.getAppList();
        if (!list.isEmpty()) {
            List<Preference> appListPrefs = new ArrayList<>();
            PackageManager pm = getContext().getPackageManager();
            for (GcmDatabase.App app : list) {
                try {
                    pm.getApplicationInfo(app.packageName, 0);
                    appListPrefs.add(new GcmAppPreference(getPreferenceManager().getContext(), app));
                } catch (PackageManager.NameNotFoundException e) {
                    final List<GcmDatabase.Registration> registrations = database.getRegistrationsByApp(app.packageName);
                    if (registrations.isEmpty()) {
                        database.removeApp(app.packageName);
                    } else {
                        appListPrefs.add(new GcmAppPreference(getPreferenceManager().getContext(), app));
                    }
                }
            }
            addPreferencesSorted(appListPrefs, appList);
        } else {
            // If there's no item to display, add a "None" item.
            Preference banner = new Preference(getPreferenceManager().getContext());
            banner.setLayoutResource(R.layout.list_no_item);
            banner.setTitle(R.string.list_no_item_none);
            banner.setSelectable(false);
            appList.addPreference(banner);
        }
    }

    public static class GcmAppPreference extends DimmableIconPreference implements Preference.OnPreferenceClickListener {

        private GcmDatabase database;
        private GcmDatabase.App app;

        public GcmAppPreference(Context context, GcmDatabase.App app) {
            super(context);
            this.app = app;
            this.database = new GcmDatabase(context);
            setKey(app.packageName);

            PackageManager packageManager = context.getPackageManager();
            try {
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(app.packageName, 0);
                setTitle(packageManager.getApplicationLabel(applicationInfo));
                setIcon(packageManager.getApplicationIcon(applicationInfo));
            } catch (PackageManager.NameNotFoundException e) {
                setTitle(app.packageName);
                setIcon(android.R.drawable.sym_def_app_icon);
            }
            setOnPreferenceClickListener(this);
            updateViewDetails();
        }

        private void updateViewDetails() {
            if (database.getRegistrationsByApp(app.packageName).isEmpty()) {
                setSummary(R.string.gcm_not_registered);
            } else if (app.lastMessageTimestamp > 0) {
                setSummary(getContext().getString(R.string.gcm_last_message_at, DateUtils.getRelativeDateTimeString(getContext(), app.lastMessageTimestamp, MINUTE_IN_MILLIS, WEEK_IN_MILLIS, FORMAT_SHOW_TIME)));
            } else {
                setSummary(R.string.gcm_no_message_yet);
            }
            database.close();
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder view) {
            updateViewDetails();
            super.onBindViewHolder(view);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            Intent intent = new Intent(getContext(), GcmAppFragment.AsActivity.class);
            intent.putExtra(GcmAppFragment.EXTRA_PACKAGE_NAME, app.packageName);
            getContext().startActivity(intent);
            return true;
        }
    }

    public static class AsActivity extends AbstractSettingsActivity {
        public AsActivity() {
            showHomeAsUp = true;
        }

        @Override
        protected Fragment getFragment() {
            return new GcmFragment();
        }
    }

    public static class AdvancedAsActivity extends AbstractSettingsActivity {
        public AdvancedAsActivity() {
            showHomeAsUp = true;
            preferencesResource = R.xml.preferences_gcm_advanced;
        }
    }
}