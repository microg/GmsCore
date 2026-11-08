/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.connection;

import androidx.annotation.NonNull;

import org.microg.gms.common.PublicApi;

/** Metadata of an established (or establishing) connection to a remote endpoint. */
@PublicApi
public class ConnectionInfo {
    private final String endpointName;
    private final String endpointId;
    private final boolean connected;

    public ConnectionInfo(@NonNull String endpointName, @NonNull String endpointId, boolean connected) {
        this.endpointName = endpointName;
        this.endpointId = endpointId;
        this.connected = connected;
    }

    @NonNull
    public String getEndpointName() {
        return endpointName;
    }

    @NonNull
    public String getEndpointId() {
        return endpointId;
    }

    public boolean isConnected() {
        return connected;
    }
}
