/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.connection;

import androidx.annotation.NonNull;

import org.microg.gms.common.PublicApi;

/** Callback invoked when connections to endpoints are initiated, established or lost. */
@PublicApi
public abstract class ConnectionLifecycleCallback {
    public abstract void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo info);

    public abstract void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution result);

    public abstract void onDisconnected(@NonNull String endpointId);
}
