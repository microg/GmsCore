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
public class ConnectionInfo {
    private final String endpointName;
    private final String authenticationToken;
    private final byte[] endpointInfo;
    private final boolean isIncomingConnection;

    public ConnectionInfo(String endpointName, String authenticationToken, boolean isIncomingConnection) {
        this(endpointName, authenticationToken, endpointName != null ? endpointName.getBytes() : null, isIncomingConnection);
    }

    public ConnectionInfo(String endpointName, String authenticationToken, byte[] endpointInfo, boolean isIncomingConnection) {
        this.endpointName = endpointName;
        this.authenticationToken = authenticationToken;
        this.endpointInfo = endpointInfo;
        this.isIncomingConnection = isIncomingConnection;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public String getAuthenticationToken() {
        return authenticationToken;
    }

    public byte[] getEndpointInfo() {
        return endpointInfo != null ? Arrays.copyOf(endpointInfo, endpointInfo.length) : null;
    }

    public boolean isIncomingConnection() {
        return isIncomingConnection;
    }
}
