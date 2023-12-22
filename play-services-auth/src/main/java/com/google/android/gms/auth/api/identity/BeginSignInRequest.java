/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.identity;

import org.microg.safeparcel.AutoSafeParcelable;

public class BeginSignInRequest extends AutoSafeParcelable {
    @Field(1)
    public PasswordRequestOptions passwordRequestOptions;
    @Field(2)
    public GoogleIdTokenRequestOptions googleIdTokenRequestOptions;
    @Field(3)
    public String score;
    @Field(4)
    public boolean isPrimary;
    @Field(5)
    public int code;

    public static final Creator<BeginSignInRequest> CREATOR = findCreator(BeginSignInRequest.class);
}
