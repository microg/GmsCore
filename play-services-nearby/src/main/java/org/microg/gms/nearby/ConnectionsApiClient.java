/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby;

import android.content.Context;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import com.google.android.gms.nearby.connection.internal.IConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.internal.IConnectionsResultCallback;
import com.google.android.gms.nearby.connection.internal.IConnectionsService;
import com.google.android.gms.nearby.connection.internal.IEndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.internal.IPayloadCallback;

import org.microg.gms.common.GmsClient;
import org.microg.gms.common.GmsService;
import com.google.android.gms.common.api.internal.ConnectionCallbacks;
import com.google.android.gms.common.api.internal.OnConnectionFailedListener;

/**
 * GmsClient for the Nearby Connections service (GmsService.NEARBY_CONNECTIONS),
 * implemented by NearbySharingService in GmsCore. Follows the same pattern as
 * ExposureNotificationApiClient: thin pass-throughs over the AIDL interface.
 */
public class ConnectionsApiClient extends GmsClient<IConnectionsService> {
    public ConnectionsApiClient(Context context, ConnectionCallbacks callbacks, OnConnectionFailedListener connectionFailedListener) {
        super(context, callbacks, connectionFailedListener, GmsService.NEARBY_CONNECTIONS.ACTION);
        serviceId = GmsService.NEARBY_CONNECTIONS.SERVICE_ID;
        requireMicrog = true;
    }

    @Override
    protected IConnectionsService interfaceFromBinder(IBinder binder) {
        return IConnectionsService.Stub.asInterface(binder);
    }

    public void startAdvertising(String userNickName, String serviceId, IConnectionLifecycleCallback lifecycle, IConnectionsResultCallback result) throws RemoteException {
        getServiceInterface().startAdvertising(userNickName, serviceId, lifecycle, result);
    }

    public void stopAdvertising(IConnectionsResultCallback result) throws RemoteException {
        getServiceInterface().stopAdvertising(result);
    }

    public void startDiscovery(String serviceId, IEndpointDiscoveryCallback discovery, IConnectionsResultCallback result) throws RemoteException {
        getServiceInterface().startDiscovery(serviceId, discovery, result);
    }

    public void stopDiscovery(IConnectionsResultCallback result) throws RemoteException {
        getServiceInterface().stopDiscovery(result);
    }

    public void requestConnection(String userNickName, String endpointId, byte[] endpointInfo, IConnectionLifecycleCallback lifecycle, IConnectionsResultCallback result) throws RemoteException {
        getServiceInterface().requestConnection(userNickName, endpointId, endpointInfo, lifecycle, result);
    }

    public void acceptConnection(String endpointId, IPayloadCallback payload, IConnectionsResultCallback result) throws RemoteException {
        getServiceInterface().acceptConnection(endpointId, payload, result);
    }

    public void rejectConnection(String endpointId, IConnectionsResultCallback result) throws RemoteException {
        getServiceInterface().rejectConnection(endpointId, result);
    }

    public void sendPayload(String endpointId, long payloadId, int payloadType, byte[] bytes, ParcelFileDescriptor pfd, IConnectionsResultCallback result) throws RemoteException {
        getServiceInterface().sendPayload(endpointId, payloadId, payloadType, bytes, pfd, result);
    }

    public void cancelPayload(long payloadId, IConnectionsResultCallback result) throws RemoteException {
        getServiceInterface().cancelPayload(payloadId, result);
    }

    public void disconnectFromEndpoint(String endpointId) throws RemoteException {
        getServiceInterface().disconnectFromEndpoint(endpointId);
    }

    public void stopAllEndpoints() throws RemoteException {
        getServiceInterface().stopAllEndpoints();
    }
}
