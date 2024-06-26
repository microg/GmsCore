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

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;
import com.google.android.gms.maps.model.StreetViewSource;

@SafeParcelable.Class
public class StreetViewPanoramaOptions extends AbstractSafeParcelable {
    @Field(2)
    public StreetViewPanoramaCamera panoramaCamera;

    @Field(3)
    public String panoramaId;

    @Field(4)
    public LatLng position;

    @Field(5)
    public Integer radius;

    @Field(6)
    private Boolean userNavigationEnabled;

    @Field(7)
    private Boolean zoomGesturesEnabled;

    @Field(8)
    private Boolean panningGesturesEnabled;

    @Field(9)
    private Boolean streetNamesEnabled;

    @Field(10)
    private Boolean useViewLifecycleInFragment;

    @Field(11)
    public StreetViewSource source;

    public static final SafeParcelableCreatorAndWriter<StreetViewPanoramaOptions> CREATOR = findCreator(StreetViewPanoramaOptions.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

}
