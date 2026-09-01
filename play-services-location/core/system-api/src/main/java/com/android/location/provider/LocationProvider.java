/*
 * SPDX-FileCopyrightText: 2010, The Android Open Source Project
 * SPDX-FileCopyrightText: 2014, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.location.provider;

import android.location.Criteria;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.WorkSource;

/**
 * An abstract superclass for location providers that are implemented
 * outside of the core android platform.
 * Location providers can be implemented as services and return the result of
 * {@link LocationProvider#getBinder()} in its getBinder() method.
 *
 * @hide
 */
public abstract class LocationProvider {

    public LocationProvider() {
    }

    /**
     * Returns the Binder interface for the location provider.
     * This is intended to be used for the onBind() method of
     * a service that implements a location provider service.
     *
     * @return the IBinder instance for the provider
     */
    public IBinder getBinder() {
        return null;
    }

    /**
     * Used by the location provider to report new locations.
     *
     * @param location new Location to report
     *                 <p/>
     *                 Requires the android.permission.INSTALL_LOCATION_PROVIDER permission.
     */
    public void reportLocation(Location location) {
    }

    /**
     * Returns true if the provider requires access to a
     * data network (e.g., the Internet), false otherwise.
     */
    public abstract boolean onRequiresNetwork();

    /**
     * Returns true if the provider requires access to a
     * satellite-based positioning system (e.g., GPS), false
     * otherwise.
     */
    public abstract boolean onRequiresSatellite();

    /**
     * Returns true if the provider requires access to an appropriate
     * cellular network (e.g., to make use of cell tower IDs), false
     * otherwise.
     */
    public abstract boolean onRequiresCell();

    /**
     * Returns true if the use of this provider may result in a
     * monetary charge to the user, false if use is free.  It is up to
     * each provider to give accurate information.
     */
    public abstract boolean onHasMonetaryCost();

    /**
     * Returns true if the provider is able to provide altitude
     * information, false otherwise.  A provider that reports altitude
     * under most circumstances but may occassionally not report it
     * should return true.
     */
    public abstract boolean onSupportsAltitude();

    /**
     * Returns true if the provider is able to provide speed
     * information, false otherwise.  A provider that reports speed
     * under most circumstances but may occassionally not report it
     * should return true.
     */
    public abstract boolean onSupportsSpeed();

    /**
     * Returns true if the provider is able to provide bearing
     * information, false otherwise.  A provider that reports bearing
     * under most circumstances but may occassionally not report it
     * should return true.
     */
    public abstract boolean onSupportsBearing();

    /**
     * Returns the power requirement for this provider.
     *
     * @return the power requirement for this provider, as one of the
     * constants Criteria.POWER_REQUIREMENT_*.
     */
    public abstract int onGetPowerRequirement();

    /**
     * Returns true if this provider meets the given criteria,
     * false otherwise.
     */
    public abstract boolean onMeetsCriteria(Criteria criteria);

    /**
     * Returns a constant describing horizontal accuracy of this provider.
     * If the provider returns finer grain or exact location,
     * {@link Criteria#ACCURACY_FINE} is returned, otherwise if the
     * location is only approximate then {@link Criteria#ACCURACY_COARSE}
     * is returned.
     */
    public abstract int onGetAccuracy();

    /**
     * Enables the location provider
     */
    public abstract void onEnable();

    /**
     * Disables the location provider
     */
    public abstract void onDisable();

    /**
     * Returns a information on the status of this provider.
     * {@link android.location.LocationProvider#OUT_OF_SERVICE} is returned if the provider is
     * out of service, and this is not expected to change in the near
     * future; {@link android.location.LocationProvider#TEMPORARILY_UNAVAILABLE} is returned if
     * the provider is temporarily unavailable but is expected to be
     * available shortly; and {@link android.location.LocationProvider#AVAILABLE} is returned
     * if the provider is currently available.
     * <p/>
     * <p> If extras is non-null, additional status information may be
     * added to it in the form of provider-specific key/value pairs.
     */
    public abstract int onGetStatus(Bundle extras);

    /**
     * Returns the time at which the status was last updated. It is the
     * responsibility of the provider to appropriately set this value using
     * {@link android.os.SystemClock#elapsedRealtime SystemClock.elapsedRealtime()}.
     * there is a status update that it wishes to broadcast to all its
     * listeners. The provider should be careful not to broadcast
     * the same status again.
     *
     * @return time of last status update in millis since last reboot
     */
    public abstract long onGetStatusUpdateTime();

    /**
     * Returns debugging information about the location provider.
     *
     * @return string describing the internal state of the location provider, or null.
     */
    public abstract String onGetInternalState();

    /**
     * Notifies the location provider that clients are listening for locations.
     * Called with enable set to true when the first client is added and
     * called with enable set to false when the last client is removed.
     * This allows the provider to prepare for receiving locations,
     * and to shut down when no clients are remaining.
     *
     * @param enable true if location tracking should be enabled.
     */
    public abstract void onEnableLocationTracking(boolean enable);

    /**
     * Notifies the location provider of the smallest minimum time between updates amongst
     * all clients that are listening for locations.  This allows the provider to reduce
     * the frequency of updates to match the requested frequency.
     *
     * @param minTime the smallest minTime value over all listeners for this provider.
     * @param ws      the source this work is coming from.
     */
    public abstract void onSetMinTime(long minTime, WorkSource ws);

    /**
     * Updates the network state for the given provider. This function must
     * be overwritten if {@link android.location.LocationProvider#requiresNetwork} returns true.
     * The state is {@link android.location.LocationProvider#TEMPORARILY_UNAVAILABLE} (disconnected)
     * OR {@link android.location.LocationProvider#AVAILABLE} (connected or connecting).
     *
     * @param state data state
     */
    @SuppressWarnings("deprecation")
    public abstract void onUpdateNetworkState(int state, android.net.NetworkInfo info);

    /**
     * Informs the provider when a new location has been computed by a different
     * location provider.  This is intended to be used as aiding data for the
     * receiving provider.
     *
     * @param location new location from other location provider
     */
    public abstract void onUpdateLocation(Location location);

    /**
     * Implements addditional location provider specific additional commands.
     *
     * @param command name of the command to send to the provider.
     * @param extras  optional arguments for the command (or null).
     *                The provider may optionally fill the extras Bundle with results from the command.
     * @return true if the command succeeds.
     */
    public abstract boolean onSendExtraCommand(String command, Bundle extras);

    /**
     * Notifies the location provider when a new client is listening for locations.
     *
     * @param uid user ID of the new client.
     * @param ws  a WorkSource representation of the client.
     */
    public abstract void onAddListener(int uid, WorkSource ws);

    /**
     * Notifies the location provider when a client is no longer listening for locations.
     *
     * @param uid user ID of the client no longer listening.
     * @param ws  a WorkSource representation of the client.
     */
    public abstract void onRemoveListener(int uid, WorkSource ws);
}
