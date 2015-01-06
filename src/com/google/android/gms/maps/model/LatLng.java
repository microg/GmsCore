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

public class LatLng implements SafeParcelable {
    public static Creator<LatLng> CREATOR = new Creator<LatLng>() {
        public LatLng createFromParcel(Parcel source) {
            return new LatLng(source);
        }

        public LatLng[] newArray(int size) {
            return new LatLng[size];
        }
    };
    @SafeParceled(2)
    public double latitude;
    @SafeParceled(3)
    public double longitude;
    @SafeParceled(1)
    private int versionCode;

    public LatLng(int versionCode, double latitude, double longitude) {
        this.versionCode = versionCode;
        this.latitude = Math.max(-90, Math.min(90, latitude));
        if ((-180 <= longitude) && (longitude < 180)) {
            this.longitude = longitude;
        } else {
            this.longitude = ((360 + (longitude - 180) % 360) % 360 - 180);
        }
    }

    private LatLng(Parcel in) {
        SafeParcelUtil.readObject(this, in);
    }

    public LatLng(double latitude, double longitude) {
        this(1, latitude, longitude);
    }

    @Override
    public final int hashCode() {
        long lat = Double.doubleToLongBits(latitude);
        int tmp = 31 + (int) (lat ^ lat >>> 32);
        long lon = Double.doubleToLongBits(longitude);
        return tmp * 31 + (int) (lon ^ lon >>> 32);
    }

    @Override
    public String toString() {
        return "lat/lng: (" + latitude + "," + longitude + ")";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        SafeParcelUtil.writeObject(this, dest, flags);
    }
}
