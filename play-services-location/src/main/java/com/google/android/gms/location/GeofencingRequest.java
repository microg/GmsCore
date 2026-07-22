/*
 * SPDX-FileCopyrightText: 2017 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.location.internal.ParcelableGeofence;
import org.microg.safeparcel.AutoSafeParcelable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

/**
 * Specifies the list of geofences to be monitored and how the geofence notifications should be reported.
 * <p>
 * Refer to {@link GeofencingClient#addGeofences(GeofencingRequest, android.app.PendingIntent)} on how to monitor geofences.
 */
public class GeofencingRequest extends AutoSafeParcelable {
    /**
     * A flag indicating that geofencing service should trigger {@link Geofence#GEOFENCE_TRANSITION_ENTER} notification at the moment when the geofence is
     * added and if the device is already inside that geofence.
     */
    public static final int INITIAL_TRIGGER_ENTER = 1;
    /**
     * A flag indicating that geofencing service should trigger {@link Geofence#GEOFENCE_TRANSITION_EXIT} notification at the moment when the geofence is
     * added and if the device is already outside that geofence.
     */
    public static final int INITIAL_TRIGGER_EXIT = 2;
    /**
     * A flag indicating that geofencing service should trigger {@link Geofence#GEOFENCE_TRANSITION_DWELL} notification at the moment when the geofence is
     * added and if the device is already inside that geofence for some time.
     */
    public static final int INITIAL_TRIGGER_DWELL = 4;

    @Field(value = 1, subClass = ParcelableGeofence.class)
    private List<Geofence> geofences;
    @Field(2)
    private @InitialTrigger int initialTrigger;
    @Field(3)
    private String tag = "";
    @Field(4)
    @Nullable
    private String contextAttributionTag;

    /**
     * Gets the list of geofences to be monitored.
     *
     * @return the list of geofences to be monitored
     */
    public List<Geofence> getGeofences() {
        return geofences;
    }

    /**
     * Gets the triggering behavior at the moment when the geofences are added.
     *
     * @return the triggering behavior at the moment when the geofences are added. It's a bit-wise of {@link #INITIAL_TRIGGER_ENTER},
     * {@link #INITIAL_TRIGGER_EXIT}, and {@link #INITIAL_TRIGGER_DWELL}.
     */
    public @InitialTrigger int getInitialTrigger() {
        return initialTrigger;
    }


    /**
     * The triggering behavior at the moment when the geofences are added. It's either 0, or a bit-wise OR of {@link GeofencingRequest#INITIAL_TRIGGER_ENTER},
     * {@link GeofencingRequest#INITIAL_TRIGGER_EXIT}, and {@link GeofencingRequest#INITIAL_TRIGGER_DWELL}.
     */
    @Target({ElementType.TYPE_USE})
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {INITIAL_TRIGGER_ENTER, INITIAL_TRIGGER_EXIT, INITIAL_TRIGGER_DWELL}, flag = true)
    @interface InitialTrigger {

    }

    /**
     * A builder that builds {@link GeofencingRequest}.
     */
    public static class Builder {
        private List<Geofence> geofences = new ArrayList<>();
        private @InitialTrigger int initialTrigger = INITIAL_TRIGGER_ENTER | INITIAL_TRIGGER_DWELL;

        /**
         * Adds a geofence to be monitored by geofencing service.
         *
         * @param geofence the geofence to be monitored. The geofence must be built with {@link Geofence.Builder}.
         * @return the builder object itself for method chaining
         * @throws IllegalArgumentException if the geofence is not built with {@link Geofence.Builder}.
         * @throws NullPointerException     if the given geofence is null
         */
        @NonNull
        public GeofencingRequest.Builder addGeofence(Geofence geofence) {
            if (geofence == null) throw new NullPointerException("geofence can't be null.");
            if (!(geofence instanceof ParcelableGeofence)) throw new IllegalArgumentException("Geofence must be created using Geofence.Builder.");
            geofences.add(geofence);
            return this;
        }

        /**
         * Adds all the geofences in the given list to be monitored by geofencing service.
         *
         * @param geofences the geofences to be monitored. The geofences in the list must be built with {@link Geofence.Builder}.
         * @return the builder object itself for method chaining
         * @throws IllegalArgumentException if the geofence is not built with {@link Geofence.Builder}.
         */
        @NonNull
        public GeofencingRequest.Builder addGeofences(List<Geofence> geofences) {
            if (geofences != null) {
                for (Geofence geofence : geofences) {
                    if (geofence != null) addGeofence(geofence);
                }
            }
            return this;
        }

        /**
         * Sets the geofence notification behavior at the moment when the geofences are added. The default behavior is
         * {@link GeofencingRequest#INITIAL_TRIGGER_ENTER} and {@link GeofencingRequest#INITIAL_TRIGGER_DWELL}.
         *
         * @param initialTrigger the notification behavior. It's a bit-wise of {@link GeofencingRequest#INITIAL_TRIGGER_ENTER} and/or
         *                       {@link GeofencingRequest#INITIAL_TRIGGER_EXIT} and/or {@link GeofencingRequest#INITIAL_TRIGGER_DWELL}. When
         *                       {@code initialTrigger} is set to 0 ({@code setInitialTrigger(0)}), initial trigger would be disabled.
         * @return the builder object itself for method chaining
         */
        public GeofencingRequest.Builder setInitialTrigger(@InitialTrigger int initialTrigger) {
            this.initialTrigger = initialTrigger;
            return this;
        }

        /**
         * Builds the {@link GeofencingRequest} object.
         *
         * @return a {@link GeofencingRequest} object
         * @throws IllegalArgumentException if no geofence has been added to this list
         */
        public GeofencingRequest build() {
            if (geofences.isEmpty()) throw new IllegalArgumentException("No geofence has been added to this request.");
            GeofencingRequest request = new GeofencingRequest();
            request.geofences = geofences;
            request.initialTrigger = initialTrigger;
            return request;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "GeofencingRequest[geofences=" + this.geofences + ", initialTrigger=" + this.initialTrigger + ", tag=" + this.tag + ", attributionTag=" + this.contextAttributionTag + "]";
    }

    public static final Creator<GeofencingRequest> CREATOR = new AutoCreator<>(GeofencingRequest.class);
}
