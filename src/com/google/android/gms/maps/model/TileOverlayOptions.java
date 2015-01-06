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

public class TileOverlayOptions implements SafeParcelable {

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        SafeParcelUtil.writeObject(this, dest, flags);
    }

    public TileOverlayOptions() {
    }

    private TileOverlayOptions(Parcel in) {
        SafeParcelUtil.readObject(this, in);
    }

    public static Creator<TileOverlayOptions> CREATOR = new Creator<TileOverlayOptions>() {
        public TileOverlayOptions createFromParcel(Parcel source) {
            return new TileOverlayOptions(source);
        }

        public TileOverlayOptions[] newArray(int size) {
            return new TileOverlayOptions[size];
        }
    };
}
