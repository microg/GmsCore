/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import android.os.WorkSource;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.location.internal.ClientIdentity;
import com.google.android.gms.tasks.CancellationToken;
import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;
import org.microg.gms.location.GranularityUtil;
import org.microg.gms.location.PriorityUtil;
import org.microg.safeparcel.AutoSafeParcelable;

/**
 * An encapsulation of various parameters for requesting the current location through FusedLocationProviderClient.
 *
 * @see FusedLocationProviderClient#getCurrentLocation(CurrentLocationRequest, CancellationToken)
 */
@PublicApi
public class CurrentLocationRequest extends AutoSafeParcelable {
    @Field(1)
    private long maxUpdateAgeMillis;
    @Field(2)
    private @Granularity int granularity;
    @Field(3)
    private @Priority int priority;
    @Field(4)
    private long durationMillis;
    @Field(5)
    private boolean bypass;
    @Field(6)
    private WorkSource workSource;
    @Field(7)
    private @ThrottleBehavior int throttleBehavior;
    @Field(8)
    private String moduleId;
    @Field(9)
    private ClientIdentity impersonation;

    private CurrentLocationRequest() {
        maxUpdateAgeMillis = Long.MAX_VALUE;
        granularity = Granularity.GRANULARITY_PERMISSION_LEVEL;
        priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY;
        durationMillis = Long.MAX_VALUE;
        workSource = new WorkSource();
        throttleBehavior = ThrottleBehavior.THROTTLE_BACKGROUND;
    }

    /**
     * The duration in milliseconds of the location request used to derive the current location if no historical location satisfies the current location
     * request. If this duration expires with no location, the current location request will return a null location. The current location request may fail and
     * return a null location after a shorter duration (ie, the given duration may be capped internally), but never a longer duration.
     */
    public long getDurationMillis() {
        return durationMillis;
    }

    /**
     * The {@link Granularity} of locations returned for this request. This controls whether fine or coarse locations may be returned.
     */
    public @Granularity int getGranularity() {
        return granularity;
    }

    /**
     * The maximum age of any location returned for this request. A value of 0 indicates that only freshly derived locations will be returned, and no
     * historical locations will ever be returned. A value Long.MAX_VALUE represents an effectively unbounded maximum age.
     * <p>
     * NOTE: This parameter applies only to historical locations. Freshly derived locations should almost always have timestamps close to the present time -
     * however it is possible under unlikely conditions for location derivation to take longer than expected, in which case freshly derived locations may have
     * slightly older timestamps.
     */
    public long getMaxUpdateAgeMillis() {
        return maxUpdateAgeMillis;
    }

    /**
     * The {@link Priority} of the location request used to derive the current location if no historical location satisfies the current location request.
     */
    public @Priority int getPriority() {
        return priority;
    }

    /**
     * A builder for {@link CurrentLocationRequest}.
     */
    public static class Builder {
        private long maxUpdateAgeMillis;
        private @Granularity int granularity;
        private @Priority int priority;
        private long durationMillis;
        private boolean bypass;
        private @ThrottleBehavior int throttleBehavior;
        @Nullable
        private String moduleId;
        @Nullable
        private WorkSource workSource;
        @Nullable
        private ClientIdentity impersonation;

        /**
         * Constructs a new builder with default values.
         */
        public Builder() {
            this.maxUpdateAgeMillis = 60000L;
            this.granularity = Granularity.GRANULARITY_PERMISSION_LEVEL;
            this.priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY;
            this.durationMillis = Long.MAX_VALUE;
            this.bypass = false;
            this.throttleBehavior = ThrottleBehavior.THROTTLE_BACKGROUND;
            this.moduleId = null;
            this.workSource = null;
            this.impersonation = null;
        }

        /**
         * Constructs a new builder with values copied from the given {@link CurrentLocationRequest}.
         */
        public Builder(CurrentLocationRequest request) {
            this.maxUpdateAgeMillis = request.getMaxUpdateAgeMillis();
            this.granularity = request.getGranularity();
            this.priority = request.getPriority();
            this.durationMillis = request.getDurationMillis();
            this.bypass = request.isBypass();
            this.throttleBehavior = request.getThrottleBehavior();
            this.moduleId = request.getModuleId();
            this.workSource = new WorkSource(request.getWorkSource());
            this.impersonation = request.getImpersonation();
        }

