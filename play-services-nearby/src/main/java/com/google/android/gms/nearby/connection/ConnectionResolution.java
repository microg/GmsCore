/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.connection;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.Status;

import org.microg.gms.common.PublicApi;

/** Result of a connection attempt, delivered to {@link ConnectionLifecycleCallback}. */
@PublicApi
public class ConnectionResolution {
    private final Status status;

    public ConnectionResolution(@NonNull Status status) {
        this.status = status;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }
}
