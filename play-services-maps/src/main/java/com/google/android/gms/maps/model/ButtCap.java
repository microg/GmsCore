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
 * Cap that is squared off exactly at the start or end vertex of a {@link Polyline} with solid stroke pattern, equivalent to having no
 * additional cap beyond the start or end vertex. This is the default cap type at start and end vertices of {@link Polyline}s with
 * solid stroke pattern.
 */
public class ButtCap extends Cap {
    /**
     * Constructs a {@code ButtCap}.
     */
    public ButtCap() {
        super(0, null, null);
    }

    @NonNull
    @Override
    public String toString() {
        return "[ButtCap]";
    }
}
