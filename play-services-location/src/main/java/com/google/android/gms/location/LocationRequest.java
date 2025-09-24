/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import android.Manifest;
import android.os.SystemClock;

import android.os.WorkSource;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import com.google.android.gms.location.internal.ClientIdentity;
import org.microg.gms.common.PublicApi;
import org.microg.gms.location.GranularityUtil;
import org.microg.gms.location.PriorityUtil;
import org.microg.gms.location.ThrottleBehaviorUtil;
import org.microg.gms.utils.WorkSourceUtil;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Arrays;
import java.util.Objects;

/**
 * A data object that contains quality of service parameters for requests to the
 * FusedLocationProviderApi.
 * <p/>
 * LocationRequest objects are used to request a quality of service for location updates from the
 * FusedLocationProviderApi.
 * <p/>
 * For example, if your application wants high accuracy location it should create a location
 * request with {@link #setPriority(int)} set to {@link #PRIORITY_HIGH_ACCURACY} and
 * {@link #setInterval(long)} to 5 seconds. This would be appropriate for mapping applications that
 * are showing your location in real-time.
 * <p/>
 * At the other extreme, if you want negligible power impact, but to still receive location updates
 * when available, then create a location request with {@link #setPriority(int)} set to
 * {@link #PRIORITY_NO_POWER}. With this request your application will not trigger (and therefore
 * will not receive any power blame) any location updates, but will receive locations triggered by
 * other applications. This would be appropriate for applications that have no firm requirement for
 * location, but can take advantage when available.
 * <p/>
 * In between these two extremes is a very common use-case, where applications definitely want to
 * receive updates at a specified interval, and can receive them faster when available, but still
 * want a low power impact. These applications should consider
 * {@link #PRIORITY_BALANCED_POWER_ACCURACY} combined with a faster
 * {@link #setFastestInterval(long)} (such as 1 minute) and a slower {@link #setInterval(long)}
 * (such as 60 minutes). They will only be assigned power blame for the interval set by
 * {@link #setInterval(long)}, but can still receive locations triggered by other applications at a
 * rate up to {@link #setFastestInterval(long)}. This style of request is appropriate for many
 * location aware applications, including background usage. Do be careful to also throttle
 * {@link #setFastestInterval(long)} if you perform heavy-weight work after receiving an update -
 * such as using the network.
 * <p/>
 * Activities should strongly consider removing all location request when entering the background
 * (for example at {@link android.app.Activity#onPause()}), or at least swap the request to a
 * larger interval and lower quality.
 * <p/>
 * Applications cannot specify the exact location sources, such as GPS, that are used by the
 * LocationClient. In fact, the system may have multiple location sources (providers) running and
 * may fuse the results from several sources into a single Location object.
 * <p/>
 * Location requests from applications with
 * {@link android.Manifest.permission#ACCESS_COARSE_LOCATION} and not
 * {@link android.Manifest.permission#ACCESS_FINE_LOCATION} will be automatically throttled to a
 * slower interval, and the location object will be obfuscated to only show a coarse level of
 * accuracy.
 * <p/>
 * All location requests are considered hints, and you may receive locations that are more/less
 * accurate, and faster/slower than requested.
 */
public class LocationRequest extends AutoSafeParcelable {

    /**
     * @deprecated Use {@link Priority#PRIORITY_BALANCED_POWER_ACCURACY} instead.
     */
    @Deprecated
    public static final int PRIORITY_BALANCED_POWER_ACCURACY = 102;
    /**
     * @deprecated Use {@link Priority#PRIORITY_HIGH_ACCURACY} instead.
     */
    @Deprecated
    public static final int PRIORITY_HIGH_ACCURACY = 100;
    /**
     * @deprecated Use {@link Priority#PRIORITY_LOW_POWER} instead.
     */
    @Deprecated
    public static final int PRIORITY_LOW_POWER = 104;
    /**
     * @deprecated Use {@link Priority#PRIORITY_PASSIVE} instead.
     */
    @Deprecated
    public static final int PRIORITY_NO_POWER = 105;

