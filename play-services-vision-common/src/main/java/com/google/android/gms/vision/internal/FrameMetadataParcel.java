/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.vision.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class FrameMetadataParcel extends AutoSafeParcelable {
    @Field(1)
    private final int versionCode = 1;
    @Field(2)
    public int width;
    @Field(3)
    public int height;
    @Field(4)
    public int id;
    @Field(5)
    public long timestampMillis;
    @Field(6)
    public int rotation;

    public static Creator<FrameMetadataParcel> CREATOR = new AutoCreator<>(FrameMetadataParcel.class);
}
