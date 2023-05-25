/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import android.app.Activity;
import android.content.Intent;
import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParcelUtil;
import org.microg.safeparcel.SafeParceled;

/**
 * Stores the current states of all location-related settings.
 */
@PublicApi
public class LocationSettingsStates extends AutoSafeParcelable {

    @Field(1000)
    private int versionCode = 2;

    @Field(1)
    private boolean gpsUsable;

    @Field(2)
    private boolean networkLocationUsable;

    @Field(3)
    private boolean bleUsable;

    @Field(4)
    private boolean gpsPresent;

    @Field(5)
    private boolean networkLocationPresent;

    @Field(6)
    private boolean blePresent;

    /**
     * Whether BLE is present on the device.
     */
    public boolean isBlePresent() {
        return blePresent;
    }

    /**
     * Whether BLE is enabled and is usable by the app.
     */
    public boolean isBleUsable() {
        return bleUsable;
    }

    /**
     * Whether GPS provider is present on the device.
     */
    public boolean isGpsPresent() {
        return gpsPresent;
    }

    /**
     * Whether GPS provider is enabled and is usable by the app.
     */
    public boolean isGpsUsable() {
        return gpsUsable;
    }

    /**
     * Whether location is present on the device.
     * <p>
     * This method returns true when either GPS or network location provider is present.
     */
    public boolean isLocationPresent() {
        return isGpsPresent() || isNetworkLocationPresent();
    }

    /**
     * Whether location is enabled and is usable by the app.
     * <p>
     * This method returns true when either GPS or network location provider is usable.
     */
    public boolean isLocationUsable() {
        return isGpsUsable() || isNetworkLocationUsable();
    }

    /**
     * Whether network location provider is present on the device.
     */
    public boolean isNetworkLocationPresent() {
        return networkLocationPresent;
    }

    /**
     * Whether network location provider is enabled and usable by the app.
     */
    public boolean isNetworkLocationUsable() {
        return networkLocationUsable;
    }

    public LocationSettingsStates(boolean gpsUsable, boolean networkLocationUsable, boolean bleUsable, boolean gpsPresent, boolean networkLocationPresent, boolean blePresent) {
        this.gpsUsable = gpsUsable;
        this.networkLocationUsable = networkLocationUsable;
        this.bleUsable = bleUsable;
        this.gpsPresent = gpsPresent;
        this.networkLocationPresent = networkLocationPresent;
        this.blePresent = blePresent;
    }

    /**
     * Retrieves the location settings states from the intent extras. When the location settings dialog finishes, you can use this method to retrieve the
     * current location settings states from the intent in your {@link Activity#onActivityResult(int, int, Intent)};
     */
    public static LocationSettingsStates fromIntent(Intent intent) {
        byte[] bytes = intent.getByteArrayExtra(EXTRA_NAME);
        if (bytes == null) return null;
        return SafeParcelUtil.fromByteArray(bytes, CREATOR);
    }

    public static final Creator<LocationSettingsStates> CREATOR = new AutoCreator<LocationSettingsStates>(LocationSettingsStates.class);

    private static final String EXTRA_NAME = "com.google.android.gms.location.LOCATION_SETTINGS_STATES";
}
