/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.credential.manager.invocationparams;

import androidx.annotation.NonNull;
import org.microg.safeparcel.AutoSafeParcelable;

public class CredentialManagerAccount extends AutoSafeParcelable {
    @Field(1)
    public String name;

    @NonNull
    @Override
    public String toString() {
        return name;
    }

    public static final String NAME_LOCAL = "pwm.constant.LocalAccount";
    public static final Creator<CredentialManagerAccount> CREATOR = new AutoCreator<>(CredentialManagerAccount.class);
}
