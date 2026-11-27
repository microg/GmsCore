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
public class AdvertisingOptions {
    private final Strategy strategy;
    private final boolean autoUpgradeBandwidth;
    private final boolean enforceTopologyConstraints;

    public AdvertisingOptions(Strategy strategy) {
        this(strategy, true, true);
    }

    private AdvertisingOptions(Strategy strategy, boolean autoUpgradeBandwidth, boolean enforceTopologyConstraints) {
        this.strategy = strategy;
        this.autoUpgradeBandwidth = autoUpgradeBandwidth;
        this.enforceTopologyConstraints = enforceTopologyConstraints;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public boolean getAutoUpgradeBandwidth() {
        return autoUpgradeBandwidth;
    }

    public boolean getEnforceTopologyConstraints() {
        return enforceTopologyConstraints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdvertisingOptions that = (AdvertisingOptions) o;
        return autoUpgradeBandwidth == that.autoUpgradeBandwidth &&
                enforceTopologyConstraints == that.enforceTopologyConstraints &&
                Objects.equals(strategy, that.strategy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(strategy, autoUpgradeBandwidth, enforceTopologyConstraints);
    }

    @PublicApi
    public static final class Builder {
        private Strategy strategy = Strategy.P2P_CLUSTER;
        private boolean autoUpgradeBandwidth = true;
        private boolean enforceTopologyConstraints = true;

        public Builder() {}

        public Builder(AdvertisingOptions options) {
            if (options != null) {
                this.strategy = options.strategy;
                this.autoUpgradeBandwidth = options.autoUpgradeBandwidth;
                this.enforceTopologyConstraints = options.enforceTopologyConstraints;
            }
        }

        public Builder setStrategy(Strategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder setAutoUpgradeBandwidth(boolean autoUpgradeBandwidth) {
            this.autoUpgradeBandwidth = autoUpgradeBandwidth;
            return this;
        }

        public Builder setEnforceTopologyConstraints(boolean enforceTopologyConstraints) {
            this.enforceTopologyConstraints = enforceTopologyConstraints;
            return this;
        }

        public AdvertisingOptions build() {
            return new AdvertisingOptions(strategy, autoUpgradeBandwidth, enforceTopologyConstraints);
        }
    }
}
