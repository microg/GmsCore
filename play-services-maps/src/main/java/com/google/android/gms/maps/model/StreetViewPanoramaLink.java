/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
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
import org.microg.gms.utils.ToStringHelper;

import java.util.Objects;

/**
 * An immutable class that represents a link to another Street View panorama.
 */
@SafeParcelable.Class
public class StreetViewPanoramaLink extends AbstractSafeParcelable {
    /**
     * Panorama ID of the linked Street View panorama
     */
    @NonNull
    @Field(2)
    public final String panoId;
    /**
     * The direction of the linked Street View panorama, in degrees clockwise from north
     */
    @Field(3)
    public final float bearing;

    @Constructor
    StreetViewPanoramaLink(@NonNull @Param(2) String panoId, @Param(3) float bearing) {
        this.panoId = panoId;
        this.bearing = (bearing < 0.0f ? (bearing % 360.0f) + 360.0f : bearing) % 360.0f;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StreetViewPanoramaLink)) return false;

        StreetViewPanoramaLink that = (StreetViewPanoramaLink) o;
        return bearing == that.bearing && panoId.equals(that.panoId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[]{this.panoId, this.bearing});
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("StreetViewPanoramaLink")
                .field("panoId", panoId)
                .field("bearing", bearing)
                .end();
    }

    public static final SafeParcelableCreatorAndWriter<StreetViewPanoramaLink> CREATOR = findCreator(StreetViewPanoramaLink.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
