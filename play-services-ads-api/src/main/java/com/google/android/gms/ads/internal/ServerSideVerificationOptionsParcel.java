/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.ads.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class ServerSideVerificationOptionsParcel extends AutoSafeParcelable {
    @Field(1)
    public String userId;
    @Field(2)
    public String customData;
    public static final Creator<ServerSideVerificationOptionsParcel> CREATOR = new AutoCreator<>(ServerSideVerificationOptionsParcel.class);
}
