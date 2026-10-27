/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.nearby.connection;

import org.microg.gms.common.PublicApi;

import java.util.Locale;
import java.util.Objects;

@PublicApi
public final class Strategy {
    public static final Strategy P2P_CLUSTER = new Strategy(1, 3);
    public static final Strategy P2P_STAR = new Strategy(1, 2);
    public static final Strategy P2P_POINT_TO_POINT = new Strategy(1, 1);

    private final int connectionType;
    private final int topology;

    private Strategy(int connectionType, int topology) {
        this.connectionType = connectionType;
        this.topology = topology;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Strategy strategy = (Strategy) o;
        return connectionType == strategy.connectionType && topology == strategy.topology;
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectionType, topology);
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "Strategy(%d, %d)", connectionType, topology);
    }
}
