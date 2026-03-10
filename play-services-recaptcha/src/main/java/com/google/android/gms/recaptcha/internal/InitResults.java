/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.recaptcha.internal;

import com.google.android.gms.recaptcha.RecaptchaHandle;

import org.microg.safeparcel.AutoSafeParcelable;

public class InitResults extends AutoSafeParcelable {
    @Field(1)
    public RecaptchaHandle handle;

    public static final Creator<InitResults> CREATOR = new AutoCreator<>(InitResults.class);
}
