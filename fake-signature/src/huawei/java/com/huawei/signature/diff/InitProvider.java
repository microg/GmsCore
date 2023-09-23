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
import android.util.Log;
import java.util.List;

public class InitProvider extends ContentProvider {
    private static final String TAG = "InitProvider";

    @Override
    public boolean onCreate() {
        Log.d(TAG, "onCreate");
        if (!isServiceRunning(getContext(), getContext().getPackageName(), SignatureService.class.getName())) {
            Intent intent = new Intent(getContext(), SignatureService.class);
            try {
                getContext().startService(intent);
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private boolean isServiceRunning(Context context, String packageName, String serviceName) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceInfoList = manager.getRunningServices(Integer.MAX_VALUE);
        if (serviceInfoList == null) {
            return false;
        }
        for (ActivityManager.RunningServiceInfo info : serviceInfoList) {
            if (info.service.getPackageName().equals(packageName) && info.service.getClassName().equals(serviceName)) {
                return true;
            }
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