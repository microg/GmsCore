/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.connection.internal;

import android.os.ParcelFileDescriptor;

import com.google.android.gms.nearby.connection.internal.IConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.internal.IConnectionsResultCallback;
import com.google.android.gms.nearby.connection.internal.IEndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.internal.IPayloadCallback;

/**
 * Service side of the Nearby Connections API, implemented by
 * NearbySharingService in GmsCore. Every method returns immediately; the
 * outcome is reported through the result callback, and lifecycle /
 * discovery / payload events arrive through the callback interfaces.
 *
 * Notes on semantics (see ConnectionsServiceImpl):
 * - requestConnection's result fires once the connection request is on the
 *   wire (handshake done, waiting for both accept decisions). Acceptance
 *   and rejection are reported through onConnectionResult, not the Task.
 * - sendPayload's result fires when the payload is fully sent, fails with
 *   STATUS_PAYLOAD_IO_ERROR on transport failure, and reports
 *   CommonStatusCodes.CANCELED when the payload was cancelled.
 * - The engine speaks the Nearby Share wire protocol over mDNS/TCP, so
 *   advertising and discovery are scoped to that transport: the requested
 *   serviceId is echoed in DiscoveredEndpointInfo but is not used to
 *   filter mDNS results, and every strategy behaves as point-to-point.
 */
interface IConnectionsService {
    void startAdvertising(String userNickName, String serviceId, IConnectionLifecycleCallback lifecycle, IConnectionsResultCallback result);
    void stopAdvertising(IConnectionsResultCallback result);
    void startDiscovery(String serviceId, IEndpointDiscoveryCallback discovery, IConnectionsResultCallback result);
    void stopDiscovery(IConnectionsResultCallback result);
    void requestConnection(String userNickName, String endpointId, in byte[] endpointInfo, IConnectionLifecycleCallback lifecycle, IConnectionsResultCallback result);
    void acceptConnection(String endpointId, IPayloadCallback payload, IConnectionsResultCallback result);
    void rejectConnection(String endpointId, IConnectionsResultCallback result);
    void sendPayload(String endpointId, long payloadId, int payloadType, in byte[] bytes, in ParcelFileDescriptor pfd, IConnectionsResultCallback result);
    void cancelPayload(long payloadId, IConnectionsResultCallback result);
    void disconnectFromEndpoint(String endpointId);
    void stopAllEndpoints();
}
