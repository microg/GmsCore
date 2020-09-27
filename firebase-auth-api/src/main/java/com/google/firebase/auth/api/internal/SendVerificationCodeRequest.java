/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import org.microg.safeparcel.AutoSafeParcelable;

public class SendVerificationCodeRequest extends AutoSafeParcelable {
    @Field(1)
    public String phoneNumber;
    @Field(2)
    public Long timeoutInSeconds;
    @Field(3)
    public boolean forceNewSmsVerificationSession;
    @Field(4)
    public String languageHeader;
    @Field(5)
    public String tenantId;
    @Field(6)
    public String recaptchaToken;

    public static final Creator<SendVerificationCodeRequest> CREATOR = new AutoCreator<>(SendVerificationCodeRequest.class);
}
