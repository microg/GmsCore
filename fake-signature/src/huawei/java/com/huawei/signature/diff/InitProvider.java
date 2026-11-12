/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.huawei.signature.diff;

import android.app.ActivityManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.util.List;

public class InitProvider extends ContentProvider {
    private static final String TAG = "InitProvider";

    @Override
    public boolean onCreate() {
        Log.d(TAG, "onCreate");
        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "Context unavailable during provider initialization");
            return false;
        }

        startSignatureServiceIfNeeded(context);

        // ContentProvider.onCreate() must return true when initialization succeeds.
        // Returning false can cause Android to treat the provider as failed and may
        // discard side effects needed by the fake-signature mechanism.
        return true;
    }

    private void startSignatureServiceIfNeeded(Context context) {
        if (context == null) {
            return;
        }
        String packageName = context.getPackageName();
        if (packageName == null || packageName.isEmpty()) {
            return;
        }
        if (isServiceRunning(context, packageName, SignatureService.class.getName())) {
            return;
        }

        Intent intent = new Intent(context, SignatureService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (IllegalStateException | SecurityException e) {
            Log.w(TAG, "Failed to start SignatureService", e);
        } catch (RuntimeException e) {
            Log.w(TAG, "Unexpected failure starting SignatureService", e);
        }
    }

    private boolean isServiceRunning(Context context, String packageName, String serviceName) {
        if (context == null || packageName == null || serviceName == null) {
            return false;
        }
        try {
            Object service = context.getSystemService(Context.ACTIVITY_SERVICE);
            if (!(service instanceof ActivityManager)) {
                return false;
            }
            ActivityManager manager = (ActivityManager) service;

            // Deprecated/restricted on newer Android versions, but kept as a
            // best-effort guard to avoid redundant start requests on older devices.
            @SuppressWarnings("deprecation")
            List<ActivityManager.RunningServiceInfo> serviceInfoList =
                    manager.getRunningServices(Integer.MAX_VALUE);
            if (serviceInfoList == null || serviceInfoList.isEmpty()) {
                return false;
            }
            for (ActivityManager.RunningServiceInfo info : serviceInfoList) {
                if (info == null || info.service == null) {
                    continue;
                }
                if (packageName.equals(info.service.getPackageName())
                        && serviceName.equals(info.service.getClassName())) {
                    return true;
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Unable to query running services", e);
        } catch (RuntimeException e) {
            Log.w(TAG, "Unexpected error while checking service state", e);
        }
        return false;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}