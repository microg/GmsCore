/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.nearby.connection;

import org.microg.gms.common.PublicApi;

@PublicApi
public abstract class ConnectionLifecycleCallback {
    public abstract void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo);

    public abstract void onConnectionResult(String endpointId, ConnectionResolution resolution);

    public abstract void onDisconnected(String endpointId);
}
