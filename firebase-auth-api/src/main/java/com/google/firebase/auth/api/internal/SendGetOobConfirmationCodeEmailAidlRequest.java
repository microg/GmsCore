/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.firebase.auth.api.internal;

import com.google.firebase.auth.ActionCodeSettings;

import org.microg.safeparcel.AutoSafeParcelable;

public class SendGetOobConfirmationCodeEmailAidlRequest extends AutoSafeParcelable {
    @Field(1)
    public String email;
    @Field(2)
    public ActionCodeSettings settings;
    @Field(3)
    public String tenantId;
    public static final Creator<SendGetOobConfirmationCodeEmailAidlRequest> CREATOR = new AutoCreator<>(SendGetOobConfirmationCodeEmailAidlRequest.class);
}
