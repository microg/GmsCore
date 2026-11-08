/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.connection;

import androidx.annotation.NonNull;

import org.microg.gms.common.PublicApi;

/** Options for {@link ConnectionsClient#startAdvertising}. */
@PublicApi
public class AdvertisingOptions {
    private final Strategy strategy;

    private AdvertisingOptions(Strategy strategy) {
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
        public AdvertisingOptions build() {
            return new AdvertisingOptions(strategy);
        }
    }
}
