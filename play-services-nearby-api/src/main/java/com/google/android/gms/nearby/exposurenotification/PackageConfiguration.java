/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.exposurenotification;

import android.os.Bundle;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

/**
 * Holds configuration values that can be passed onto the client app after it has finished installing via {@link ExposureNotificationClient#getPackageConfiguration()}.
 */
@PublicApi
public class PackageConfiguration extends AutoSafeParcelable {
    @Field(1)
    private Bundle values;

    @PublicApi(exclude = true)
    public PackageConfiguration() {
    }

    @PublicApi(exclude = true)
    public PackageConfiguration(Bundle values) {
        this.values = values;
    }

    public Bundle getValues() {
        return values;
    }

    /**
     * A builder for {@link PackageConfiguration}.
     */
    public static final class PackageConfigurationBuilder {
        private Bundle values;

        /**
         * Sets a Bundle containing configuration options.
         */
        public PackageConfigurationBuilder setValues(Bundle values) {
            this.values = values;
            return this;
        }

        /**
         * Builds a {@link PackageConfiguration}.
         */
        public PackageConfiguration build() {
            return new PackageConfiguration(values);
        }
    }

    public static final Creator<PackageConfiguration> CREATOR = new AutoCreator<>(PackageConfiguration.class);
}
