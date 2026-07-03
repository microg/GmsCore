/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import org.microg.safeparcel.AutoSafeParcelable;

public class GetIidTokenResponse extends AutoSafeParcelable {
    @Field(1)
    public String iidToken;

    public static final Creator<GetIidTokenResponse> CREATOR = new AutoCreator<>(GetIidTokenResponse.class);
}
