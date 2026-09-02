/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.aang;

import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.List;

@Hide
public class Oauth2TokenMetadata extends AutoSafeParcelable {
    @Field(1)
    public Long expiry;
    @Field(2)
    public List<String> scopes;

    public static final Creator<Oauth2TokenMetadata> CREATOR = new AutoCreator<>(Oauth2TokenMetadata.class);
}
