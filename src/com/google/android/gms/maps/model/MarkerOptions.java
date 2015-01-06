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

import android.os.IBinder;
import android.os.Parcel;
import org.microg.safeparcel.SafeParcelUtil;
import org.microg.safeparcel.SafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class MarkerOptions implements SafeParcelable {

    @SafeParceled(1)
    private int versionCode = 1;
    @SafeParceled(2)
    private LatLng position;
    @SafeParceled(3)
    private String title;
    @SafeParceled(4)
    private String snippet;
    @SafeParceled(5)
    private IBinder icon;
    @SafeParceled(6)
    private float anchorU = 0.5F;
    @SafeParceled(7)
    private float anchorV = 1F;
    @SafeParceled(8)
    private boolean draggable;
    @SafeParceled(9)
    private boolean visible;
    @SafeParceled(10)
    private boolean flat;
    @SafeParceled(11)
    private float rotation = 0F;
    @SafeParceled(12)
    private float infoWindowAnchorU = 0F;
    @SafeParceled(13)
    private float infoWindowAnchorV = 1F;
    @SafeParceled(14)
    private float alpha = 1F;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    public MarkerOptions() {
    }

    private MarkerOptions(Parcel in) {
        SafeParcelUtil.readObject(this, in);
        // this.icon = icon == null ? null : new BitmapDescriptor(ObjectWrapper.asInterface(icon));
    }

    public LatLng getPosition() {
        return position;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

    public IBinder getIcon() {
        return icon;
    }

    public float getAnchorU() {
        return anchorU;
    }

    public float getAnchorV() {
        return anchorV;
    }

    public boolean isDraggable() {
        return draggable;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isFlat() {
        return flat;
    }

    public float getRotation() {
        return rotation;
    }

    public float getInfoWindowAnchorU() {
        return infoWindowAnchorU;
    }

    public float getInfoWindowAnchorV() {
        return infoWindowAnchorV;
    }

    public float getAlpha() {
        return alpha;
    }

    public static Creator<MarkerOptions> CREATOR = new Creator<MarkerOptions>() {
        public MarkerOptions createFromParcel(Parcel source) {
            return new MarkerOptions(source);
        }

        public MarkerOptions[] newArray(int size) {
            return new MarkerOptions[size];
        }
    };
}
