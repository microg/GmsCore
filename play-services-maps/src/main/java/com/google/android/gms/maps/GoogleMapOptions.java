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

package com.google.android.gms.maps;

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLngBounds;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public final class GoogleMapOptions extends AutoSafeParcelable {
    @SafeParceled(1)
    private int versionCode;
    @SafeParceled(2)
    private int zOrderOnTop;
    @SafeParceled(3)
    private boolean useViewLifecycleInFragment;
    @SafeParceled(4)
    private int mapType;
    @SafeParceled(5)
    private CameraPosition camera;
    @SafeParceled(6)
    private boolean zoomControlsEnabled;
    @SafeParceled(7)
    private boolean compassEnabled;
    @SafeParceled(8)
    private boolean scrollGesturesEnabled = true;
    @SafeParceled(9)
    private boolean zoomGesturesEnabled = true;
    @SafeParceled(10)
    private boolean tiltGesturesEnabled = true;
    @SafeParceled(11)
    private boolean rotateGesturesEnabled = true;
    @SafeParceled(12)
    private boolean liteMode = false;
    @SafeParceled(14)
    private boolean mapToobarEnabled = false;
    @SafeParceled(15)
    private boolean ambientEnabled = false;
    @SafeParceled(16)
    private float minZoom;
    @SafeParceled(17)
    private float maxZoom;
    @SafeParceled(18)
    private LatLngBounds boundsForCamera;
    @SafeParceled(19)
    private boolean scrollGesturesEnabledDuringRotateOrZoom = true;

    public GoogleMapOptions() {
    }

    public Boolean getAmbientEnabled() {
        return ambientEnabled;
    }

    public CameraPosition getCamera() {
        return camera;
    }

    public Boolean getCompassEnabled() {
        return compassEnabled;
    }

    public LatLngBounds getLatLngBoundsForCameraTarget() {
        return boundsForCamera;
    }

    public Boolean getLiteMode() {
        return liteMode;
    }

    public Boolean getMapToolbarEnabled() {
        return mapToobarEnabled;
    }

    public int getMapType() {
        return mapType;
    }

    public Float getMaxZoomPreference() {
        return maxZoom;
    }

    public Float getMinZoomPreference() {
        return minZoom;
    }

    public Boolean getRotateGesturesEnabled() {
        return rotateGesturesEnabled;
    }

    public Boolean getScrollGesturesEnabled() {
        return scrollGesturesEnabled;
    }

    public Boolean getScrollGesturesEnabledDuringRotateOrZoom() {
        return scrollGesturesEnabledDuringRotateOrZoom;
    }

    public Boolean getTiltGesturesEnabled() {
        return tiltGesturesEnabled;
    }

    public Boolean getUseViewLifecycleInFragment() {
        return useViewLifecycleInFragment;
    }

    public Boolean getZOrderOnTop() {
        return zOrderOnTop == 1; // TODO
    }

    public Boolean getZoomControlsEnabled() {
        return zoomControlsEnabled;
    }

    public Boolean getZoomGesturesEnabled() {
        return zoomGesturesEnabled;
    }

    @Deprecated
    public boolean isCompassEnabled() {
        return compassEnabled;
    }

    @Deprecated
    public boolean isZoomControlsEnabled() {
        return zoomControlsEnabled;
    }

    @Deprecated
    public boolean isScrollGesturesEnabled() {
        return scrollGesturesEnabled;
    }

    @Deprecated
    public boolean isZoomGesturesEnabled() {
        return zoomGesturesEnabled;
    }

    @Deprecated
    public boolean isTiltGesturesEnabled() {
        return tiltGesturesEnabled;
    }

    @Deprecated
    public boolean isRotateGesturesEnabled() {
        return rotateGesturesEnabled;
    }


    public static Creator<GoogleMapOptions> CREATOR = new AutoCreator<GoogleMapOptions>(GoogleMapOptions.class);
}
