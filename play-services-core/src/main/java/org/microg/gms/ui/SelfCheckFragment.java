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

import android.content.Context;
import android.content.Intent;
import android.content.pm.CrossProfileApps;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import org.microg.gms.common.Constants;
import org.microg.tools.selfcheck.InstalledPackagesChecks;
import org.microg.tools.selfcheck.InstalledPatcherChecks;
//import org.microg.tools.selfcheck.NlpOsCompatChecks;
//import org.microg.tools.selfcheck.NlpStatusChecks;
import org.microg.tools.selfcheck.PermissionCheckGroup;
import org.microg.tools.selfcheck.RomSpoofSignatureChecks;
import org.microg.tools.selfcheck.SelfCheckGroup;
import org.microg.tools.selfcheck.SystemChecks;
import org.microg.tools.ui.AbstractSelfCheckFragment;
import org.microg.tools.ui.AbstractSettingsActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.GET_ACCOUNTS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_MEDIA_AUDIO;
import static android.Manifest.permission.READ_MEDIA_IMAGES;
import static android.Manifest.permission.READ_MEDIA_VIDEO;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.RECEIVE_SMS;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;

public class SelfCheckFragment extends AbstractSelfCheckFragment {

    @Override
    protected void prepareSelfCheckList(List<SelfCheckGroup> checks) {
        Context context = getContext();
        checks.add(new InstalledPatcherChecks());
        if (Objects.equals(context.getPackageName(), Constants.GMS_PACKAGE_NAME)) {
            checks.add(new RomSpoofSignatureChecks());
        }
        checks.add(new InstalledPackagesChecks());
        if (SDK_INT >= 23) {
            List<String> permissions = new ArrayList<>();
            permissions.add(ACCESS_COARSE_LOCATION);
            permissions.add(ACCESS_FINE_LOCATION);
            if (SDK_INT >= 29) {
                permissions.add(ACCESS_BACKGROUND_LOCATION);
            }
            // Storage permissions changed across API levels: WRITE_EXTERNAL_STORAGE stopped
            // being runtime-grantable at API 30, and READ_EXTERNAL_STORAGE at API 33 was
            // replaced by READ_MEDIA_*. Requesting the legacy ones on new Android silently
            // does nothing, so only add the grantable permissions for the current API level.
            if (SDK_INT >= 33) {
                permissions.add(READ_MEDIA_IMAGES);
                permissions.add(READ_MEDIA_VIDEO);
                permissions.add(READ_MEDIA_AUDIO);
            } else {
                permissions.add(READ_EXTERNAL_STORAGE);
            }
            if (SDK_INT < 30) {
                permissions.add(WRITE_EXTERNAL_STORAGE);
            }
            permissions.add(GET_ACCOUNTS);
            permissions.add(READ_PHONE_STATE);
            permissions.add(RECEIVE_SMS);
            checks.add(new PermissionCheckGroup(permissions.toArray(new String[0])) {
                @Override
                public void doChecks(Context context, ResultCollector collector) {
                    super.doChecks(context, collector);
                    PackageManager pm = context.getPackageManager();
                    // Notifications: mirror MicroG-RE — open notification settings instead of requesting
                    // POST_NOTIFICATIONS, which the system silently auto-dismisses when the app-op is blocked
                    if (SDK_INT >= 33) {
                        boolean notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled();
                        collector.addResult(
                                context.getString(com.google.android.gms.R.string.self_check_name_notifications),
                                notificationsEnabled ? Result.Positive : Result.Negative,
                                context.getString(com.google.android.gms.R.string.self_check_resolution_notifications),
                                fragment -> {
                                    Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
                                    ((AbstractSelfCheckFragment) fragment).launchIntent(intent);
                                }
                        );
                    }
                    // Add SYSTEM_ALERT_WINDOW appops permission
                    try {
                        PermissionInfo info = pm.getPermissionInfo("android.permission.SYSTEM_ALERT_WINDOW", 0);
                        CharSequence permLabel = info.loadLabel(pm);
                        collector.addResult(
                                context.getString(org.microg.tools.ui.R.string.self_check_name_permission, permLabel),
                                Settings.canDrawOverlays(context) ? Result.Positive : Result.Negative,
                                context.getString(org.microg.tools.ui.R.string.self_check_resolution_permission),
                                fragment -> {
                                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName()));
                                    startActivityForResult(intent, 42);
                                }
                        );
                    } catch (Exception e) {
                        Log.w("SelfCheckPerms", e);
                    }
                    // Add INTERACT_ACROSS_PROFILES appop permission (INTERACT_ACROSS_USERS is superior)
                    // Only show Negative when a request is actually possible (work profile exists);
                    // otherwise Neutral so the row isn't a dead end.
                    if (SDK_INT >= 30) try {
                        CrossProfileApps crossProfile = context.getSystemService(CrossProfileApps.class);
                        boolean interactAllowed = context.checkSelfPermission("android.permission.INTERACT_ACROSS_USERS") == PackageManager.PERMISSION_GRANTED
                                || crossProfile.canInteractAcrossProfiles();
                        boolean canRequest = crossProfile.canRequestInteractAcrossProfiles();
                        collector.addResult(
                                context.getString(org.microg.tools.ui.R.string.self_check_name_permission_interact_across_profiles),
                                interactAllowed ? Result.Positive : (canRequest ? Result.Negative : Result.Neutral),
                                context.getString(org.microg.tools.ui.R.string.self_check_resolution_permission),
                                canRequest ? fragment -> {
                                    Intent intent = crossProfile.createRequestInteractAcrossProfilesIntent();
                                    ((AbstractSelfCheckFragment) fragment).launchIntent(intent);
                                } : null
                        );
                    } catch (Exception e) {
                        Log.w("SelfCheckPerms", e);
                    }
                }
            });
        }
        if (SDK_INT >= 23) {
            checks.add(new SystemChecks());
        }
//        checks.add(new NlpOsCompatChecks());
//        checks.add(new NlpStatusChecks());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        reset(LayoutInflater.from(getContext()));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        reset(LayoutInflater.from(getContext()));
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static class AsActivity extends AbstractSettingsActivity {
        public AsActivity() {
            showHomeAsUp = true;
        }

        @Override
        protected Fragment getFragment() {
            return new SelfCheckFragment();
        }
    }
}
