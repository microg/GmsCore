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

public class GroundOverlayOptions implements SafeParcelable {
    @SafeParceled(1)
    private int versionCode;
    @SafeParceled(2)
    private IBinder wrappedImage;
    @SafeParceled(3)
    private LatLng location;
    @SafeParceled(4)
    private float width;
    @SafeParceled(5)
    private float height;
    @SafeParceled(6)
    private LatLngBounds bounds;
    @SafeParceled(7)
    private float bearing;
    @SafeParceled(8)
    private float zIndex;
    @SafeParceled(9)
    private boolean visible;
    @SafeParceled(10)
    private float transparency;
    @SafeParceled(11)
    private float anchorU;
    @SafeParceled(12)
    private float anchorV;

    public GroundOverlayOptions() {
    }

    private GroundOverlayOptions(Parcel in) {
        SafeParcelUtil.readObject(this, in);
        // wrappedImage = new BitmapDescriptor(IObjectWrapper.Stub.asInterface(SafeReader.readBinder(in, position)));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        SafeParcelUtil.writeObject(this, dest, flags);
        // SafeParcelWriter.write(dest, 2, wrappedImage.getRemoteObject().asBinder(), false);
    }

    public static Creator<GroundOverlayOptions> CREATOR = new Creator<GroundOverlayOptions>() {
        public GroundOverlayOptions createFromParcel(Parcel source) {
            return new GroundOverlayOptions(source);
        }

        public GroundOverlayOptions[] newArray(int size) {
            return new GroundOverlayOptions[size];
        }
    };
}
