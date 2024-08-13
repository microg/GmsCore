/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.GuardedBy;

import com.google.android.gms.common.stats.ConnectionTracker;

import java.util.HashMap;

class GmsClientSupervisorImpl extends GmsClientSupervisor implements Handler.Callback {
    private final String TAG = "GmsClientSupervisor";
    @GuardedBy("mConnectionStatus")
    private final HashMap<ServiceInfo, GmsClientServiceConnection> serviceConnectionHashMap = new HashMap<>();
    public final Context context;
    public final Handler mHandler;
    public final ConnectionTracker connectionTracker;
    public final long delayTime;

    GmsClientSupervisorImpl(Context context) {
        this.context = context;
        this.mHandler = new Handler(context.getMainLooper(), this);
        this.connectionTracker = ConnectionTracker.getInstance();
        this.delayTime = 5000L;
    }

    @Override
    protected boolean bindService(ServiceInfo serviceInfo, ServiceConnection connection, String tag) {
        Preconditions.checkNotNull(connection, "ServiceConnection must not be null");
        Log.d(tag, "bindService: serviceInfo " + serviceInfo + " " + serviceInfo.hashCode());
        synchronized (this.serviceConnectionHashMap) {
            GmsClientServiceConnection serviceConnection;
            if ((serviceConnection = this.serviceConnectionHashMap.get(serviceInfo)) == null) {
                Log.d(tag, "bindService: start 1111");
                (serviceConnection = new GmsClientServiceConnection(this, serviceInfo)).addServiceConnection(connection, tag);
                serviceConnection.bindService(tag);
                this.serviceConnectionHashMap.put(serviceInfo, serviceConnection);
            } else {
                Log.d(tag, "bindService: start 2222");
                this.mHandler.removeMessages(0, serviceInfo);
                if (serviceConnection.serviceConnected(connection)) {
                    String info = String.valueOf(serviceInfo);
                    throw new IllegalStateException("Trying to bind a GmsServiceConnection that was already connected before.  config=" + info);
                }
                serviceConnection.addServiceConnection(connection, tag);
                switch (serviceConnection.getState()) {
                    case 1:
                        connection.onServiceConnected(serviceConnection.getComponentName(), serviceConnection.getBinder());
                        break;
                    case 2:
                        serviceConnection.bindService(tag);
                }
            }

            return serviceConnection.isBound();
        }
    }

    @Override
    protected void unbindService(ServiceInfo serviceInfo, ServiceConnection connection, String tag) {
        Preconditions.checkNotNull(connection, "ServiceConnection must not be null");
        synchronized (this.serviceConnectionHashMap) {
            Log.d(tag, "unbindService: serviceInfo " + serviceInfo + " " + serviceInfo.hashCode());
            GmsClientServiceConnection serviceConnection;
            String info;
            if ((serviceConnection = this.serviceConnectionHashMap.get(serviceInfo)) == null) {
                info = String.valueOf(serviceInfo);
                Log.w(TAG, "unbindService: " + "Nonexistent connection status for service config: " + info);
            } else if (!serviceConnection.serviceConnected(connection)) {
                info = String.valueOf(serviceInfo);
                Log.w(TAG, "unbindService: " + "Trying to unbind a GmsServiceConnection  that was not bound before.  config=" + info);
            } else {
                serviceConnection.removeServiceConnection(connection, tag);
                if (serviceConnection.serviceConnectionStatus()) {
                    Message var6 = this.mHandler.obtainMessage(0, serviceInfo);
                    this.mHandler.sendMessageDelayed(var6, this.delayTime);
                }
            }
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        ServiceInfo serviceInfo;
        GmsClientServiceConnection serviceConnection;
        switch (msg.what) {
            case 0:
                synchronized (this.serviceConnectionHashMap) {
                    serviceInfo = (ServiceInfo) msg.obj;
                    if ((serviceConnection = this.serviceConnectionHashMap.get(serviceInfo)) != null && serviceConnection.serviceConnectionStatus()) {
                        if (serviceConnection.isBound()) {
                            serviceConnection.unbindService(TAG);
                        }
                        this.serviceConnectionHashMap.remove(serviceInfo);
                    }
                    return true;
                }
            case 1:
                synchronized (this.serviceConnectionHashMap) {
                    serviceInfo = (ServiceInfo) msg.obj;
                    if ((serviceConnection = this.serviceConnectionHashMap.get(serviceInfo)) != null && serviceConnection.getState() == 3) {
                        String info = String.valueOf(serviceInfo);
                        Log.e(TAG, "Timeout waiting for ServiceConnection callback " + info, new Exception());
                        ComponentName componentName;
                        if ((componentName = serviceConnection.getComponentName()) == null) {
                            componentName = serviceInfo.getComponentName();
                        }
                        if (componentName == null) {
                            componentName = new ComponentName(serviceInfo.getPackage(), "unknown");
                        }
                        serviceConnection.onServiceDisconnected(componentName);
                    }
                    return true;
                }
            default:
                return false;
        }
    }
}

