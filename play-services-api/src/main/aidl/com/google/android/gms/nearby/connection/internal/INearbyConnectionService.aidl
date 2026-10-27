package com.google.android.gms.nearby.connection.internal;

import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.nearby.connection.internal.INearbyConnectionCallbacks;

interface INearbyConnectionService {
    void startAdvertising(in INearbyConnectionCallbacks callbacks, in String name, in String serviceId, in IStatusCallback statusCallback) = 1;
    void stopAdvertising(in IStatusCallback statusCallback) = 2;
    void startDiscovery(in INearbyConnectionCallbacks callbacks, in String serviceId, in IStatusCallback statusCallback) = 3;
    void stopDiscovery(in IStatusCallback statusCallback) = 4;
    void requestConnection(in INearbyConnectionCallbacks callbacks, in String name, in String endpointId, in byte[] connectionInfo, in IStatusCallback statusCallback) = 5;
    void acceptConnection(in INearbyConnectionCallbacks callbacks, in String endpointId, in IStatusCallback statusCallback) = 6;
    void rejectConnection(in INearbyConnectionCallbacks callbacks, in String endpointId, in IStatusCallback statusCallback) = 7;
    void sendPayload(in List<String> endpointIds, long payloadId, in byte[] bytes, in IStatusCallback statusCallback) = 8;
    void disconnectFromEndpoint(in String endpointId, in IStatusCallback statusCallback) = 9;
    void stopAllEndpoints(in IStatusCallback statusCallback) = 10;
}
