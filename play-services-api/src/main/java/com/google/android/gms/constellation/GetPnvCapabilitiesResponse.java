/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import org.microg.safeparcel.AutoSafeParcelable;

public class GetPnvCapabilitiesResponse extends AutoSafeParcelable {
    @Field(1)
    public boolean capable;
    @Field(2)
    public boolean hasDroidGuard;

    public static final Creator<GetPnvCapabilitiesResponse> CREATOR = new AutoCreator<>(GetPnvCapabilitiesResponse.class);
}
