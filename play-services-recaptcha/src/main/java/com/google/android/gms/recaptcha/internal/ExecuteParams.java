/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.recaptcha.internal;

import androidx.annotation.NonNull;

import com.google.android.gms.recaptcha.RecaptchaAction;
import com.google.android.gms.recaptcha.RecaptchaHandle;

import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

public class ExecuteParams extends AutoSafeParcelable {
    @Field(1)
    public RecaptchaHandle handle;
    @Field(2)
    public RecaptchaAction action;
    @Field(3)
    public String version;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("ExecuteParams")
                .field("handle", handle)
                .field("action", action)
                .field("version", version)
                .end();
    }

    public static final Creator<ExecuteParams> CREATOR = new AutoCreator<>(ExecuteParams.class);
}
