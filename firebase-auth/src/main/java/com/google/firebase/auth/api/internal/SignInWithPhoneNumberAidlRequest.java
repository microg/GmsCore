/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import com.google.firebase.auth.PhoneAuthCredential;

import org.microg.safeparcel.AutoSafeParcelable;

public class SignInWithPhoneNumberAidlRequest extends AutoSafeParcelable {
    @Field(1)
    public PhoneAuthCredential credential;
    @Field(2)
    public String tenantId;

    public static final Creator<SignInWithPhoneNumberAidlRequest> CREATOR = new AutoCreator<>(SignInWithPhoneNumberAidlRequest.class);
}
