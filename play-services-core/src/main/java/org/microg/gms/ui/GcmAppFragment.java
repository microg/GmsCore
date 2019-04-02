package org.microg.gms.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.R;

import org.microg.gms.gcm.GcmDatabase;
import org.microg.gms.gcm.PushRegisterManager;
import org.microg.tools.ui.AbstractSettingsActivity;
import org.microg.tools.ui.ResourceSettingsFragment;

import java.util.List;

import static android.text.format.DateUtils.FORMAT_SHOW_TIME;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static android.text.format.DateUtils.WEEK_IN_MILLIS;

public class GcmAppFragment extends ResourceSettingsFragment {
    public static final String EXTRA_PACKAGE_NAME = "package_name";

    public static final String PREF_WAKE_FOR_DELIVERY = "gcm_app_wake_for_delivery";
    public static final String PREF_ALLOW_REGISTER = "gcm_app_allow_register";
    public static final String PREF_REGISTER_DETAILS = "gcm_app_register_details";
    public static final String PREF_MESSAGE_DETAILS = "gcm_app_message_details";

    protected String packageName;
    private String appName;
    private GcmDatabase database;

    public GcmAppFragment() {
        preferencesResource = R.xml.preferences_gcm_app_detail;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        packageName = getArguments().getString(EXTRA_PACKAGE_NAME);

        AbstractSettingsActivity activity = (AbstractSettingsActivity) getActivity();

        if (packageName != null && activity != null) {
            activity.setCustomBarLayout(R.layout.app_bar);
            try {
                PackageManager pm = activity.getPackageManager();
                ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
                ((ImageView) activity.findViewById(R.id.app_icon)).setImageDrawable(pm.getApplicationIcon(info));
                appName = pm.getApplicationLabel(info).toString();
                ((TextView) activity.findViewById(R.id.app_name)).setText(appName);
                View view = activity.findViewById(R.id.app_bar);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", packageName, null);
                        intent.setData(uri);
                        getContext().startActivity(intent);
                    }
                });
                view.setClickable(true);
            } catch (Exception e) {
                appName = packageName;
                ((TextView) activity.findViewById(R.id.app_name)).setText(packageName);
            }
        }

        database = new GcmDatabase(getContext());
        updateAppDetails();
    }

    @Override
    public void onPause() {
        super.onPause();
        database.close();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (database != null) {
            updateAppDetails();
        }
    }

    private void updateAppDetails() {
        GcmDatabase.App app = database.getApp(packageName);
        if (app == null) {
            getActivity().finish();
            return;
        }
        PreferenceScreen root = getPreferenceScreen();

        SwitchPreference wakeForDelivery = (SwitchPreference) root.findPreference(PREF_WAKE_FOR_DELIVERY);
        wakeForDelivery.setChecked(app.wakeForDelivery);
        wakeForDelivery.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue instanceof Boolean) {
                    database.setAppWakeForDelivery(packageName, (Boolean) newValue);
                    return true;
                }
                return false;
            }
        });

        SwitchPreference allowRegister = (SwitchPreference) root.findPreference(PREF_ALLOW_REGISTER);
        allowRegister.setChecked(app.allowRegister);
        allowRegister.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue instanceof Boolean) {
                    if (!(boolean) newValue) {
                        final List<GcmDatabase.Registration> registrations = database.getRegistrationsByApp(packageName);
                        if (!registrations.isEmpty()) {
                            showUnregisterConfirm(registrations, getString(R.string.gcm_unregister_after_deny_message));
                        }
                    }
                    database.setAppAllowRegister(packageName, (Boolean) newValue);
                    return true;
                }
                return false;
            }
        });

        Preference registerDetails = root.findPreference(PREF_REGISTER_DETAILS);
        final List<GcmDatabase.Registration> registrations = database.getRegistrationsByApp(packageName);
        if (registrations.isEmpty()) {
            registerDetails.setTitle("");
            registerDetails.setSelectable(false);
            registerDetails.setSummary(R.string.gcm_not_registered);
        } else {
            StringBuilder sb = new StringBuilder();
            for (GcmDatabase.Registration registration : registrations) {
                if (sb.length() != 0) sb.append("\n");
                if (registration.timestamp == 0) {
                    sb.append(getString(R.string.gcm_registered));
                } else {
                    sb.append(getString(R.string.gcm_registered_since, DateUtils.getRelativeDateTimeString(getContext(), registration.timestamp, MINUTE_IN_MILLIS, WEEK_IN_MILLIS, FORMAT_SHOW_TIME)));
                }
            }
            registerDetails.setTitle(R.string.gcm_unregister_app);
            registerDetails.setSummary(sb.toString());
            registerDetails.setSelectable(true);
            registerDetails.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showUnregisterConfirm(registrations, getString(R.string.gcm_unregister_confirm_message));
                    return true;
                }
            });
        }

        Preference messageDetails = root.findPreference(PREF_MESSAGE_DETAILS);
        if (app.totalMessageCount == 0) {
            messageDetails.setSummary(R.string.gcm_no_message_yet);
        } else {
            String s = getString(R.string.gcm_messages_counter, app.totalMessageCount, app.totalMessageBytes);
            if (app.lastMessageTimestamp != 0) {
                s += "\n" + getString(R.string.gcm_last_message_at, DateUtils.getRelativeDateTimeString(getContext(), app.lastMessageTimestamp, MINUTE_IN_MILLIS, WEEK_IN_MILLIS, FORMAT_SHOW_TIME));
            }
            messageDetails.setSummary(s);
        }
    }

    private void showUnregisterConfirm(final List<GcmDatabase.Registration> registrations, String unregisterConfirmDesc) {
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.gcm_unregister_confirm_title, appName))
                .setMessage(unregisterConfirmDesc)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                for (GcmDatabase.Registration registration : registrations) {
                                    PushRegisterManager.unregister(getContext(), registration.packageName, registration.signature, null, null);
                                }
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateAppDetails();
                                    }
                                });
                            }
                        }).start();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                }).show();
    }

    public static class AsActivity extends AbstractSettingsActivity {
        public AsActivity() {
            showHomeAsUp = true;
        }

        @Override
        protected Fragment getFragment() {
            GcmAppFragment fragment = new GcmAppFragment();
            fragment.setArguments(getIntent().getExtras());
            return fragment;
        }
    }
}
