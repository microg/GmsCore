/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.connection;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;

import org.microg.gms.common.PublicApi;

/**
 * Entry point for peer-to-peer connections between devices running the same service, as used by
 * Nearby Share / Quick Share.
 */
@PublicApi
public interface ConnectionsClient {
    @NonNull
    Task<Void> startAdvertising(@NonNull String serviceId, @NonNull String localEndpointName,
                                @NonNull ConnectionLifecycleCallback callback,
                                @NonNull AdvertisingOptions options);

    @NonNull
    Task<Void> startDiscovery(@NonNull String serviceId,
                              @NonNull EndpointDiscoveryCallback callback,
                              @NonNull DiscoveryOptions options);

    @NonNull
    Task<Void> stopAdvertising();

    @NonNull
    Task<Void> stopDiscovery();

    @NonNull
    Task<Void> stopAllEndpoints();
}
