/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class ResetPasswordResponse extends AutoSafeParcelable {
    @Field(2)
    public String email;
    @Field(3)
    public String newEmail;
    @Field(4)
    public String requestType;
    @Field(5)
    public MfaInfo mfaInfo;
    public static final Creator<ResetPasswordResponse> CREATOR = new AutoCreator<>(ResetPasswordResponse.class);
}
