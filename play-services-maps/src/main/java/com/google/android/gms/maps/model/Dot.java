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
 * An immutable class representing a dot used in the stroke pattern for a Polyline or the outline of a Polygon or Circle.
 */
@PublicApi
public final class Dot extends PatternItem {
    /**
     * Constructs a {@code Dot}.
     */
    public Dot() {
        super(1, null);
    }

    @NonNull
    @Override
    public String toString() {
        return "[Dot]";
    }
}
