/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.maps.model;

import org.microg.safeparcel.AutoSafeParcelable;

public class StyleSpan extends AutoSafeParcelable {
    @Field(2)
    private StrokeStyle style;
    @Field(3)
    private double segments;

    public double getSegments() {
        return segments;
    }

    public StrokeStyle getStyle() {
        return style;
    }

    public static final Creator<StyleSpan> CREATOR = new AutoCreator<>(StyleSpan.class);
}
