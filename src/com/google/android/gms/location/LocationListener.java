package com.google.android.gms.location;

import android.location.Location;

/**
 * Used for receiving notifications from the {@link FusedLocationProviderApi} when the location has
 * changed. The methods are called if the LocationListener has been registered with the location
 * client.
 */
public interface LocationListener {

    /**
     * Called when the location has changed.
     *
     * @param location The updated location.
     */
    public void onLocationChanged(Location location);
}
