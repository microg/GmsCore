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

import android.location.Location;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.List;

@PublicApi
public class LocationResult extends AutoSafeParcelable {

    @SafeParceled(1000)
    private final int versionCode = 2;

    @SafeParceled(value = 1, subClass = Location.class)
    public final List<Location> locations;

    private LocationResult(List<Location> locations) {
        this.locations = locations;
    }

    public static LocationResult create(List<Location> locations) {
        return new LocationResult(locations);
    }

    public static final Creator<LocationResult> CREATOR = new AutoCreator<LocationResult>(LocationResult.class);
}
