/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.recaptcha.internal;

import androidx.annotation.NonNull;

import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

public class InitParams extends AutoSafeParcelable {
    @Field(1)
    public String siteKey;
    @Field(2)
    public String version;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("InitParams")
                .field("siteKey", siteKey)
                .field("version", version)
                .end();
    }

    public static final Creator<InitParams> CREATOR = new AutoCreator<>(InitParams.class);
}
