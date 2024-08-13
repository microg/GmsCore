/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;

import com.google.android.gms.common.stats.ConnectionTracker;

import java.util.HashSet;
import java.util.Set;

public class GmsClientServiceConnection implements ServiceConnection {
    private static final String TAG = "ClientServiceConnection";
    private final Set<ServiceConnection> serviceConnections;
    private int mState;
    private boolean connected;
    private IBinder serviceBinder;
    private final GmsClientSupervisor.ServiceInfo serviceInfo;
    private ComponentName mComponentName;
    private final GmsClientSupervisorImpl gmsClientSupervisor;

    public GmsClientServiceConnection(GmsClientSupervisorImpl gmsClientSupervisor, GmsClientSupervisor.ServiceInfo serviceInfo) {
        this.gmsClientSupervisor = gmsClientSupervisor;
        this.serviceInfo = serviceInfo;
        this.serviceConnections = new HashSet<>();
        this.mState = 2;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d(TAG, "onServiceConnected: componentName " + componentName );
        synchronized (GmsClientSupervisor.lock) {
            gmsClientSupervisor.mHandler.removeMessages(1, this.serviceInfo);
            this.serviceBinder = iBinder;
            this.mComponentName = componentName;
            for (ServiceConnection serviceConnection : this.serviceConnections) {
                serviceConnection.onServiceConnected(componentName, iBinder);
            }
            this.mState = 1;
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.d(TAG, "onServiceDisconnected: componentName " + componentName );
        synchronized (GmsClientSupervisor.lock) {
            gmsClientSupervisor.mHandler.removeMessages(1, this.serviceInfo);
            this.serviceBinder = null;
            this.mComponentName = componentName;
            for (ServiceConnection serviceConnection : this.serviceConnections) {
                serviceConnection.onServiceDisconnected(componentName);
            }
            this.mState = 2;
        }
    }

    @Override
    public final void onBindingDied(ComponentName componentName) {
        onServiceDisconnected(componentName);
    }

    public void bindService(String tag) {
        this.mState = 3;
        StrictMode.VmPolicy vmPolicy = StrictMode.getVmPolicy();
        try {
            ConnectionTracker connectionTracker = gmsClientSupervisor.connectionTracker;
            Context context = gmsClientSupervisor.context;
            GmsClientSupervisor.ServiceInfo info = this.serviceInfo;
            Intent serviceIntent = info.getServiceIntent();
            boolean connect = connectionTracker.bindService(context, tag, serviceIntent, this, Context.BIND_AUTO_CREATE);
            this.connected = connect;
            Log.d(tag, "bindService: connected: " + connected);
            if (connect) {
                this.gmsClientSupervisor.mHandler.sendMessageDelayed(this.gmsClientSupervisor.mHandler.obtainMessage(1, info), this.gmsClientSupervisor.delayTime);
            } else {
                this.mState = 2;
                connectionTracker.unbindService(context, this);
            }
        } finally {
            StrictMode.setVmPolicy(vmPolicy);
        }
    }

    public void unbindService(String tag) {
        gmsClientSupervisor.mHandler.removeMessages(1, this.serviceInfo);
        gmsClientSupervisor.unbindService(this.serviceInfo, this, tag);
        this.connected = false;
        this.mState = 2;
    }

    public final void addServiceConnection(ServiceConnection connection, String tag) {
        Log.d(tag, "addServiceConnection: " + connection);
        this.serviceConnections.add(connection);
    }

    public final void removeServiceConnection(ServiceConnection connection, String tag) {
        Log.d(tag, "removeServiceConnection: " + connection);
        this.serviceConnections.remove(connection);
    }

    public boolean isBound() {
        return this.connected;
    }

    public int getState() {
        return this.mState;
    }

    public boolean serviceConnected(ServiceConnection connection) {
        return this.serviceConnections.contains(connection);
    }

    public boolean serviceConnectionStatus() {
        return this.serviceConnections.isEmpty();
    }

    public IBinder getBinder() {
        return this.serviceBinder;
    }

    public ComponentName getComponentName() {
        return this.mComponentName;
    }
}