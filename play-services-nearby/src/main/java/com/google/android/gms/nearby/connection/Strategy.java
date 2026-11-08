/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.connection;

import org.microg.gms.common.PublicApi;

/**
 * Topology of the connection graph formed between endpoints of a service.
 */
@PublicApi
public enum Strategy {
    /** Every endpoint can talk to every other endpoint. */
    P2P_CLUSTER(1),
    /** One endpoint is the center of the graph, all others connect to it. */
    P2P_STAR(2),
    /** Endpoints only connect to the endpoint they discovered. */
    P2P_POINT_TO_POINT(3);

    private final int value;

    Strategy(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