        /**
         * Builds a new {@link CurrentLocationRequest}.
         */
        @NonNull
        public CurrentLocationRequest build() {
            CurrentLocationRequest request = new CurrentLocationRequest();
            request.maxUpdateAgeMillis = maxUpdateAgeMillis;
            request.granularity = granularity;
            request.priority = priority;
            request.durationMillis = durationMillis;
            request.bypass = bypass;
            request.throttleBehavior = throttleBehavior;
            request.moduleId = moduleId;
            request.workSource = new WorkSource(workSource);
            request.impersonation = impersonation;
            return request;
        }

        /**
         * Sets the duration in milliseconds of the location request used to derive the current location if no historical location satisfies the current
         * location request. If this duration expires with no location, the current location request will return a null location. The current location request
         * may fail and return a null location after a shorter duration, but never a longer duration.
         * <p>
         * NOTE: Internally, this duration may be capped with what the Fused Location Provider believes is a reasonable maximum duration until it is unlikely
         * that any current location can be derived. This value is usually around roughly 30 seconds.
         * <p>
         * The default value is {@link Long#MAX_VALUE}.
         */
        public CurrentLocationRequest.Builder setDurationMillis(long durationMillis) {
            if (durationMillis <= 0) throw new IllegalArgumentException("durationMillis must be greater than 0");
            this.durationMillis = durationMillis;
            return this;
        }

        /**
         * Sets the {@link Granularity} of locations returned for this request. This controls whether fine or coarse locations may be returned.
         * <p>
         * The default value is {@link Granularity#GRANULARITY_PERMISSION_LEVEL}.
         */
        public CurrentLocationRequest.Builder setGranularity(@Granularity int granularity) {
            GranularityUtil.checkValidGranularity(granularity);
            this.granularity = granularity;
            return this;
        }

        /**
         * Sets the maximum age of any location returned for this request. A value of 0 indicates that only freshly derived locations will be returned, and no
         * historical locations will ever be returned. A value {@link Long#MAX_VALUE} represents an effectively unbounded maximum age.
         * <p>
         * NOTE: This parameter applies only to historical locations. Freshly derived locations should almost always have timestamps close to the present time -
         * however it is possible under unlikely conditions for location derivation to take longer than expected, in which case freshly derived locations may
         * have slightly older timestamps.
         * <p>
         * The default value is 1 minute. Do not rely on the default value always being 1 minute as this may change without notice.
         */
        public CurrentLocationRequest.Builder setMaxUpdateAgeMillis(long maxUpdateAgeMillis) {
            if (maxUpdateAgeMillis < 0) throw new IllegalArgumentException("maxUpdateAgeMillis must be greater than or equal to 0");
            this.maxUpdateAgeMillis = maxUpdateAgeMillis;
            return this;
        }

        /**
         * Sets the {@link Priority} of the location request used to derive the current location if no historical location satisfies the current location
         * request.
         * <p>
         * The default value is {@link Priority#PRIORITY_BALANCED_POWER_ACCURACY}.
         */
        public CurrentLocationRequest.Builder setPriority(@Priority int priority) {
            PriorityUtil.checkValidPriority(priority);
            this.priority = priority;
            return this;
        }

        @Hide
        public CurrentLocationRequest.Builder setBypass(boolean bypass) {
            this.bypass = bypass;
            return this;
        }

        @Hide
        public CurrentLocationRequest.Builder setThrottleBehavior(int throttleBehavior) {
            this.throttleBehavior = throttleBehavior;
            return this;
        }

        @Hide
        public CurrentLocationRequest.Builder setModuleId(@Nullable String moduleId) {
            this.moduleId = moduleId;
            return this;
        }

        @Hide
        public CurrentLocationRequest.Builder setWorkSource(@Nullable WorkSource workSource) {
            this.workSource = workSource;
            return this;
        }

        @Hide
        public CurrentLocationRequest.Builder setImpersonation(@Nullable ClientIdentity impersonation) {
            this.impersonation = impersonation;
            return this;
        }
    }

    @Hide
    public boolean isBypass() {
        return bypass;
    }

    @Hide
    public @ThrottleBehavior int getThrottleBehavior() {
        return throttleBehavior;
    }

    @Hide
    public String getModuleId() {
        return moduleId;
    }

    @Hide
    public WorkSource getWorkSource() {
        return workSource;
    }

    @Hide
    public ClientIdentity getImpersonation() {
        return impersonation;
    }

    public static final Creator<CurrentLocationRequest> CREATOR = new AutoCreator<>(CurrentLocationRequest.class);
}