    @Field(1000)
    private int versionCode = 1;
    @Field(1)
    @Priority
    private int priority;
    @Field(2)
    private long intervalMillis;
    @Field(3)
    private long minUpdateIntervalMillis;
    @Field(4)
    @Deprecated
    private boolean explicitFastestInterval;
    @Field(5)
    @Deprecated
    private long expirationTime;
    @Field(6)
    private int maxUpdates;
    @Field(7)
    private float minUpdateDistanceMeters;
    @Field(8)
    private long maxUpdateDelayMillis;
    @Field(9)
    private boolean waitForAccurateLocation;
    @Field(10)
    private long durationMillis;
    @Field(11)
    private long maxUpdateAgeMillis;
    @Field(12)
    @Granularity
    private int granularity;
    @Field(13)
    @ThrottleBehavior
    private int throttleBehavior;
    @Field(14)
    @Nullable
    private String moduleId;
    @Field(15)
    private boolean bypass;
    @Field(16)
    @NonNull
    private WorkSource workSource;
    @Field(17)
    @Nullable
    private ClientIdentity impersonation;

    @Deprecated
    public LocationRequest() {
        this.priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY;
        this.intervalMillis = 3600000;
        this.minUpdateIntervalMillis = 600000;
        this.maxUpdateDelayMillis = 0;
        this.durationMillis = Long.MAX_VALUE;
        this.maxUpdates = Integer.MAX_VALUE;
        this.minUpdateDistanceMeters = 0;
        this.waitForAccurateLocation = false;
        this.maxUpdateAgeMillis = -1;
        this.granularity = Granularity.GRANULARITY_PERMISSION_LEVEL;
        this.throttleBehavior = ThrottleBehavior.THROTTLE_BACKGROUND;
        this.bypass = false;
        this.workSource = new WorkSource();

        // deprecated
        this.explicitFastestInterval = false;
        this.expirationTime = Long.MAX_VALUE;
    }

