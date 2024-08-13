/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.HandlerThread;

import java.util.Objects;

public abstract class GmsClientSupervisor {
    public static final Object lock = new Object();
    protected HandlerThread handlerThread;
    private static GmsClientSupervisor instance;

    public GmsClientSupervisor() {
        if (handlerThread == null) {
            handlerThread = new HandlerThread("GoogleApi");
        }
        handlerThread.start();
    }

    public static GmsClientSupervisor getInstance(Context context) {
        synchronized (lock) {
            if (instance == null) {
                instance = new GmsClientSupervisorImpl(context.getApplicationContext());
            }
        }
        return instance;
    }

    public boolean bindService(String action, ServiceConnection connection, String tag) {
        return this.bindService(new ServiceInfo(action), connection, tag);
    }

    public boolean bindService(ComponentName componentName, ServiceConnection connection, String tag) {
        return this.bindService(new ServiceInfo(componentName), connection, tag);
    }

    public void unbindService(String action, ServiceConnection connection, String tag) {
        this.unbindService(new ServiceInfo(action), connection, tag);
    }

    public void unbindService(ComponentName componentName, ServiceConnection connection, String tag) {
        this.unbindService(new ServiceInfo(componentName), connection, tag);
    }

    protected abstract boolean bindService(ServiceInfo serviceInfo, ServiceConnection connection, String tag);

    protected abstract void unbindService(ServiceInfo serviceInfo, ServiceConnection connection, String tag);

    protected static final class ServiceInfo {
        private final String action;
        private final String packageName;
        private final ComponentName mComponentName;

        public ServiceInfo(String action) {
            this.action = Preconditions.checkNotEmpty(action);
            this.packageName = "com.google.android.gms";
            this.mComponentName = null;
        }

        public ServiceInfo(ComponentName componentName) {
            this.action = null;
            this.packageName = "com.google.android.gms";
            this.mComponentName = Preconditions.checkNotNull(componentName);
        }

        @Override
        public String toString() {
            return this.action == null ? this.mComponentName.flattenToString() : this.action;
        }

        public String getAction() {
            return action;
        }

        public String getPackage() {
            return this.packageName;
        }

        public ComponentName getComponentName() {
            return this.mComponentName;
        }

        public Intent getServiceIntent() {
            Intent intent;
            if (this.action != null) {
                intent = (new Intent(this.action)).setPackage(this.packageName);
            } else {
                intent = (new Intent()).setComponent(this.mComponentName);
            }
            return intent;
        }

        @Override
        public int hashCode() {
            if (action != null) {
                return action.hashCode();
            }
            return this.mComponentName.flattenToString().hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (!(object instanceof ServiceInfo)) {
                return false;
            } else {
                ServiceInfo target = (ServiceInfo) object;
                if (this.action != null) {
                    return Objects.equals(this.action, target.action);
                }
                return Objects.equals(this.mComponentName.flattenToString(), target.mComponentName.flattenToString());
            }
        }
    }
}
