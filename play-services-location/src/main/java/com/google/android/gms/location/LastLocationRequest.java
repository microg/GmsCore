/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.location.internal.ClientIdentity;
import org.microg.gms.common.Hide;
import org.microg.gms.location.GranularityUtil;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Objects;

/**
 * An encapsulation of various parameters for requesting a (cached) last location through {@link FusedLocationProviderClient}.
 *
 * @see FusedLocationProviderClient#getLastLocation(LastLocationRequest)
 */
public class LastLocationRequest extends AutoSafeParcelable {
    @Field(1)
    private long maxUpdateAgeMillis;
    @Field(2)
    private @Granularity int granularity;
    @Field(3)
    private boolean bypass;
    @Field(4)
    @Nullable
    private String moduleId;
    @Field(5)
    @Nullable
    private ClientIdentity impersonation;

    private LastLocationRequest() {
        maxUpdateAgeMillis = Long.MAX_VALUE;
        granularity = Granularity.GRANULARITY_PERMISSION_LEVEL;
    }


    /**
     * The {@link Granularity} of locations returned for this request. This controls whether fine or coarse locations may be returned.
     */
    public @Granularity int getGranularity() {
        return granularity;
    }

    /**
     * The maximum age of any location returned for this request. A value of {@link Long#MAX_VALUE} represents an effectively unbounded maximum age.
     */
    public long getMaxUpdateAgeMillis() {
        return maxUpdateAgeMillis;
    }

    /**
     * A builder for {@link LastLocationRequest}.
     */
    public static class Builder {
        private long maxUpdateAgeMillis;
        private @Granularity int granularity;
        private boolean bypass;
        @Nullable
        private String moduleId;
        @Nullable
        private ClientIdentity impersonation;

        /**
         * Constructs a new builder with default values.
         */
        public Builder() {
            maxUpdateAgeMillis = Long.MAX_VALUE;
            granularity = Granularity.GRANULARITY_PERMISSION_LEVEL;
        }

        /**
         * Constructs a new builder with values copied from the given {@link LastLocationRequest}.
         */
        public Builder(@NonNull LastLocationRequest request) {
            this.maxUpdateAgeMillis = request.getMaxUpdateAgeMillis();
            this.granularity = request.getGranularity();
            this.bypass = request.isBypass();
            this.moduleId = request.getModuleId();
            this.impersonation = request.getImpersonation();
        }

        /**
         * Sets the {@link Granularity} of locations returned for this request. This controls whether fine or coarse locations may be returned.
         * <p>
         * The default value is {@link Granularity#GRANULARITY_PERMISSION_LEVEL}.
         */
        public LastLocationRequest.Builder setGranularity(@Granularity int granularity) {
            GranularityUtil.checkValidGranularity(granularity);
            this.granularity = granularity;
            return this;
        }

        /**
         * Sets the maximum age of any location returned for this request. A value of {@link Long#MAX_VALUE} represents an effectively unbounded maximum age.
         * <p>
         * The default value is {@link Long#MAX_VALUE}.
         */
        public LastLocationRequest.Builder setMaxUpdateAgeMillis(long maxUpdateAgeMillis) {
            if (maxUpdateAgeMillis <= 0) throw new IllegalArgumentException("maxUpdateAgeMillis must be greater than 0");
            this.maxUpdateAgeMillis = maxUpdateAgeMillis;
            return this;
        }

        /**
         * Builds a new {@link LastLocationRequest}.
         */
        public LastLocationRequest build() {
            LastLocationRequest request = new LastLocationRequest();
            request.maxUpdateAgeMillis = maxUpdateAgeMillis;
            request.granularity = granularity;
            request.bypass = bypass;
            request.moduleId = moduleId;
            request.impersonation = impersonation;
            return request;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LastLocationRequest)) return false;

        LastLocationRequest request = (LastLocationRequest) o;

        if (maxUpdateAgeMillis != request.maxUpdateAgeMillis) return false;
        if (granularity != request.granularity) return false;
        if (bypass != request.bypass) return false;
        if (!Objects.equals(moduleId, request.moduleId)) return false;
        return Objects.equals(impersonation, request.impersonation);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[]{this.maxUpdateAgeMillis, this.granularity, this.bypass});
    }

    @Hide
    public boolean isBypass() {
        return bypass;
    }

    @Hide
    @Nullable
    public String getModuleId() {
        return moduleId;
    }

    @Hide
    @Nullable
    public ClientIdentity getImpersonation() {
        return impersonation;
    }

    public static final Creator<LastLocationRequest> CREATOR = new AutoCreator<>(LastLocationRequest.class);
}
