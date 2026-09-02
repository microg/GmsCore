/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.aang;

import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class GetTokenResponse extends AutoSafeParcelable {
    @Field(1)
    public String token;
    @Field(2)
    public Oauth2TokenMetadata oauth2TokenMetadata;

    public static final Creator<GetTokenResponse> CREATOR = new AutoCreator<>(GetTokenResponse.class);
}
