/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.maps.model;

import androidx.annotation.NonNull;

/**
 * Bitmap overlay centered at the start or end vertex of a {@link Polyline}, orientated according to the direction of the line's first
 * or last edge and scaled with respect to the line's stroke width. {@code CustomCap} can be applied to {@link Polyline} with any stroke pattern.
 */
public class CustomCap extends Cap {
    @NonNull
    public final BitmapDescriptor bitmapDescriptor;
    public final Float refWidth;

    /**
     * Constructs a new {@code CustomCap}.
     *
     * @param bitmapDescriptor Descriptor of the bitmap to be used. Must not be {@code null}.
     * @param refWidth         Stroke width, in pixels, for which the cap bitmap at its native dimension is designed. Must be positive.
     */
    public CustomCap(@NonNull BitmapDescriptor bitmapDescriptor, float refWidth) {
        super(3, bitmapDescriptor, refWidth);
        this.bitmapDescriptor = bitmapDescriptor;
        this.refWidth = refWidth;
    }

    /**
     * Constructs a new {@code CustomCap} with default reference stroke width of 10 pixels (equal to the default stroke width, see
     * {@link PolylineOptions#width(float)}).
     *
     * @param bitmapDescriptor Descriptor of the bitmap to be used. Must not be {@code null}.
     */
    public CustomCap(@NonNull BitmapDescriptor bitmapDescriptor) {
        super(3, bitmapDescriptor, null);
        this.bitmapDescriptor = bitmapDescriptor;
        this.refWidth = null;
    }

    @NonNull
    @Override
    public String toString() {
        return "[CustomCap bitmapDescriptor=" + bitmapDescriptor + " refWidth=" + refWidth + "]";
    }
}
