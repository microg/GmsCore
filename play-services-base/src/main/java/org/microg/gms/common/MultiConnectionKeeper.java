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

package org.microg.gms.common;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.os.Build.VERSION_CODES.TIRAMISU;
import static org.microg.gms.common.Constants.GMS_PACKAGE_NAME;
import static org.microg.gms.common.Constants.USER_GMS_PACKAGE_NAME;

public class MultiConnectionKeeper {
    private static final String TAG = "GmsMultiConKeeper";
    private static final String PREF_MASTER = "GmsMultiConKeeper";
    private static final String PREF_TARGET = "GmsMultiConKeeper_target";

    private static MultiConnectionKeeper INSTANCE;

    private final Context context;

    private final String gmsPackage;
    private final Map<String, Connection> connections = new HashMap<String, Connection>();

    private String getApplicationName(String packageId) throws PackageManager.NameNotFoundException {
        ApplicationInfo ai;
        if (SDK_INT >= TIRAMISU) {
            ai = context.getPackageManager().getApplicationInfo(
                    packageId,
                    PackageManager.ApplicationInfoFlags.of(
                            PackageManager.GET_META_DATA
                    )
            );
        } else {
            ai = context.getPackageManager().getApplicationInfo(packageId, 0);
        }
        return (String) context.getPackageManager().getApplicationLabel(ai);
    }

    private String getTargetPackageWithoutPref() {
        // Pref: gms > microG > self
        PackageManager pm = context.getPackageManager();
        try {
            // We check the application has an activity and its name to avoid any fraudulent
            // app impersonating microG, "Cooking app" with com.google.android.gms id
            String name = getApplicationName(GMS_PACKAGE_NAME);
            if (name.contains("Google Play") || name.contains("microG")) {
                Log.d(TAG, GMS_PACKAGE_NAME + " found !");
                return GMS_PACKAGE_NAME;
            } else {
                Log.w(TAG, USER_GMS_PACKAGE_NAME + " found with another name: " + name);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, GMS_PACKAGE_NAME + " not found");
        }
        try {
            String name = getApplicationName(USER_GMS_PACKAGE_NAME);
            if (name.contains("microG")) {
                Log.d(TAG, USER_GMS_PACKAGE_NAME + " found !");
                return USER_GMS_PACKAGE_NAME;
            } else {
                Log.w(TAG, USER_GMS_PACKAGE_NAME + " found with another name: " + name);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, USER_GMS_PACKAGE_NAME + " not found");
        }
        return null;
    }

