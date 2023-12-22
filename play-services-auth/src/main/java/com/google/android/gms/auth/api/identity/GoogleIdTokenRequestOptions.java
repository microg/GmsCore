/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.identity;

import org.microg.safeparcel.AutoSafeParcelable;

import java.util.ArrayList;

public class GoogleIdTokenRequestOptions extends AutoSafeParcelable {
    @Field(1)
    public boolean idTokenRequested;
    @Field(2)
    public String clientId;
    @Field(3)
    public String requestToken;
    @Field(4)
    public boolean serverAuthCodeRequested;
    @Field(5)
    public String serverClientId;
    @Field(6)
    public ArrayList<?> scopes;
    @Field(7)
    public boolean forceCodeForRefreshToken;

    public static final Creator<GoogleIdTokenRequestOptions> CREATOR = findCreator(GoogleIdTokenRequestOptions.class);
}
