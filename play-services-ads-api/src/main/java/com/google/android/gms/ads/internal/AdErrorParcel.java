/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal;

import com.google.android.gms.ads.internal.client.IResponseInfo;
import org.microg.safeparcel.AutoSafeParcelable;

public class AdErrorParcel extends AutoSafeParcelable {
    @Field(1)
    public int code;
    @Field(2)
    public String message;
    @Field(3)
    public String domain;
    @Field(4)
    public AdErrorParcel cause;
    @Field(5)
    public IResponseInfo responseInfo;
    public static final Creator<AdErrorParcel> CREATOR = new AutoCreator<>(AdErrorParcel.class);
}
