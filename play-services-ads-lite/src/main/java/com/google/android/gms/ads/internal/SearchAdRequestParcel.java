/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class SearchAdRequestParcel extends AutoSafeParcelable {
    @Field(15)
    public String query;
    public static final Creator<SearchAdRequestParcel> CREATOR = new AutoCreator<>(SearchAdRequestParcel.class);
}
