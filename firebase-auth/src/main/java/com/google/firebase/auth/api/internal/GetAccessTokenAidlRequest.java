/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class GetAccessTokenAidlRequest extends AutoSafeParcelable {
    @Field(1)
    public String refreshToken;

    public static final Creator<GetAccessTokenAidlRequest> CREATOR = new AutoCreator<>(GetAccessTokenAidlRequest.class);
}
