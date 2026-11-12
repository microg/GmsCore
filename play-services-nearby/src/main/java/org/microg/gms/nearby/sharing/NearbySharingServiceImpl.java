/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.nearby.sharing.INearbySharingService;
import com.google.android.gms.nearby.sharing.ISharingCallback;

public class NearbySharingServiceImpl extends INearbySharingService.Stub {
    private static final String TAG = "NearbySharingService";

    private final Context context;
    private String deviceName = "microG Device";
    private int visibility = 0;
    private String currentAccount;

    public NearbySharingServiceImpl(Context context) {
        this.context = context;
    }

    @Override
    public void registerSharingProvider(ISharingCallback callback) throws RemoteException {
        Log.d(TAG, "registerSharingProvider");
        if (callback != null) {
            callback.onResult(Status.SUCCESS);
        }
    }

    @Override
    public void unregisterSharingProvider(ISharingCallback callback) throws RemoteException {
        Log.d(TAG, "unregisterSharingProvider");
        if (callback != null) {
            callback.onResult(Status.SUCCESS);
        }
    }

    @Override
    public void registerSendSurface(IBinder token, ISharingCallback callback, int mode) throws RemoteException {
        Log.d(TAG, "registerSendSurface mode: " + mode);
        if (callback != null) {
            callback.onResult(Status.SUCCESS);
        }
    }

    @Override
    public void unregisterSendSurface(IBinder token, ISharingCallback callback) throws RemoteException {
        Log.d(TAG, "unregisterSendSurface");
        if (callback != null) {
            callback.onResult(Status.SUCCESS);
        }
    }

    @Override
    public void registerReceiveSurface(IBinder token, ISharingCallback callback, int mode) throws RemoteException {
        Log.d(TAG, "registerReceiveSurface mode: " + mode);
        if (callback != null) {
            callback.onResult(Status.SUCCESS);
        }
    }

    @Override
    public void unregisterReceiveSurface(IBinder token, ISharingCallback callback) throws RemoteException {
        Log.d(TAG, "unregisterReceiveSurface");
        if (callback != null) {
            callback.onResult(Status.SUCCESS);
        }
    }

    @Override
    public void setAccount(String accountName, IStatusCallback callback) throws RemoteException {
        this.currentAccount = accountName;
        if (callback != null) {
            callback.onResult(Status.SUCCESS);
        }
    }

    @Override
    public void setDeviceName(String deviceName, IStatusCallback callback) throws RemoteException {
        if (deviceName != null && !deviceName.trim().isEmpty()) {
            this.deviceName = deviceName;
        }
        if (callback != null) {
            callback.onResult(Status.SUCCESS);
        }
    }

    @Override
    public void setVisibility(int visibility, IStatusCallback callback) throws RemoteException {
        this.visibility = visibility;
        if (callback != null) {
            callback.onResult(Status.SUCCESS);
        }
    }

    @Override
    public String getDeviceName() {
        return deviceName;
    }

    @Override
    public int getVisibility() {
        return visibility;
    }
}
