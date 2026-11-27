/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby;

import android.app.Activity;
import android.content.Context;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.List;

public class ConnectionsClientImpl extends GoogleApi<Api.ApiOptions.NoOptions> implements ConnectionsClient {
    private static final Api<Api.ApiOptions.NoOptions> API = new Api<>((options, context, looper, clientSettings, callbacks, connectionFailedListener) -> null);

    public ConnectionsClientImpl(Context context) {
        super(context, API, Api.ApiOptions.NO_OPTIONS);
    }

    public ConnectionsClientImpl(Activity activity) {
        super(activity, API, Api.ApiOptions.NO_OPTIONS);
    }

    @Override
    public Task<Void> startAdvertising(String userNickName, String serviceId, ConnectionLifecycleCallback connectionLifecycleCallback, AdvertisingOptions advertisingOptions) {
        return Tasks.forResult(null);
    }

    @Override
    public Task<Void> stopAdvertising() {
        return Tasks.forResult(null);
    }

    @Override
    public Task<Void> startDiscovery(String serviceId, EndpointDiscoveryCallback endpointDiscoveryCallback, DiscoveryOptions discoveryOptions) {
        return Tasks.forResult(null);
    }

    @Override
    public Task<Void> stopDiscovery() {
        return Tasks.forResult(null);
    }

    @Override
    public Task<Void> requestConnection(String userNickName, String endpointId, ConnectionLifecycleCallback connectionLifecycleCallback) {
        return Tasks.forResult(null);
    }

    @Override
    public Task<Void> requestConnection(byte[] endpointInfo, String endpointId, ConnectionLifecycleCallback connectionLifecycleCallback) {
        return Tasks.forResult(null);
    }

    @Override
    public Task<Void> acceptConnection(String endpointId, PayloadCallback payloadCallback) {
        return Tasks.forResult(null);
    }

    @Override
    public Task<Void> rejectConnection(String endpointId) {
        return Tasks.forResult(null);
    }

    @Override
    public Task<Void> sendPayload(String endpointId, Payload payload) {
        return Tasks.forResult(null);
    }

    @Override
    public Task<Void> sendPayload(List<String> endpointIds, Payload payload) {
        return Tasks.forResult(null);
    }

    @Override
    public Task<Void> cancelPayload(long payloadId) {
        return Tasks.forResult(null);
    }

    @Override
    public void disconnectFromEndpoint(String endpointId) {
    }

    @Override
    public void stopAllEndpoints() {
    }
}
