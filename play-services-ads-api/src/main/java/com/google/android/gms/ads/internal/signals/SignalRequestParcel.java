/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.ads.internal.signals;

import com.google.android.gms.ads.internal.AdRequestParcel;
import com.google.android.gms.ads.internal.client.AdSizeParcel;
import org.microg.safeparcel.AutoSafeParcelable;

/** Request fields used by the Mobile Ads signal generator wire protocol. */
public class SignalRequestParcel extends AutoSafeParcelable {
    @Field(1)
    public String adUnitId;
    @Field(2)
    public String adFormat;
    @Field(3)
    public AdSizeParcel adSize;
    @Field(4)
    public AdRequestParcel adRequest;

    public static final Creator<SignalRequestParcel> CREATOR = new AutoCreator<>(SignalRequestParcel.class);
}
