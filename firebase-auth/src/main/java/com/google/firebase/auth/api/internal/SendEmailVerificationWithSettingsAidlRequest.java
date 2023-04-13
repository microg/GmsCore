/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import com.google.firebase.auth.ActionCodeSettings;

import org.microg.safeparcel.AutoSafeParcelable;

public class SendEmailVerificationWithSettingsAidlRequest extends AutoSafeParcelable {
    @Field(1)
    public String token;
    @Field(2)
    public ActionCodeSettings settings;
    public static final Creator<SendEmailVerificationWithSettingsAidlRequest> CREATOR = new AutoCreator<>(SendEmailVerificationWithSettingsAidlRequest.class);
}
