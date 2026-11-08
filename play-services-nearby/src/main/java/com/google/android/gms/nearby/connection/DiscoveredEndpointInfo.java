/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.connection;

import androidx.annotation.NonNull;

import org.microg.gms.common.PublicApi;

/** Description of an endpoint that was discovered nearby. */
@PublicApi
public class DiscoveredEndpointInfo {
    private final String endpointName;
    private final String serviceId;

    public DiscoveredEndpointInfo(@NonNull String endpointName, @NonNull String serviceId) {
        this.endpointName = endpointName;
        this.serviceId = serviceId;
    }

    @NonNull
    public String getEndpointName() {
        return endpointName;
    }

    @NonNull
    public String getServiceId() {
        return serviceId;
    }
}
