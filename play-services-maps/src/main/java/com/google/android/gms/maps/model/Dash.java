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
 * An immutable class representing a dash used in the stroke pattern for a Polyline or the outline of a Polygon or Circle.
 */
@PublicApi
public final class Dash extends PatternItem {
    /**
     * Length in pixels (non-negative).
     */
    public final float length;

    /**
     * Constructs a {@code Dash}.
     * @param length Length in pixels. Negative value will be clamped to zero.
     */
    public Dash(float length) {
        super(0, Math.max(length, 0));
        this.length = Math.max(length, 0);
    }

    @NonNull
    @Override
    public String toString() {
        return "[Dash: length=" + length + "]";
    }
}
