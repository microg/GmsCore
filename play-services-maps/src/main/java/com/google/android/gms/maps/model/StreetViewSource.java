/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.maps.model;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaOptions;

/**
 * Identifiers to limit Street View searches to selected sources. See {@link StreetViewPanorama#setPosition(LatLng, StreetViewSource)},
 * {@link StreetViewPanorama#setPosition(LatLng, int, StreetViewSource)}, {@link StreetViewPanoramaOptions#position(LatLng, Integer, StreetViewSource)} or
 * {@link StreetViewPanoramaOptions#position(LatLng, StreetViewSource)}.
 */
@SafeParcelable.Class
public class StreetViewSource extends AbstractSafeParcelable {

    /**
     * Default: Uses the default sources of Street View, searches will not be limited to specific sources.
     */
    public static final StreetViewSource DEFAULT = new StreetViewSource(0);
    /**
     * Limits Street View searches to outdoor collections. Indoor collections are not included in search results. Note also that the search only
     * returns panoramas where it's possible to determine whether they're indoors or outdoors. For example, photo spheres are not returned
     * because it's unknown whether they are indoors or outdoors.
     */
    public static final StreetViewSource OUTDOOR = new StreetViewSource(1);

    @Field(2)
    final int type;

    @Constructor
    StreetViewSource(@Param(2) int type) {
        this.type = type;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StreetViewSource)) return false;

        StreetViewSource that = (StreetViewSource) o;
        return type == that.type;
    }

    @Override
    public int hashCode() {
        return type;
    }

    @NonNull
    @Override
    public String toString() {
        switch (type) {
            case 0:
                return "StreetViewSource:DEFAULT";
            case 1:
                return "StreetViewSource:OUTDOOR";
            default:
                return "StreetViewSource:UNKNOWN(" + type + ")";
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<StreetViewSource> CREATOR = findCreator(StreetViewSource.class);
}
