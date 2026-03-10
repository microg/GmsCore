/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.recaptcha.internal;

import com.google.android.gms.recaptcha.RecaptchaResultData;

import org.microg.safeparcel.AutoSafeParcelable;

public class ExecuteResults extends AutoSafeParcelable {
    @Field(1)
    public RecaptchaResultData data;

    public static final Creator<ExecuteResults> CREATOR = new AutoCreator<>(ExecuteResults.class);
}
