/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.connection;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.Status;

import org.microg.gms.common.PublicApi;

/** Result of a successful {@link ConnectionsClient#startAdvertising} call. */
@PublicApi
public class StartAdvertisingResult {
    private final Status status;
    private final String localEndpointName;

    public StartAdvertisingResult(@NonNull Status status, @NonNull String localEndpointName) {
        this.status = status;
        this.localEndpointName = localEndpointName;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    @NonNull
    public String getLocalEndpointName() {
        return localEndpointName;
    }
}
