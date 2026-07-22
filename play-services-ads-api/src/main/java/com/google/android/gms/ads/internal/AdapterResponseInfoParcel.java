/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal;

import android.os.Bundle;
import org.microg.safeparcel.AutoSafeParcelable;

public class AdapterResponseInfoParcel extends AutoSafeParcelable {
    @Field(1)
    public String adapterClassName;
    @Field(2)
    public long latencyMillis;
    @Field(3)
    public AdErrorParcel error;
    @Field(4)
    public Bundle credentials;
    @Field(5)
    public String adSourceName;
    @Field(6)
    public String adSourceId;
    @Field(7)
    public String adSourceInstanceName;
    @Field(8)
    public String adSourceInstanceId;

    public static final Creator<AdapterResponseInfoParcel> CREATOR = new AutoCreator<>(AdapterResponseInfoParcel.class);
}
