package com.google.android.gms.nearby.connection.internal;

import com.google.android.gms.common.api.Status;

interface INearbyConnectionCallbacks {
    void onConnectionInitiated(in String endpointId, in byte[] endpointInfo) = 1;
    void onConnectionResult(in String endpointId, in Status status) = 2;
    void onDisconnected(in String endpointId) = 3;
    void onEndpointFound(in String endpointId, in String serviceId, in String endpointName) = 4;
    void onEndpointLost(in String endpointId) = 5;
    void onPayloadReceived(in String endpointId, long payloadId, in byte[] bytes) = 6;
    void onPayloadTransferUpdate(in String endpointId, long payloadId, int status, long bytesTransferred, long totalBytes) = 7;
}
