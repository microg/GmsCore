/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.maps.model;

import androidx.annotation.NonNull;

import org.microg.gms.common.PublicApi;

/**
 * An immutable class representing a gap used in the stroke pattern for a Polyline or the outline of a Polygon or Circle.
 */
@PublicApi
public final class Gap extends PatternItem {
    /**
     * Length in pixels (non-negative).
     */
    public final float length;

    /**
     * Constructs a {@code Gap}.
     * @param length Length in pixels. Negative value will be clamped to zero.
     */
    public Gap(float length) {
        super(2, Math.max(length, 0));
        this.length = Math.max(length, 0);
    }

    @NonNull
    @Override
    public String toString() {
        return "[Gap: length=" + length + "]";
    }
}
