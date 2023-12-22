/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.identity;

import org.microg.safeparcel.AutoSafeParcelable;

public class GetSignInIntentRequest extends AutoSafeParcelable {
    @Field(1)
    public String clientId;
    @Field(2)
    public String scope;
    @Field(3)
    public String requestTag;
    @Field(4)
    public String requestToken;
    @Field(5)
    public boolean isPrimary;
    @Field(6)
    public int code;

    public static final Creator<GetSignInIntentRequest> CREATOR = findCreator(GetSignInIntentRequest.class);

}
