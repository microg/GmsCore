/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.connection;

import androidx.annotation.NonNull;

import org.microg.gms.common.PublicApi;

/** Callback invoked when endpoints advertising a service appear and disappear nearby. */
@PublicApi
public abstract class EndpointDiscoveryCallback {
    public abstract void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo info);

    public abstract void onEndpointLost(@NonNull String endpointId);
}
