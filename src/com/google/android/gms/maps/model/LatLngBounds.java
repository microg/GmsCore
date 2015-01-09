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

/**
 * An immutable class representing a latitude/longitude aligned rectangle.
 */
public final class LatLngBounds implements SafeParcelable {
    @SafeParceled(1)
    private final int versionCode;
    /**
     * Southwest corner of the bound.
     */
    @SafeParceled(2)
    public final LatLng southwest;
    /**
     * Northeast corner of the bound.
     */
    @SafeParceled(3)
    public final LatLng northeast;

    /**
     * This constructor is dirty setting the final fields to make the compiler happy.
     * In fact, those are replaced by their real values later using SafeParcelUtil.
     */
    private LatLngBounds() {
        this.versionCode = -1;
        southwest = northeast = null;
    }

    private LatLngBounds(Parcel in) {
        this();
        SafeParcelUtil.readObject(this, in);
    }

    public LatLngBounds(LatLng southwest, LatLng northeast) throws IllegalArgumentException {
        this.versionCode = 1;
        this.southwest = southwest;
        this.northeast = northeast;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        SafeParcelUtil.writeObject(this, dest, flags);
    }

    public static Creator<LatLngBounds> CREATOR = new Creator<LatLngBounds>() {
        public LatLngBounds createFromParcel(Parcel source) {
            return new LatLngBounds(source);
        }

        public LatLngBounds[] newArray(int size) {
            return new LatLngBounds[size];
        }
    };
}
