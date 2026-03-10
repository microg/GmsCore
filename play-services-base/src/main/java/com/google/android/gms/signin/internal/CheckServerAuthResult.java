/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.signin.internal;

import com.google.android.gms.common.api.Scope;
import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.List;

@Hide
public class CheckServerAuthResult extends AutoSafeParcelable {
    @Field(1)
    private int versionCode = 1;
    @Field(2)
    public boolean newAuthCodeRequired;
    @Field(3)
    public List<Scope> additionalScopes;

    public static final Creator<CheckServerAuthResult> CREATOR = findCreator(CheckServerAuthResult.class);
}
