/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import org.microg.safeparcel.AutoSafeParcelable;

public class GetPnvCapabilitiesRequest extends AutoSafeParcelable {
    @Field(1)
    public int[] simSlots;
    @Field(2)
    public String[] gcmCapablePackageNames;

    public static final Creator<GetPnvCapabilitiesRequest> CREATOR = new AutoCreator<>(GetPnvCapabilitiesRequest.class);
}
