/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class AdapterStatusParcel extends AutoSafeParcelable {
    @Field(1)
    public String className;
    @Field(2)
    public boolean isReady;
    @Field(3)
    public int latency;
    @Field(4)
    public String description;

    public AdapterStatusParcel() {}

    public AdapterStatusParcel(String className, boolean isReady, int latency, String description) {
        this.className = className;
        this.isReady = isReady;
        this.latency = latency;
        this.description = description;
    }

    public static final Creator<AdapterStatusParcel> CREATOR = new AutoCreator<>(AdapterStatusParcel.class);
}
