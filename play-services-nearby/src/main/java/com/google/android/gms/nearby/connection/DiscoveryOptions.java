/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.nearby.connection;

import org.microg.gms.common.PublicApi;

import java.util.Objects;

@PublicApi
public class DiscoveryOptions {
    private final Strategy strategy;
    private final boolean forwardUnrecognizedBluetoothDevices;

    public DiscoveryOptions(Strategy strategy) {
        this(strategy, false);
    }

    private DiscoveryOptions(Strategy strategy, boolean forwardUnrecognizedBluetoothDevices) {
        this.strategy = strategy;
        this.forwardUnrecognizedBluetoothDevices = forwardUnrecognizedBluetoothDevices;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public boolean getForwardUnrecognizedBluetoothDevices() {
        return forwardUnrecognizedBluetoothDevices;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscoveryOptions that = (DiscoveryOptions) o;
        return forwardUnrecognizedBluetoothDevices == that.forwardUnrecognizedBluetoothDevices &&
                Objects.equals(strategy, that.strategy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(strategy, forwardUnrecognizedBluetoothDevices);
    }

    @PublicApi
    public static final class Builder {
        private Strategy strategy = Strategy.P2P_CLUSTER;
        private boolean forwardUnrecognizedBluetoothDevices = false;

        public Builder() {}

        public Builder(DiscoveryOptions options) {
            if (options != null) {
                this.strategy = options.strategy;
                this.forwardUnrecognizedBluetoothDevices = options.forwardUnrecognizedBluetoothDevices;
            }
        }

        public Builder setStrategy(Strategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder setForwardUnrecognizedBluetoothDevices(boolean forward) {
            this.forwardUnrecognizedBluetoothDevices = forward;
            return this;
        }

        public DiscoveryOptions build() {
            return new DiscoveryOptions(strategy, forwardUnrecognizedBluetoothDevices);
        }
    }
}
