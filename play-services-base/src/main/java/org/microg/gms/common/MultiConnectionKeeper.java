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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static org.microg.gms.common.Constants.GMS_PACKAGE_NAME;

public class MultiConnectionKeeper {
    private static final String TAG = "GmsMultiConKeeper";

    private static MultiConnectionKeeper INSTANCE;

    private final Context context;
    private final Map<String, Connection> connections = new HashMap<String, Connection>();

    public MultiConnectionKeeper(Context context) {
        this.context = context;
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

        @SuppressLint("InlinedApi")
        public void bind() {
            Log.d(TAG, "Connection(" + actionString + ") : bind()");
            Intent gmsIntent = new Intent(actionString).setPackage(GMS_PACKAGE_NAME);
            Intent selfIntent = new Intent(actionString).setPackage(context.getPackageName());
            Intent intent;
            ResolveInfo resolveInfo;
            if ((resolveInfo = context.getPackageManager().resolveService(gmsIntent, 0)) == null) {
                Log.w(TAG, "No GMS service found for " + actionString);
                if (context.getPackageManager().resolveService(selfIntent, 0) != null) {
                    Log.d(TAG, "Found service for " + actionString + " in self package, using it instead");
                    intent = selfIntent;
                } else {
                    return;
                }
            } else if (requireMicrog && !isMicrog(resolveInfo)) {
                Log.w(TAG, "GMS service found for " + actionString + " but looks not like microG");
                if (context.getPackageManager().resolveService(selfIntent, 0) != null) {
                    Log.d(TAG, "Found service for " + actionString + " in self package, using it instead");
                    intent = selfIntent;
                } else {
                    intent = gmsIntent;
                }
            } else {
                intent = gmsIntent;
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
