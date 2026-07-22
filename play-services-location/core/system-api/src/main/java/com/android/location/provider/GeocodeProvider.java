/*
 * SPDX-FileCopyrightText: 2010, The Android Open Source Project
 * SPDX-FileCopyrightText: 2014, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.location.provider;

import android.location.Address;
import android.location.GeocoderParams;
import android.os.IBinder;

import java.util.List;

/**
 * Base class for geocode providers implemented as unbundled services.
 * <p/>
 * <p>Geocode providers can be implemented as services and return the result of
 * {@link GeocodeProvider#getBinder()} in its getBinder() method.
 * <p/>
 * <p>IMPORTANT: This class is effectively a public API for unbundled
 * applications, and must remain API stable. See README.txt in the root
 * of this package for more information.
 */
public abstract class GeocodeProvider {
    /**
     * This method is overridden to implement the
     * {@link android.location.Geocoder#getFromLocation(double, double, int)} method.
     * Classes implementing this method should not hold a reference to the params parameter.
     */
    public abstract String onGetFromLocation(double latitude, double longitude, int maxResults,
            GeocoderParams params, List<Address> addrs);

    /**
     * This method is overridden to implement the
     * {@link android.location.Geocoder#getFromLocationName(String, int, double, double, double, double)} method.
     * Classes implementing this method should not hold a reference to the params parameter.
     */
    public abstract String onGetFromLocationName(String locationName,
            double lowerLeftLatitude, double lowerLeftLongitude,
            double upperRightLatitude, double upperRightLongitude, int maxResults,
            GeocoderParams params, List<Address> addrs);

    /**
     * Returns the Binder interface for the geocode provider.
     * This is intended to be used for the onBind() method of
     * a service that implements a geocoder service.
     *
     * @return the IBinder instance for the provider
     */
    public IBinder getBinder() {
        return null;
    }
}
