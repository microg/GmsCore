/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.connection.internal;

/**
 * Service-side counterpart of EndpointDiscoveryCallback. Delivered on a
 * Binder thread; the client forwards to the app callback directly.
 */
interface IEndpointDiscoveryCallback {
    void onEndpointFound(String endpointId, String serviceId, String endpointName, in byte[] endpointInfo);
    void onEndpointLost(String endpointId);
}
