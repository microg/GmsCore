/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.connection;

import androidx.annotation.NonNull;

import org.microg.gms.common.PublicApi;

import java.util.Locale;

/** Identifier of a remote endpoint in a {@link ConnectionsClient} session. */
@PublicApi
public class EndpointId {
    private final String id;

    public EndpointId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public static EndpointId fromString(@NonNull String id) {
        return new EndpointId(id);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EndpointId && id.equals(((EndpointId) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "EndpointId(%s)", id);
    }
}
