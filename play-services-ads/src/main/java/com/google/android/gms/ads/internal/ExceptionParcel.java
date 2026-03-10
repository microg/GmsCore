/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class ExceptionParcel extends AutoSafeParcelable {
    @Field(1)
    public String message;
    @Field(2)
    public int code;
    public static final Creator<ExceptionParcel> CREATOR = new AutoCreator<>(ExceptionParcel.class);
}
