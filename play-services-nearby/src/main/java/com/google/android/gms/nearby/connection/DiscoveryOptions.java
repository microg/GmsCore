/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.connection;

import androidx.annotation.NonNull;

import org.microg.gms.common.PublicApi;

/** Options for {@link ConnectionsClient#startDiscovery}. */
@PublicApi
public class DiscoveryOptions {
    private final Strategy strategy;

    private DiscoveryOptions(Strategy strategy) {
        this.strategy = strategy;
    }

    @NonNull
    public Strategy getStrategy() {
        return strategy;
    }

    public static final class Builder {
        private Strategy strategy = Strategy.P2P_CLUSTER;

        @NonNull
        public Builder setStrategy(@NonNull Strategy strategy) {
            this.strategy = strategy;
            return this;
        }

        @NonNull
        public DiscoveryOptions build() {
            return new DiscoveryOptions(strategy);
        }
    }
}
