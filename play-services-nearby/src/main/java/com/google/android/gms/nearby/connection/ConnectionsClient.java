/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.nearby.connection;

import com.google.android.gms.common.api.HasApiKey;
import com.google.android.gms.tasks.Task;
import org.microg.gms.common.PublicApi;

import java.util.List;

@PublicApi
public interface ConnectionsClient extends HasApiKey<com.google.android.gms.common.api.Api.ApiOptions.NoOptions> {
    Task<Void> startAdvertising(String userNickName, String serviceId, ConnectionLifecycleCallback connectionLifecycleCallback, AdvertisingOptions advertisingOptions);

    Task<Void> stopAdvertising();

    Task<Void> startDiscovery(String serviceId, EndpointDiscoveryCallback endpointDiscoveryCallback, DiscoveryOptions discoveryOptions);

    Task<Void> stopDiscovery();

    Task<Void> requestConnection(String userNickName, String endpointId, ConnectionLifecycleCallback connectionLifecycleCallback);

    Task<Void> requestConnection(byte[] endpointInfo, String endpointId, ConnectionLifecycleCallback connectionLifecycleCallback);

    Task<Void> acceptConnection(String endpointId, PayloadCallback payloadCallback);

    Task<Void> rejectConnection(String endpointId);

    Task<Void> sendPayload(String endpointId, Payload payload);

    Task<Void> sendPayload(List<String> endpointIds, Payload payload);

    Task<Void> cancelPayload(long payloadId);

    void disconnectFromEndpoint(String endpointId);

    void stopAllEndpoints();
}
