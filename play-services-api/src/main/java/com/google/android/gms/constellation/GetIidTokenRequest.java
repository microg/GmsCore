/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import org.microg.safeparcel.AutoSafeParcelable;

public class GetIidTokenRequest extends AutoSafeParcelable {
    @Field(1)
    public String[] gcmCapablePackageNames;
    @Field(2)
    public boolean preferUntrusted;

    public static final Creator<GetIidTokenRequest> CREATOR = new AutoCreator<>(GetIidTokenRequest.class);
}
