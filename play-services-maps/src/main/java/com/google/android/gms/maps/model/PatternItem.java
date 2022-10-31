/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.maps.model;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

/**
 * Immutable item used in the stroke pattern for a Polyline or the outline of a Polygon or Circle.
 */
@PublicApi
public class PatternItem extends AutoSafeParcelable {
    @Field(2)
    private int type;
    @Field(3)
    private Float length;

    @PublicApi(exclude = true)
    PatternItem(int type, Float length) {
        this.type = type;
        this.length = length;
    }

    @Override
    public String toString() {
        return "[PatternItem: type=" + type + " length=" + length + "]";
    }

    public static final Creator<PatternItem> CREATOR = new AutoCreator<>(PatternItem.class);
}
