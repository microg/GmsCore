/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby;

import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;

import java.util.List;

/**
 * Transport behind ConnectionsClientImpl: the seam between the public
 * Connections API and whatever actually performs the work. Production uses
 * the GmsCore service (see ServiceTransport in ConnectionsClientImpl);
 * unit tests inject a fake. Every operation reports its real outcome
 * through ResultCallback; a transport must never report success for work
 * it did not perform.
 */
interface ConnectionsTransport {
    interface ResultCallback {
        void onResult(int statusCode);
    }

    void startAdvertising(String userNickName, String serviceId, ConnectionLifecycleCallback lifecycle, AdvertisingOptions options, ResultCallback result);

    void stopAdvertising(ResultCallback result);

    void startDiscovery(String serviceId, EndpointDiscoveryCallback discovery, DiscoveryOptions options, ResultCallback result);

    void stopDiscovery(ResultCallback result);

    void requestConnection(String userNickName, String endpointId, byte[] endpointInfo, ConnectionLifecycleCallback lifecycle, ResultCallback result);

    void acceptConnection(String endpointId, PayloadCallback payload, ResultCallback result);

    void rejectConnection(String endpointId, ResultCallback result);

    void sendPayload(String endpointId, Payload payload, ResultCallback result);

    void sendPayload(List<String> endpointIds, Payload payload, ResultCallback result);

    void cancelPayload(long payloadId, ResultCallback result);

    void disconnectFromEndpoint(String endpointId);

    void stopAllEndpoints();
}
