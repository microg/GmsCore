/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.framework.tracing.wrapper;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.chimera.IntentService;

import org.microg.gms.utils.PackageManagerWrapper;
import org.microg.gms.droidguard.core.VersionUtil;

public abstract class TracingIntentService extends IntentService {
    private static final String TAG = "TracingIntentService";

    public TracingIntentService(String name) {
        super(name);
    }

    @Override
    public void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
    }

    protected abstract void a(@Nullable Intent intent);

    @Override
    public PackageManager getPackageManager() {
        return new PackageManagerWrapper(super.getPackageManager()) {
            @NonNull
            @Override
            public PackageInfo getPackageInfo(@NonNull String packageName, int flags) {
                PackageInfo packageInfo = super.getPackageInfo(packageName, flags);
                if ("com.google.android.gms".equals(packageName)) {
                    VersionUtil versionUtil = new VersionUtil(TracingIntentService.this);
                    packageInfo.versionCode = versionUtil.getVersionCode();
                    packageInfo.versionName = versionUtil.getVersionString();
                    packageInfo.sharedUserId = "com.google.uid.shared";
                }
                return packageInfo;
            }
        };
    }

    @Override
    public void onHandleIntent(@Nullable Intent intent) {
        this.a(intent);
    }
}
