/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.maps.model;

import org.microg.safeparcel.AutoSafeParcelable;

public class StrokeStyle extends AutoSafeParcelable {
    @Field(2)
    private float width;
    @Field(3)
    private int color;
    @Field(4)
    private int toColor;
    @Field(5)
    private boolean isVisible;
    @Field(6)
    private StampStyle stamp;

    public static final Creator<StrokeStyle> CREATOR = new AutoCreator<>(StrokeStyle.class);
}
