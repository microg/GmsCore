/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.internal.GetServiceRequest;
import com.google.android.gms.common.internal.IGmsCallbacks;
import com.google.android.gms.wearable.internal.IWearableService;

import org.microg.gms.BaseService;
import org.microg.gms.common.GmsService;
import org.microg.gms.common.PackageUtils;

/**
 * Service handling Wearable API requests.
 * This is the entry point for the WearOS companion app on the phone.
 */
public class WearableService extends BaseService {
    private static final String TAG = "GmsWearSvc";

    public WearableService() {
        super(TAG, GmsService.WEARABLE);
    }

    @Override
    public void handleServiceRequest(IGmsCallbacks callback, GetServiceRequest request, GmsService service) throws RemoteException {
        String packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName);
        callback.onPostInitComplete(0, new WearableServiceImpl(packageName).asBinder(), null);
    }

    private class WearableServiceImpl extends IWearableService.Stub {
        private final String packageName;

        public WearableServiceImpl(String packageName) {
            this.packageName = packageName;
        }

        @Override
        public void putData(IBinder callbacks, android.net.Uri uri, byte[] data, Bundle bundle) throws RemoteException {
            Log.d(TAG, "putData: " + uri + " from " + packageName);
        }

        @Override
        public void getDataItem(IBinder callbacks, android.net.Uri uri) throws RemoteException {
            Log.d(TAG, "getDataItem: " + uri + " from " + packageName);
        }

        @Override
        public void deleteDataItems(IBinder callbacks, android.net.Uri uri) throws RemoteException {
            Log.d(TAG, "deleteDataItems: " + uri + " from " + packageName);
        }

        @Override
        public void sendMessage(IBinder callbacks, String nodeId, String path, byte[] data) throws RemoteException {
            Log.d(TAG, "sendMessage: " + path + " to " + nodeId + " from " + packageName);
        }

        @Override
        public void getLocalNode(IBinder callbacks) throws RemoteException {
            Log.d(TAG, "getLocalNode from " + packageName);
        }

        @Override
        public void getConnectedNodes(IBinder callbacks) throws RemoteException {
            Log.d(TAG, "getConnectedNodes from " + packageName);
        }

        @Override
        public void addListener(IBinder callbacks, Bundle filter) throws RemoteException {
            Log.d(TAG, "addListener from " + packageName);
        }

        @Override
        public void removeListener(IBinder callbacks, IBinder listener) throws RemoteException {
            Log.d(TAG, "removeListener from " + packageName);
        }
    }
}