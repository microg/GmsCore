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

import android.os.Parcel;
import org.microg.safeparcel.SafeParcelUtil;
import org.microg.safeparcel.SafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.Arrays;

public class CameraPosition implements SafeParcelable {
    @SafeParceled(1)
    private int versionCode;
    @SafeParceled(2)
    public LatLng target;
    @SafeParceled(3)
    public float zoom;
    @SafeParceled(4)
    public float tilt;
    @SafeParceled(5)
    public float bearing;

    private CameraPosition(Parcel in) {
        SafeParcelUtil.readObject(this, in);
    }

    public CameraPosition(int versionCode, LatLng target, float zoom, float tilt, float bearing) {
        this.versionCode = versionCode;
        if (target == null) {
            throw new NullPointerException("null camera target");
        }
        this.target = target;
        this.zoom = zoom;
        if (tilt < 0 || 90 < tilt) {
            throw new IllegalArgumentException("Tilt needs to be between 0 and 90 inclusive");
        }
        this.tilt = tilt;
        if (bearing <= 0) {
            bearing += 360;
        }
        this.bearing = bearing % 360;
    }

    public CameraPosition(LatLng target, float zoom, float tilt, float bearing) {
        this(1, target, zoom, tilt, bearing);
    }

    public static CameraPosition create(LatLng latLng) {
        return new CameraPosition(latLng, 0, 0, 0);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[] { target, zoom, tilt, bearing });
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        SafeParcelUtil.writeObject(this, dest, flags);
    }

    public static Creator<CameraPosition> CREATOR = new Creator<CameraPosition>() {
        public CameraPosition createFromParcel(Parcel source) {
            return new CameraPosition(source);
        }

        public CameraPosition[] newArray(int size) {
            return new CameraPosition[size];
        }
    };
}
