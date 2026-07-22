/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.credential.manager.invocationparams;

import androidx.annotation.NonNull;
import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

public class CredentialManagerInvocationParams extends AutoSafeParcelable {
    @Field(1)
    public CredentialManagerAccount account;
    @Field(2)
    public CallerInfo caller;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("CredentialManagerInvocationParams")
                .field("account", account)
                .field("caller", caller)
                .end();
    }

    public static final Creator<CredentialManagerInvocationParams> CREATOR = new AutoCreator<>(CredentialManagerInvocationParams.class);
}
