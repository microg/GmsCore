/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.nearby.connection;

import org.microg.gms.common.PublicApi;

import java.util.Arrays;

@PublicApi
public class DiscoveredEndpointInfo {
    private final String serviceId;
    private final String endpointName;
    private final byte[] endpointInfo;

    public DiscoveredEndpointInfo(String serviceId, String endpointName) {
        this(serviceId, endpointName, endpointName != null ? endpointName.getBytes() : null);
    }

    public DiscoveredEndpointInfo(String serviceId, byte[] endpointInfo) {
        this(serviceId, endpointInfo != null ? new String(endpointInfo) : "", endpointInfo);
    }

    public DiscoveredEndpointInfo(String serviceId, String endpointName, byte[] endpointInfo) {
        this.serviceId = serviceId;
        this.endpointName = endpointName;
        this.endpointInfo = endpointInfo;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public byte[] getEndpointInfo() {
        return endpointInfo != null ? Arrays.copyOf(endpointInfo, endpointInfo.length) : null;
    }
}
