/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.stats;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public class ConnectionTracker {
    private static final Object lock = new Object();
    private static volatile ConnectionTracker instance;

    public static ConnectionTracker getInstance() {
        if (instance == null) {
            synchronized(lock) {
                if (instance == null) {
                    instance = new ConnectionTracker();
                }
            }
        }
        return instance;
    }

    public final boolean bindService(Context context, String className, Intent intent, ServiceConnection connection, int flags) {
        ComponentName componentName;
        if ((componentName = intent.getComponent()) != null && checkPackageLive(context, componentName.getPackageName())) {
            Log.w("ConnectionTracker", "Attempted to bind to a service in a STOPPED package.");
            return false;
        } else {
            Log.d("ConnectionTracker", "bindService: " + intent + " ServiceConnection: " + connection);
            return context.bindService(intent, connection, flags);
        }
    }

    public boolean bindService(Context context, Intent intent, ServiceConnection connection, int flag) {
        return this.bindService(context, context.getClass().getName(), intent, connection, flag);
    }

    public void unbindService(Context context, ServiceConnection connection) {
        context.unbindService(connection);
    }

    private boolean checkPackageLive(Context context, String packageName) {
        try {
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(packageName, 0);
            return (applicationInfo.flags & 2097152) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
