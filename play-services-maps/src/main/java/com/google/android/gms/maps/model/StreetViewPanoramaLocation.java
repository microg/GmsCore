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
 * An immutable class that contains details of the user's current Street View panorama
 */
@SafeParcelable.Class
public class StreetViewPanoramaLocation extends AbstractSafeParcelable {
    /**
     * Array of {@link StreetViewPanoramaLink} able to be reached from the current position
     */
    @NonNull
    @Field(2)
    public final StreetViewPanoramaLink[] links;
    /**
     * The location of the current Street View panorama
     */
    @NonNull
    @Field(3)
    public final LatLng position;
    /**
     * The panorama ID of the current Street View panorama
     */
    @NonNull
    @Field(4)
    public final String panoId;

    /**
     * Constructs a StreetViewPanoramaLocation.
     *
     * @param links    List of {@link StreetViewPanoramaLink} reachable from the current position. Must not be {@code null}.
     * @param position The location of the current Street View panorama. Must not be {@code null}.
     * @param panoId   Identification string for the current Street View panorama. Must not be {@code null}.
     */
    @Constructor
    public StreetViewPanoramaLocation(@NonNull @Param(2) StreetViewPanoramaLink[] links, @NonNull @Param(3) LatLng position, @NonNull @Param(4) String panoId) {
        this.links = links;
        this.position = position;
        this.panoId = panoId;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StreetViewPanoramaLocation)) return false;

        StreetViewPanoramaLocation that = (StreetViewPanoramaLocation) o;
        return position.equals(that.position) && panoId.equals(that.panoId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(new Object[]{this.position, this.panoId});
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("StreetViewPanoramaLocation")
                .field("panoId", panoId)
                .field("position", position)
                .end();
    }

    public static final SafeParcelableCreatorAndWriter<StreetViewPanoramaLocation> CREATOR = findCreator(StreetViewPanoramaLocation.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