    /**
     * @deprecated Use {@link LocationRequest.Builder} instead. May be removed in a future release.
     */
    @Deprecated
    public static LocationRequest create() {
        return new LocationRequest();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LocationRequest)) return false;
        LocationRequest other = (LocationRequest) o;

        if (this.priority != other.priority) return false;
        if (this.intervalMillis != other.intervalMillis && !isPassive()) return false;
        if (this.minUpdateIntervalMillis != other.minUpdateIntervalMillis) return false;
        if (isBatched() != other.isBatched()) return false;
        if (this.maxUpdateDelayMillis != other.maxUpdateDelayMillis && isBatched()) return false;
        if (this.durationMillis != other.durationMillis) return false;
        if (this.maxUpdates != other.maxUpdates) return false;
        if (this.minUpdateDistanceMeters != other.minUpdateDistanceMeters) return false;
        if (this.waitForAccurateLocation != other.waitForAccurateLocation) return false;
        if (this.granularity != other.granularity) return false;
        if (this.throttleBehavior != other.throttleBehavior) return false;
        if (this.workSource.equals(other.workSource)) return false;
        if (!Objects.equals(this.moduleId, other.moduleId)) return false;
        if (!Objects.equals(this.impersonation, other.impersonation)) return false;

        return true;
    }

    /**
     * The duration of this request. A location request will not receive any locations after it has expired, and will be removed
     * shortly thereafter. A value of {@link Long#MAX_VALUE} implies an infinite duration.
     */
    public long getDurationMillis() {
        return durationMillis;
    }

    /**
     * @deprecated Use {@link #getDurationMillis()} instead. Using this method will return the duration added to the current elapsed realtime, which
     * may give unexpected results. May be removed in a future release.
     */
    @Deprecated
    public long getExpirationTime() {
        return expirationTime;
    }

    /**
     * @deprecated Use {@link #getMinUpdateIntervalMillis()} instead. May be removed in a future release.
     */
    @Deprecated
    public long getFastestInterval() {
        return getMinUpdateIntervalMillis();
    }

    /**
     * The {@link Granularity} of locations returned for this request. This controls whether fine or coarse locations may be returned.
     */
    @Granularity
    public int getGranularity() {
        return granularity;
    }

    @Nullable
    @PublicApi(exclude = true)
    public ClientIdentity getImpersonation() {
        return impersonation;
    }

    /**
     * @deprecated Use {@link #getIntervalMillis()} instead. May be removed in a future release.
     */
    @Deprecated
    public long getInterval() {
        return intervalMillis;
    }

    /**
     * The desired interval of location updates. Location updates may arrive faster than this interval (but no faster than
     * specified by {@link #getMinUpdateIntervalMillis()}) or slower than this interval (if the request is being throttled for
     * example).
     */
    public long getIntervalMillis() {
        return intervalMillis;
    }

    /**
     * The maximum age of an initial historical location delivered for this request. A value of 0 indicates that no initial historical
     * location will be delivered, only freshly derived locations. A value {@link Long#MAX_VALUE} represents an effectively unbounded
     * maximum age.
     */
    public long getMaxUpdateAgeMillis() {
        if (maxUpdateAgeMillis == Builder.IMPLICIT_MAX_UPDATE_AGE) return intervalMillis;
        return maxUpdateAgeMillis;
    }

    /**
     * The longest a location update may be delayed. This parameter controls location batching behavior. If this is set to a value
     * at least 2x larger than the interval specified by {@link #getIntervalMillis()}, then a device may (but is not required to) save
     * power by delivering locations in batches. If clients do not require immediate delivery, consider setting this value as high
     * as is reasonable to allow for additional power savings.
     * <p>
     * For example, if a request is made with a 2s interval and a 10s maximum update delay, this implies that the device may
     * choose to deliver batches of 5 locations every 10s (where each location should represent a point in time ~2s after the
     * previous).
     * <p>
     * Support for batching may vary by device type, so simply allowing batching via this parameter does not imply a client will
     * receive batched results on all devices.
     * <p>
     * {@link FusedLocationProviderClient#flushLocations()} may be used to flush locations that have been batched, but not
     * delivered yet.
     */
    public long getMaxUpdateDelayMillis() {
        return maxUpdateDelayMillis;
    }

    /**
     * The maximum number of updates delivered to this request. A location request will not receive any locations after the
     * maximum number of updates has been reached, and will be removed shortly thereafter. A value of {@link Integer#MAX_VALUE}
     * implies an unlimited number of updates.
     */
    public int getMaxUpdates() {
        return maxUpdates;
    }

    /**
     * @deprecated Use {@link #getMaxUpdateDelayMillis()} instead. May be removed in a future release.
     */
    @Deprecated
    public long getMaxWaitTime() {
        return getMaxUpdateDelayMillis();
    }

    /**
     * The minimum distance required between consecutive location updates. If a derived location update is not at least the
     * specified distance away from the previous location update delivered to the client, it will not be delivered. This may also
     * allow additional power savings under some circumstances.
     */
    public float getMinUpdateDistanceMeters() {
        return minUpdateDistanceMeters;
    }

    /**
     * The fastest allowed interval of location updates. Location updates may arrive faster than the desired interval
     * ({@link #getIntervalMillis()}), but will never arrive faster than specified here. FLP APIs make some allowance for jitter with
     * the minimum update interval, so clients need not worry about location updates that arrive a couple milliseconds too early
     * being rejected.
     */
    public long getMinUpdateIntervalMillis() {
        if (minUpdateIntervalMillis == Builder.IMPLICIT_MIN_UPDATE_INTERVAL) return intervalMillis;
        return minUpdateIntervalMillis;
    }

    @PublicApi(exclude = true)
    @Nullable
    public String getModuleId() {
        return moduleId;
    }

    /**
     * @deprecated Use {@link #getMaxUpdates()} instead. May be removed in a future release.
     */
    @Deprecated
    public int getNumUpdates() {
        return getMaxUpdates();
    }

    /**
     * The {@link Priority} of the location request.
     */
    @Priority
    public int getPriority() {
        return priority;
    }

    /**
     * @deprecated Use {@link #getMinUpdateDistanceMeters()} instead.
     */
    @Deprecated
    public float getSmallestDisplacement() {
        return getMinUpdateDistanceMeters();
    }

    @PublicApi(exclude = true)
    @ThrottleBehavior
    public int getThrottleBehavior() {
        return throttleBehavior;
    }

    @PublicApi(exclude = true)
    public WorkSource getWorkSource() {
        return workSource;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{priority, intervalMillis, minUpdateIntervalMillis, workSource});
    }

    /**
     * True if this request allows batching (i.e. {@link #getMaxUpdateDelayMillis()} is at least 2x {@link #getIntervalMillis()}).
     */
    public boolean isBatched() {
        return maxUpdateDelayMillis > 0 && maxUpdateDelayMillis > intervalMillis * 2;
    }

    @PublicApi(exclude = true)
    public boolean isBypass() {
        return bypass;
    }

    /**
     * @deprecated Do not use. May be removed in a future release.
     */
    @Deprecated
    public boolean isFastestIntervalExplicitlySet() {
        return true;
    }

    /**
     * True if the priority is {@link Priority#PRIORITY_PASSIVE}.
     */
    public boolean isPassive() {
        return priority == Priority.PRIORITY_PASSIVE;
    }

    /**
     * If this request is {@link Priority#PRIORITY_HIGH_ACCURACY}, this will delay delivery of initial low accuracy locations for a
     * small amount of time in case a high accuracy location can be delivered instead.
     */
    public boolean isWaitForAccurateLocation() {
        return waitForAccurateLocation;
    }

    /**
     * @deprecated Use {@link LocationRequest.Builder#setDurationMillis(long)} instead. May be removed in a future release.
     */
    @Deprecated
    @NonNull
    public LocationRequest setExpirationDuration(long durationMillis) {
        if (durationMillis <= 0) throw new IllegalArgumentException("durationMillis must be greater than 0");
        this.durationMillis = durationMillis;
        return this;
    }

    /**
     * @deprecated Use {@link LocationRequest.Builder#setDurationMillis(long)} instead. Using this method will express the expiration time in
     * terms of duration, which may give unexpected results. May be removed in a future release.
     */
    @Deprecated
    @NonNull
    public LocationRequest setExpirationTime(long elapsedRealtime) {
        this.durationMillis = Math.max(1, elapsedRealtime - SystemClock.elapsedRealtime());
        return this;
    }

    /**
     * @deprecated Use {@link LocationRequest.Builder#setMinUpdateIntervalMillis(long)} instead. May be removed in a future release.
     */
    @Deprecated
    @NonNull
    public LocationRequest setFastestInterval(long fastestIntervalMillis) throws IllegalArgumentException {
        if (fastestIntervalMillis < 0) throw new IllegalArgumentException("illegal fastest interval: " + fastestIntervalMillis);
        this.minUpdateIntervalMillis = fastestIntervalMillis;
        explicitFastestInterval = true; // FIXME: Remove
        return this;
    }

    /**
     * @deprecated Use {@link LocationRequest.Builder#setIntervalMillis(long)} instead. May be removed in a future release.
     */
    @Deprecated
    @NonNull
    public LocationRequest setInterval(long intervalMillis) throws IllegalArgumentException {
        if (intervalMillis < 0) throw new IllegalArgumentException("intervalMillis must be greater than or equal to 0");
        if (this.minUpdateIntervalMillis == this.intervalMillis / 6) {
            this.minUpdateIntervalMillis = intervalMillis / 6;
        }
        if (this.maxUpdateAgeMillis == this.intervalMillis) {
            this.maxUpdateAgeMillis = intervalMillis;
        }
        this.intervalMillis = intervalMillis;
        return this;
    }

    /**
     * @deprecated Use {@link LocationRequest.Builder#setMaxUpdateDelayMillis(long)} instead. May be removed in a future release.
     */
    @Deprecated
    @NonNull
    public LocationRequest setMaxWaitTime(long maxWaitTimeMillis) throws IllegalArgumentException {
        if (maxWaitTimeMillis < 0) throw new IllegalArgumentException("illegal max wait time: " + maxWaitTimeMillis);
        maxUpdateDelayMillis = maxWaitTimeMillis;
        return this;
    }

    /**
     * @deprecated Use {@link LocationRequest.Builder#setMaxUpdates(int)} instead. May be removed in a future release.
     */
    @Deprecated
    @NonNull
    public LocationRequest setNumUpdates(int maxUpdates) throws IllegalArgumentException {
        if (maxUpdates <= 0) throw new IllegalArgumentException("invalid numUpdates: " + maxUpdates);
        this.maxUpdates = maxUpdates;
        return this;
    }

    /**
     * @deprecated Use {@link LocationRequest.Builder#setPriority(int)} instead. May be removed in a future release.
     */
    @Deprecated
    @NonNull
    public LocationRequest setPriority(@Priority int priority) {
        PriorityUtil.checkValidPriority(priority);
        this.priority = priority;
        return this;
    }

    /**
     * @deprecated Use {@link LocationRequest.Builder#setMinUpdateDistanceMeters(float)} instead. May be removed in a future release.
     */
    @Deprecated
    @NonNull
    public LocationRequest setSmallestDisplacement(float smallestDisplacementMeters) {
        if (smallestDisplacementMeters < 0) throw new IllegalArgumentException("invalid displacement: " + smallestDisplacementMeters);
        this.minUpdateDistanceMeters = smallestDisplacementMeters;
        return this;
    }

    /**
     * @deprecated Use {@link LocationRequest.Builder#setWaitForAccurateLocation(boolean)} instead. May be removed in a future release.
     */
    @Deprecated
    @NonNull
    public LocationRequest setWaitForAccurateLocation(boolean waitForAccurateLocation) {
        this.waitForAccurateLocation = waitForAccurateLocation;
        return this;
    }

    @Override
    @NonNull
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Request[");
        if (isPassive()) {
            sb.append(PriorityUtil.priorityToString(priority));
        } else {
            sb.append("@");
            sb.append(intervalMillis).append("ms");
            if (isBatched()) {
                sb.append("/");
                sb.append(maxUpdateDelayMillis).append("ms");
            }
            sb.append(" ").append(PriorityUtil.priorityToString(priority));
        }
        if (isPassive() || minUpdateIntervalMillis != intervalMillis)
            sb.append(", minUpdateInterval=").append(minUpdateIntervalMillis).append("ms");
        if (minUpdateDistanceMeters > 0)
            sb.append(", minUpdateDistance=").append(minUpdateDistanceMeters).append("m");
        if (!isPassive() ? maxUpdateAgeMillis != intervalMillis : maxUpdateAgeMillis != Long.MAX_VALUE)
            sb.append(", maxUpdateAge=").append(maxUpdateAgeMillis).append("ms");
        if (durationMillis != Long.MAX_VALUE)
            sb.append(", duration=").append(durationMillis).append("ms");
        if (maxUpdates != Integer.MAX_VALUE)
            sb.append(", maxUpdates").append(maxUpdates);
        if (throttleBehavior != 0)
            sb.append(", ").append(ThrottleBehaviorUtil.throttleBehaviorToString(throttleBehavior));
        if (granularity != 0)
            sb.append(", ").append(GranularityUtil.granularityToString(granularity));
        if (waitForAccurateLocation)
            sb.append(", waitForAccurateLocation");
        if (bypass)
            sb.append(", bypass");
        if (moduleId != null)
            sb.append(", moduleId=").append(moduleId);
        if (!WorkSourceUtil.isEmpty(workSource))
            sb.append(", ").append(workSource);
        if (impersonation != null)
            sb.append(", impersonation=").append(impersonation);
        sb.append("]");
        return sb.toString();
    }

    /**
     * Builder for {@link LocationRequest}.
     */
    public static final class Builder {
        /**
         * Represents a maximum update age that is the same as the interval.
         */
        public static final long IMPLICIT_MAX_UPDATE_AGE = -1;
        /**
         * Represents a minimum update interval that is the same as the interval.
         */
        public static final long IMPLICIT_MIN_UPDATE_INTERVAL = -1;

        @Priority
        private int priority;
        private long intervalMillis;
        private long minUpdateIntervalMillis;
        private long maxUpdateDelayMillis;
        private long durationMillis;
        private int maxUpdates;
        private float minUpdateDistanceMeters;
        private boolean waitForAccurateLocation;
        private long maxUpdateAgeMillis;
        @Granularity
        private int granularity;
        @ThrottleBehavior
        private int throttleBehavior;
        @Nullable
        private String moduleId;
        private boolean bypass;
        @Nullable
        private WorkSource workSource;
        @Nullable
        private ClientIdentity impersonation;

        /**
         * Constructs a Builder with the given interval, and default values for all other fields.
         */
        public Builder(long intervalMillis) {
            this(Priority.PRIORITY_BALANCED_POWER_ACCURACY, intervalMillis);
        }

        /**
         * Constructs a Builder with the given priority and interval, and default values for all other fields.
         */
        public Builder(@Priority int priority, long intervalMillis) {
            if (intervalMillis < 0) throw new IllegalArgumentException("intervalMillis must be greater than or equal to 0");
            PriorityUtil.checkValidPriority(priority);
            this.priority = priority;
            this.intervalMillis = intervalMillis;
            this.minUpdateIntervalMillis = IMPLICIT_MIN_UPDATE_INTERVAL;
            this.maxUpdateDelayMillis = 0;
            this.durationMillis = Long.MAX_VALUE;
            this.maxUpdates = Integer.MAX_VALUE;
            this.minUpdateDistanceMeters = 0;
            this.waitForAccurateLocation = true;
            this.maxUpdateAgeMillis = IMPLICIT_MAX_UPDATE_AGE;
            this.granularity = Granularity.GRANULARITY_PERMISSION_LEVEL;
            this.throttleBehavior = ThrottleBehavior.THROTTLE_BACKGROUND;
            this.moduleId = null;
            this.bypass = false;
            this.workSource = null;
            this.impersonation = null;
        }

        /**
         * Constructs a Builder with values copied from the given {@link LocationRequest}.
         */
        public Builder(LocationRequest request) {
            this.priority = request.getPriority();
            this.intervalMillis = request.getIntervalMillis();
            this.minUpdateIntervalMillis = request.getMinUpdateIntervalMillis();
            this.maxUpdateDelayMillis = request.getMaxUpdateDelayMillis();
            this.durationMillis = request.getDurationMillis();
            this.maxUpdates = request.getMaxUpdates();
            this.minUpdateDistanceMeters = request.getMinUpdateDistanceMeters();
            this.waitForAccurateLocation = request.isWaitForAccurateLocation();
            this.maxUpdateAgeMillis = request.getMaxUpdateAgeMillis();
            this.granularity = request.getGranularity();
            this.throttleBehavior = request.getThrottleBehavior();
            this.moduleId = request.getModuleId();
            this.bypass = request.isBypass();
            this.workSource = request.getWorkSource();
            this.impersonation = request.getImpersonation();
        }

        /**
         * Builds a new {@link LocationRequest}.
         */
        @NonNull
        public LocationRequest build() {
            LocationRequest request = new LocationRequest();
            request.priority = priority;
            request.intervalMillis = intervalMillis;
            if (minUpdateIntervalMillis == IMPLICIT_MIN_UPDATE_INTERVAL) {
                request.minUpdateIntervalMillis = intervalMillis;
            } else {
                request.minUpdateIntervalMillis = priority == Priority.PRIORITY_PASSIVE ? minUpdateIntervalMillis : Math.min(intervalMillis, minUpdateIntervalMillis);
            }
            request.maxUpdateDelayMillis = Math.max(maxUpdateDelayMillis, intervalMillis);
            request.durationMillis = durationMillis;
            request.maxUpdates = maxUpdates;
            request.minUpdateDistanceMeters = minUpdateDistanceMeters;
            request.waitForAccurateLocation = waitForAccurateLocation;
            request.maxUpdateAgeMillis = maxUpdateAgeMillis != IMPLICIT_MAX_UPDATE_AGE ? maxUpdateAgeMillis : intervalMillis;
            request.granularity = granularity;
            request.throttleBehavior = throttleBehavior;
            request.moduleId = moduleId;
            request.bypass = bypass;
            request.workSource = workSource;
            request.impersonation = impersonation;
            return request;
        }

        @NonNull
        @PublicApi(exclude = true)
        @RequiresPermission(anyOf = {"android.permission.WRITE_SECURE_SETTINGS", "android.permission.LOCATION_BYPASS"})
        public Builder setBypass(boolean bypass) {
            this.bypass = bypass;
            return this;
        }

        /**
         * Sets the duration of this request. A location request will not receive any locations after it has expired, and will be
         * removed shortly thereafter. A value of {@link Long#MAX_VALUE} implies an infinite duration.
         * <p>
         * The default value is {@link Long#MAX_VALUE}.
         */
        @NonNull
        public Builder setDurationMillis(long durationMillis) {
            if (durationMillis <= 0) throw new IllegalArgumentException("intervalMillis must be greater than 0");
            this.durationMillis = durationMillis;
            return this;
        }

        /**
         * Sets the {@link Granularity} of locations returned for this request. This controls whether fine or coarse locations may be
         * returned.
         * <p>
         * The default value is {@link Granularity#GRANULARITY_PERMISSION_LEVEL}.
         */
        @NonNull
        public Builder setGranularity(@Granularity int granularity) {
            GranularityUtil.checkValidGranularity(granularity);
            this.granularity = granularity;
            return this;
        }

        /**
         * Sets the desired interval of location updates. Location updates may arrive faster than this interval (but no faster than
         * specified by {@link #setMinUpdateIntervalMillis(long)}) or slower than this interval (if the request is being throttled for
         * example).
         */
        @NonNull
        public Builder setIntervalMillis(long intervalMillis) {
            if (intervalMillis < 0) throw new IllegalArgumentException("intervalMillis must be greater than or equal to 0");
            this.intervalMillis = intervalMillis;
            return this;
        }

        /**
         * Sets the maximum age of an initial historical location delivered for this request. A value of 0 indicates that no initial
         * historical location will be delivered, only freshly derived locations. A value {@link Long#MAX_VALUE} represents an effectively
         * unbounded maximum age.
         * <p>
         * This may be set to the special value {@link #IMPLICIT_MAX_UPDATE_AGE} in which case the maximum update age will always be
         * the same as the interval.
         * <p>
         * The default value is {@link #IMPLICIT_MAX_UPDATE_AGE}.
         */
        @NonNull
        public Builder setMaxUpdateAgeMillis(long maxUpdateAgeMillis) {
            if (maxUpdateAgeMillis < 0 && maxUpdateAgeMillis != IMPLICIT_MAX_UPDATE_AGE)
                throw new IllegalArgumentException("maxUpdateAgeMillis must be greater than or equal to 0, or IMPLICIT_MAX_UPDATE_AGE");
            this.maxUpdateAgeMillis = maxUpdateAgeMillis;
            return this;
        }

        /**
         * Sets the longest a location update may be delayed. This parameter controls location batching behavior. If this is set to a
         * value at least 2x larger than the interval specified by {@link #setIntervalMillis(long)}, then a device may (but is not required
         * to) save power by delivering locations in batches. If clients do not require immediate delivery, consider setting this value
         * as high as is reasonable to allow for additional power savings. When the {@link LocationRequest} is built, the maximum
         * update delay will be set to the max of the provided maximum update delay and the interval. This normalizes requests
         * without batching to have the maximum update delay equal to the interval.
         * <p>
         * For example, if a request is made with a 2s interval and a 10s maximum update delay, this implies that the device may
         * choose to deliver batches of 5 locations every 10s (where each location in a batch represents a point in time ~2s after
         * the previous).
         * <p>
         * Support for batching may vary by device hardware, so simply allowing batching via this parameter does not imply a client
         * will receive batched results on all devices.
         * <p>
         * {@link FusedLocationProviderClient#flushLocations()} may be used to flush locations that have been batched, but not
         * delivered yet.
         * <p>
         * The default value is 0.
         */
        @NonNull
        public Builder setMaxUpdateDelayMillis(long maxUpdateDelayMillis) {
            if (maxUpdateDelayMillis < 0) throw new IllegalArgumentException("maxUpdateDelayMillis must be greater than or equal to 0");
            this.maxUpdateDelayMillis = maxUpdateDelayMillis;
            return this;
        }

        /**
         * Sets the maximum number of updates delivered to this request. A location request will not receive any locations after the
         * maximum number of updates has been reached, and will be removed shortly thereafter. A value of {@link Integer#MAX_VALUE}
         * implies an unlimited number of updates.
         * <p>
         * The default value is {@link Integer#MAX_VALUE}.
         */
        @NonNull
        public Builder setMaxUpdates(int maxUpdates) {
            if (maxUpdates <= 0) throw new IllegalArgumentException("maxUpdates must be greater than 0");
            this.maxUpdates = maxUpdates;
            return this;
        }

        /**
         * Sets the minimum distance required between consecutive location updates. If a derived location update is not at least
         * the specified distance away from the previous location update delivered to the client, it will not be delivered. This may
         * also allow additional power savings under some circumstances.
         * <p>
         * The default value is 0.
         */
        @NonNull
        public Builder setMinUpdateDistanceMeters(float minUpdateDistanceMeters) {
            if (minUpdateDistanceMeters < 0) throw new IllegalArgumentException("minUpdateDistanceMeters must be greater than or equal to 0");
            this.minUpdateDistanceMeters = minUpdateDistanceMeters;
            return this;
        }

        /**
         * Sets the fastest allowed interval of location updates. Location updates may arrive faster than the desired interval
         * ({@link #setIntervalMillis(long)}), but will never arrive faster than specified here.
         * <p>
         * This may be set to the special value {@link #IMPLICIT_MIN_UPDATE_INTERVAL} in which case the minimum update interval will
         * be the same as the interval. {@link FusedLocationProviderClient} APIs make some allowance for jitter with the minimum
         * update interval, so clients need not worry about location updates that arrive a couple milliseconds too early being
         * rejected.
         * <p>
         * The default value is {@link #IMPLICIT_MIN_UPDATE_INTERVAL}.
         */
        @NonNull
        public Builder setMinUpdateIntervalMillis(long minUpdateIntervalMillis) {
            if (minUpdateIntervalMillis < 0 && minUpdateIntervalMillis != IMPLICIT_MIN_UPDATE_INTERVAL)
                throw new IllegalArgumentException("minUpdateIntervalMillis must be greater than or equal to 0, or IMPLICIT_MIN_UPDATE_INTERVAL");
            this.minUpdateIntervalMillis = minUpdateIntervalMillis;
            return this;
        }

        @NonNull
        @Deprecated
        @PublicApi(exclude = true)
        public Builder setModuleId(@Nullable String moduleId) {
            this.moduleId = moduleId;
            return this;
        }

        /**
         * Sets the {@link Priority} of the location request.
         * <p>
         * The default value is {@link Priority#PRIORITY_BALANCED_POWER_ACCURACY}.
         */
        @NonNull
        public Builder setPriority(@Priority int priority) {
            PriorityUtil.checkValidPriority(priority);
            this.priority = priority;
            return this;
        }

        @NonNull
        @PublicApi(exclude = true)
        public Builder setThrottleBehavior(@ThrottleBehavior int throttleBehavior) {
            ThrottleBehaviorUtil.checkValidThrottleBehavior(throttleBehavior);
            this.throttleBehavior = throttleBehavior;
            return this;
        }

        /**
         * If set to true and this request is {@link Priority#PRIORITY_HIGH_ACCURACY}, this will delay delivery of initial low accuracy
         * locations for a small amount of time in case a high accuracy location can be delivered instead.
         * <p>
         * The default value is true.
         */
        @NonNull
        public Builder setWaitForAccurateLocation(boolean waitForAccurateLocation) {
            this.waitForAccurateLocation = waitForAccurateLocation;
            return this;
        }

        @NonNull
        @PublicApi(exclude = true)
        @RequiresPermission(Manifest.permission.UPDATE_DEVICE_STATS)
        public Builder setWorkSource(@Nullable WorkSource workSource) {
            this.workSource = workSource;
            return this;
        }
    }


    public static final Creator<LocationRequest> CREATOR = new AutoCreator<LocationRequest>(LocationRequest.class);
}
