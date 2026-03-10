/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class SignInWithCustomTokenAidlRequest extends AutoSafeParcelable {
    @Field(1)
    public String token;
    @Field(2)
    public String tenantId;

    public static final Creator<SignInWithCustomTokenAidlRequest> CREATOR = new AutoCreator<>(SignInWithCustomTokenAidlRequest.class);
}
