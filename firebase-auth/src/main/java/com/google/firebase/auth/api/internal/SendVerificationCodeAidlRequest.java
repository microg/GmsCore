/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class SendVerificationCodeAidlRequest extends AutoSafeParcelable {
    @Field(1)
    public SendVerificationCodeRequest request;
    public static final Creator<SendVerificationCodeAidlRequest> CREATOR = new AutoCreator<>(SendVerificationCodeAidlRequest.class);
}
