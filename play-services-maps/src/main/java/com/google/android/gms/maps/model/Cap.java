/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.maps.model;

import android.os.IBinder;

import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Objects;

/**
 * Immutable cap that can be applied at the start or end vertex of a {@link Polyline}.
 */
public class Cap extends AutoSafeParcelable {
    @Field(2)
    public final int type;
    @Field(3)
    @Nullable
    private final IBinder bitmap;
    /**
     * Descriptor of the bitmap to be overlaid at the start or end vertex.
     */
    @Nullable
    private final BitmapDescriptor bitmapDescriptor;
    /**
     * Reference stroke width (in pixels) - the stroke width for which the cap bitmap at its native dimension is designed.
     * The default reference stroke width is 10 pixels.
     */
    @Field(4)
    @Nullable
    private final Float refWidth;

    private Cap() {
        type = 0;
        bitmap = null;
        bitmapDescriptor = null;
        refWidth = 0.0f;
    }

    protected Cap(int type, @Nullable BitmapDescriptor bitmapDescriptor, @Nullable Float refWidth) {
        this.type = type;
        this.bitmap = bitmapDescriptor == null ? null : bitmapDescriptor.getRemoteObject().asBinder();
        this.bitmapDescriptor = bitmapDescriptor;
        this.refWidth = refWidth;
    }

    @NonNull
    @Override
    public String toString() {
        return "[Cap: type=" + type + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cap)) return false;

        Cap cap = (Cap) o;

        if (type != cap.type) return false;
        if (!Objects.equals(bitmapDescriptor, cap.bitmapDescriptor)) return false;
        return Objects.equals(refWidth, cap.refWidth);
    }

    @Override
    public int hashCode() {
        int result = type;
        result = 31 * result + (bitmapDescriptor != null ? bitmapDescriptor.hashCode() : 0);
        result = 31 * result + (refWidth != null ? refWidth.hashCode() : 0);
        return result;
    }

    public static final Creator<Cap> CREATOR = new AutoCreator<Cap>(Cap.class) {
        @Override
        public Cap createFromParcel(Parcel parcel) {
            Cap item = super.createFromParcel(parcel);
            switch (item.type) {
                case 0:
                    return new ButtCap();
                case 1:
                    return new SquareCap();
                case 2:
                    return new RoundCap();
                case 3:
                    if (item.refWidth != null) {
                        return new CustomCap(item.bitmapDescriptor, item.refWidth);
                    } else {
                        return new CustomCap(item.bitmapDescriptor);
                    }
                default:
                    return item;
            }
        }
    };
}
