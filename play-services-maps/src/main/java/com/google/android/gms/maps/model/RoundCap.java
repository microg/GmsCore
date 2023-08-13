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
 * Cap that is a semicircle with radius equal to half the stroke width, centered at the start or end vertex of a {@link Polyline} with solid stroke pattern.
 */
public class RoundCap extends Cap {
    /**
     * Constructs a {@code RoundCap}.
     */
    public RoundCap() {
        super(2, null, null);
    }

    @NonNull
    @Override
    public String toString() {
        return "[RoundCap]";
    }
}
