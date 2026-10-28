/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.connection.internal;

/**
 * Result of a Connections operation that carries no payload: the Task
 * returned to the app completes (STATUS_OK) or fails (any other status
 * code, surfaced as an ApiException) when this callback fires.
 */
interface IConnectionsResultCallback {
    void onResult(int statusCode);
}
