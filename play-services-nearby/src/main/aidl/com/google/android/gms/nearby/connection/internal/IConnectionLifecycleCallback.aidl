/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.connection.internal;

/**
 * Service-side counterpart of ConnectionLifecycleCallback. Delivered on a
 * Binder thread; the client forwards to the app callback directly.
 */
interface IConnectionLifecycleCallback {
    void onConnectionInitiated(String endpointId, String endpointName, String authenticationToken, in byte[] endpointInfo, boolean isIncoming);
    void onConnectionResult(String endpointId, int statusCode);
    void onDisconnected(String endpointId);
}
