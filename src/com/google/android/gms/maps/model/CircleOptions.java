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

public class CircleOptions implements SafeParcelable {
    public static Creator<CircleOptions> CREATOR = new Creator<CircleOptions>() {
        public CircleOptions createFromParcel(Parcel source) {
            return new CircleOptions(source);
        }

        public CircleOptions[] newArray(int size) {
            return new CircleOptions[size];
        }
    };
    @SafeParceled(1)
    private int versionCode;
    @SafeParceled(2)
    private LatLng center;
    @SafeParceled(3)
    private double radius;
    @SafeParceled(4)
    private float strokeWidth;
    @SafeParceled(5)
    private int strokeColor;
    @SafeParceled(6)
    private int fillColor;
    @SafeParceled(7)
    private float zIndex;
    @SafeParceled(8)
    private boolean visisble;

    public CircleOptions() {
    }

    private CircleOptions(Parcel in) {
        SafeParcelUtil.readObject(this, in);
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
