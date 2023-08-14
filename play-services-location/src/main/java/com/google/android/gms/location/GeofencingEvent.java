/*
 * SPDX-FileCopyrightText: 2017 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;

import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer;
import com.google.android.gms.location.internal.ParcelableGeofence;

import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an event from the GeofencingApi API. The event can be
 * <p>
 * A geofence triggering event generated when a geofence transition happens.
 * An error happens after geofences are registered and being monitored.
 */
@PublicApi
public class GeofencingEvent {
    @Hide
    public static final String EXTRA_ERROR_CODE = "gms_error_code";
    @Hide
    public static final String EXTRA_TRIGGERING_LOCATION = "com.google.android.location.intent.extra.triggering_location";
    @Hide
    public static final String EXTRA_TRANSITION = "com.google.android.location.intent.extra.transition";
    @Hide
    public static final String EXTRA_GEOFENCE_LIST = "com.google.android.location.intent.extra.geofence_list";

    private int errorCode;
    private @Geofence.GeofenceTransition int geofenceTransition;
    private List<Geofence> triggeringGeofences;
    private Location triggeringLocation;

    /**
     * Creates a {@link GeofencingEvent} object from the given intent.
     *
     * @param intent the intent to extract the geofencing event data from
     * @return a {@link GeofencingEvent} object or {@code null} if the given intent is {@code null}
     */

    public static GeofencingEvent fromIntent(Intent intent) {
        if (intent == null) {
            return null;
        }
        GeofencingEvent event = new GeofencingEvent();
        event.errorCode = intent.getIntExtra(EXTRA_ERROR_CODE, -1);
        event.geofenceTransition = validate(intent.getIntExtra(EXTRA_TRANSITION, -1));
        ArrayList<byte[]> parceledGeofences = (ArrayList<byte[]>) intent.getSerializableExtra(EXTRA_GEOFENCE_LIST);
        if (parceledGeofences != null) {
            event.triggeringGeofences = new ArrayList<Geofence>();
            for (byte[] parceledGeofence : parceledGeofences) {
                event.triggeringGeofences.add(SafeParcelableSerializer.deserializeFromBytes(parceledGeofence, ParcelableGeofence.CREATOR));
            }
        }
        event.triggeringLocation = intent.getParcelableExtra(EXTRA_TRIGGERING_LOCATION);
        return event;
    }

    @SuppressLint("WrongConstant")
    private static @Geofence.GeofenceTransition int validate(@Geofence.GeofenceTransition int geofenceTransition) {
        if (geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER && geofenceTransition != Geofence.GEOFENCE_TRANSITION_EXIT && geofenceTransition != Geofence.GEOFENCE_TRANSITION_DWELL) {
            return -1;
        }
        return geofenceTransition;
    }

    /**
     * Returns the error code that explains the error that triggered the intent specified in
     * {@link #fromIntent(Intent)}.
     *
     * @return the error code specified in {@link GeofenceStatusCodes} or {@code -1} if
     * {@link #hasError()} returns false.
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the transition type of the geofence transition alert.
     *
     * @return -1 if the intent specified in {@link #fromIntent(Intent)} is not generated for a
     * transition alert; Otherwise returns the GEOFENCE_TRANSITION_ flags value defined in
     * {@link Geofence}.
     */
    public @Geofence.GeofenceTransition int getGeofenceTransition() {
        return geofenceTransition;
    }


    /**
     * Returns a list of geofences that triggered this geofence transition alert.
     *
     * @return a list of geofences that triggered this geofence transition alert or {@code null} if
     * the intent specified in {@link #fromIntent(Intent)} is not generated for a geofence
     * transition alert
     */
    public List<Geofence> getTriggeringGeofences() {
        return triggeringGeofences;
    }

    /**
     * Gets the location that triggered the geofence transition. Triggering location is only
     * available if the calling app links against Google Play services 5.0 SDK.
     *
     * @return the location that triggered this geofence alert or {@code null} if it's not included
     * in the intent specified in {@link #fromIntent(Intent)}
     */
    public Location getTriggeringLocation() {
        return triggeringLocation;
    }

    /**
     * Whether an error triggered this intent.
     *
     * @return {@code true} if an error triggered the intent specified in
     * {@link #fromIntent(Intent)}, otherwise {@code false}
     */
    public boolean hasError() {
        return errorCode != -1;
    }
}
