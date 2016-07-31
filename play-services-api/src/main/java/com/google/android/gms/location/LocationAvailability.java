/*
 * Copyright 2013-2015 microG Project Team
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

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class LocationAvailability extends AutoSafeParcelable {

    @SafeParceled(1000)
    private int versionCode = 1;

    @SafeParceled(1)
    private int cellStatus;

    @SafeParceled(2)
    private int wifiStatus;

    @SafeParceled(3)
    private long elapsedRealtimeNs;

    @SafeParceled(4)
    private int locationStatus;

    private LocationAvailability(int cellStatus, int wifiStatus, int elapsedRealtimeNs, int locationStatus) {
        this.cellStatus = cellStatus;
        this.wifiStatus = wifiStatus;
        this.elapsedRealtimeNs = elapsedRealtimeNs;
        this.locationStatus = this.locationStatus;
    }

    public static LocationAvailability create() { // TODO
        return new LocationAvailability(0, 0, 0, 0);
    }

    public static final Creator<LocationAvailability> CREATOR = new AutoCreator<LocationAvailability>(LocationAvailability.class);
}