    private String getTargetPackage() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE);
        final String SELF = "SELF";
        String target;
        if ((target = prefs.getString(PREF_TARGET, null)) != null) {
            switch (target) {
                case GMS_PACKAGE_NAME:
                case USER_GMS_PACKAGE_NAME:
                    return target;
                case SELF:
                    return null;
            }
        }
        if ((target = getTargetPackageWithoutPref()) == null ) {
            prefs.edit().putString(PREF_TARGET, SELF).apply();
        } else {
            prefs.edit().putString(PREF_TARGET, target).apply();
        }
        return target;
    }

    public MultiConnectionKeeper(Context context) {
        this.context = context;
        gmsPackage = getTargetPackage();
    }

    public synchronized static MultiConnectionKeeper getInstance(Context context) {
        if (INSTANCE == null)
            INSTANCE = new MultiConnectionKeeper(context.getApplicationContext());
        return INSTANCE;
    }

    public synchronized boolean bind(String action, ServiceConnection connection) {
        return bind(action, connection, false);
    }

    public synchronized boolean bind(String action, ServiceConnection connection, boolean requireMicrog) {
        Connection con = connections.get(action);
        Log.d(TAG, "bind(" + action + ", " + connection + ", " + requireMicrog + ") has=" + (con != null));
        if (con != null) {
            if (!con.forwardsConnection(connection)) {
                con.addConnectionForward(connection);
                if (!con.isBound())
                    con.bind();
            }
        } else {
            con = new Connection(action, requireMicrog);
            con.addConnectionForward(connection);
            con.bind();
            connections.put(action, con);
        }
        Log.d(TAG, "bind() : bound=" + con.isBound());
        return con.isBound();
    }

    public synchronized void unbind(String action, ServiceConnection connection) {
        Log.d(TAG, "unbind(" + action + ", " + connection + ")");
        Connection con = connections.get(action);
        if (con != null) {
            con.removeConnectionForward(connection);
            if (con.isBound()) {
                if (!con.hasForwards()) {
                    con.unbind();
                    connections.remove(action);
                } else {
                    Log.d(TAG, "Not unbinding for " + connection + ": has pending other bindings on action " + action);
                }
            }
        }
    }

    public class Connection {
        private final String actionString;
        private final boolean requireMicrog;
        private final Set<ServiceConnection> connectionForwards = new HashSet<ServiceConnection>();
        private boolean bound = false;
        private boolean connected = false;
        private IBinder binder;
        private ComponentName component;
        private ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                Log.d(TAG, "Connection(" + actionString + ") : ServiceConnection : " +
                        "onServiceConnected(" + componentName + ")");
                binder = iBinder;
                component = componentName;
                for (ServiceConnection connection : connectionForwards) {
                    connection.onServiceConnected(componentName, iBinder);
                }
                connected = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.d(TAG, "Connection(" + actionString + ") : ServiceConnection : " +
                        "onServiceDisconnected(" + componentName + ")");
                binder = null;
                component = componentName;
                for (ServiceConnection connection : connectionForwards) {
                    connection.onServiceDisconnected(componentName);
                }
                connected = false;
                bound = false;
            }
        };

        public Connection(String actionString) {
            this(actionString, false);
        }

        public Connection(String actionString, boolean requireMicrog) {
            this.actionString = actionString;
            this.requireMicrog = requireMicrog;
        }

        private Intent getIntent() {
            Intent intent;
            ResolveInfo resolveInfo;
            if (gmsPackage != null) {
                intent = new Intent(actionString).setPackage(gmsPackage);
                if ((resolveInfo = context.getPackageManager().resolveService(intent, 0)) != null) {
                    if (requireMicrog && !isMicrog(resolveInfo)) {
                        Log.w(TAG, "GMS service found for " + actionString + " but looks not like microG");
                    } else {
                        Log.d(TAG, "GMS service found for " + actionString);
                        return intent;
                    }
                }
            }
            intent = new Intent(actionString).setPackage(context.getPackageName());
            if (context.getPackageManager().resolveService(intent, 0) != null) {
                Log.d(TAG, "Found service for " + actionString + " in self package, using it instead");
                return intent;
            }
            return null;
        }

        @SuppressLint("InlinedApi")
        public void bind() {
            Log.d(TAG, "Connection(" + actionString + ") : bind()");
            Intent intent;
            if ((intent = getIntent()) == null) {
                Log.w(TAG, "No service found for " + actionString);
                return;
            }

            int flags = Context.BIND_AUTO_CREATE | Context.BIND_DEBUG_UNBIND;
            if (SDK_INT >= ICE_CREAM_SANDWICH) {
                flags |= Context.BIND_ADJUST_WITH_ACTIVITY;
            }
            bound = context.bindService(intent, serviceConnection, flags);
            Log.d(TAG, "Connection(" + actionString + ") :  bind() : bindService=" + bound);
            if (!bound) {
                context.unbindService(serviceConnection);
            }
        }

        public boolean isMicrog(ResolveInfo resolveInfo) {
            if (resolveInfo == null || resolveInfo.serviceInfo == null) return false;
            if (resolveInfo.serviceInfo.name.startsWith("org.microg.")) return true;
            try {
                PermissionInfo info = context.getPackageManager().getPermissionInfo("org.microg.gms.EXTENDED_ACCESS", 0);
                return info.packageName.equals(resolveInfo.serviceInfo.packageName);
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }

        public boolean isBound() {
            return bound;
        }

        public IBinder getBinder() {
            return binder;
        }

        public void unbind() {
            Log.d(TAG, "Connection(" + actionString + ") : unbind()");
            try {
                context.unbindService(serviceConnection);
            } catch (IllegalArgumentException e) { // not bound (whatever reason)
                Log.w(TAG, e);
            }
            bound = false;
        }

        public void addConnectionForward(ServiceConnection connection) {
            connectionForwards.add(connection);
            if (connected) {
                connection.onServiceConnected(component, binder);
            }
        }

        public void removeConnectionForward(ServiceConnection connection) {
            connectionForwards.remove(connection);
            if (connected) {
                connection.onServiceDisconnected(component);
            }
        }

        public boolean forwardsConnection(ServiceConnection connection) {
            return connectionForwards.contains(connection);
        }

        public boolean hasForwards() {
            return !connectionForwards.isEmpty();
        }
    }
}
