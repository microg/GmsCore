/*
 * Copyright (c) 2014 μg Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.maps.model;

import org.microg.gms.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.List;

/**
 * Defines options for a polyline.
 * TODO
 */
@PublicApi
public class PolylineOptions extends AutoSafeParcelable {
    @SafeParceled(1)
    private int versionCode;
    // TODO
    private List<LatLng> points;
    private float width;
    private int color;
    private float zIndex;
    private boolean visible;
    private boolean geodesic;

    public PolylineOptions() {
    }

    public static Creator<PolylineOptions> CREATOR = new AutoCreator<>(PolylineOptions.class);
}
