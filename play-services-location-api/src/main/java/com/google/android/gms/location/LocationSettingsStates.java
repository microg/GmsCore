/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.location;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

/**
 * Stores the current states of all location-related settings.
 */
@PublicApi
public class LocationSettingsStates extends AutoSafeParcelable {

    @SafeParceled(1000)
    private int versionCode = 2;

    @SafeParceled(1)
    private boolean gpsUsable;

    @SafeParceled(2)
    private boolean networkLocationUsable;

    @SafeParceled(3)
    private boolean bleUsable;

    @SafeParceled(4)
    private boolean gpsPresent;

    @SafeParceled(5)
    private boolean networkLocationPresent;

    @SafeParceled(6)
    private boolean blePresent;

    public boolean isBlePresent() {
        return blePresent;
    }

    public boolean isBleUsable() {
        return bleUsable;
    }

    public boolean isGpsPresent() {
        return gpsPresent;
    }

    public boolean isGpsUsable() {
        return gpsUsable;
    }

    public boolean isLocationPresent() {
        return isGpsPresent() || isNetworkLocationPresent();
    }

    public boolean isLocationUsable() {
        return isGpsUsable() || isNetworkLocationUsable();
    }

    public boolean isNetworkLocationPresent() {
        return networkLocationPresent;
    }

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

    public static final Creator<LocationSettingsStates> CREATOR = new AutoCreator<LocationSettingsStates>(LocationSettingsStates.class);
}
