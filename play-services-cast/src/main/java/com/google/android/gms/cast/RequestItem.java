/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.cast;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class RequestItem extends AutoSafeParcelable {
    @Field(2)
    public String url;
    @Field(3)
    public int protocolType;
    @Field(4)
    public int initialTime;
    @Field(5)
    public String hlsSegmentFormat;

    public static Creator<RequestItem> CREATOR = new AutoCreator<RequestItem>(RequestItem.class);
}
