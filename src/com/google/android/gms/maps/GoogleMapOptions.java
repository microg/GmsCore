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

package com.google.android.gms.maps;

import android.os.Parcel;
import com.google.android.gms.maps.model.CameraPosition;
import org.microg.safeparcel.SafeParcelUtil;
import org.microg.safeparcel.SafeParcelable;
import org.microg.safeparcel.SafeParceled;

public final class GoogleMapOptions implements SafeParcelable {
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
    private boolean scrollGesturesEnabled;
    @SafeParceled(9)
    private boolean zoomGesturesEnabled;
    @SafeParceled(10)
    private boolean tiltGesturesEnabled;
    @SafeParceled(11)
    private boolean rotateGesturesEnabled;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        SafeParcelUtil.writeObject(this, dest, flags);
    }

    public GoogleMapOptions() {
    }

    private GoogleMapOptions(Parcel in) {
        SafeParcelUtil.readObject(this, in);
    }

    public int getMapType() {
        return mapType;
    }

    public CameraPosition getCamera() {
        return camera;
    }

    public boolean isZoomControlsEnabled() {
        return zoomControlsEnabled;
    }

    public boolean isCompassEnabled() {
        return compassEnabled;
    }

    public boolean isScrollGesturesEnabled() {
        return scrollGesturesEnabled;
    }

    public boolean isZoomGesturesEnabled() {
        return zoomGesturesEnabled;
    }

    public boolean isTiltGesturesEnabled() {
        return tiltGesturesEnabled;
    }

    public boolean isRotateGesturesEnabled() {
        return rotateGesturesEnabled;
    }

    public static Creator<GoogleMapOptions> CREATOR = new Creator<GoogleMapOptions>() {
        public GoogleMapOptions createFromParcel(Parcel source) {
            return new GoogleMapOptions(source);
        }

        public GoogleMapOptions[] newArray(int size) {
            return new GoogleMapOptions[size];
        }
    };
}
