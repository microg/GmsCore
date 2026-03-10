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

package com.google.android.gms.maps.model;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

@PublicApi
public class VisibleRegion extends AutoSafeParcelable {
    @SafeParceled(1)
    private int versionCode;
    @SafeParceled(2)
    private LatLng nearLeft;
    @SafeParceled(3)
    private LatLng nearRight;
    @SafeParceled(4)
    private LatLng farLeft;
    @SafeParceled(5)
    private LatLng farRight;
    @SafeParceled(6)
    private LatLngBounds bounds;

    private VisibleRegion() {
    }

    public VisibleRegion(int versionCode, LatLng nearLeft, LatLng nearRight, LatLng farLeft,
            LatLng farRight, LatLngBounds bounds) {
        this.versionCode = versionCode;
        this.nearLeft = nearLeft;
        this.nearRight = nearRight;
        this.farLeft = farLeft;
        this.farRight = farRight;
        this.bounds = bounds;
    }

    public VisibleRegion(LatLng nearLeft, LatLng nearRight, LatLng farLeft, LatLng farRight,
            LatLngBounds bounds) {
        this(1, nearLeft, nearRight, farLeft, farRight, bounds);
    }

    /**
     * This is assuming that the visible region matches the bounds, which means that it's a north
     * orientated top view
     */
    public VisibleRegion(LatLngBounds bounds) {
        this(bounds.southwest, new LatLng(bounds.southwest.latitude, bounds.northeast.longitude),
                new LatLng(bounds.northeast.latitude, bounds.southwest.longitude), bounds.northeast,
                bounds);
    }

    public static Creator<VisibleRegion> CREATOR = new AutoCreator<VisibleRegion>(VisibleRegion.class);
}
