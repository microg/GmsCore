/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.signin.internal;

import android.content.Intent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;
import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class AuthAccountResult extends AutoSafeParcelable implements Result {
    @Field(1)
    private int versionCode = 2;
    @Field(2)
    public int connectionResultCode;
    @Field(3)
    public Intent rawAuthResolutionIntent;

    @Override
    public Status getStatus() {
        return connectionResultCode == ConnectionResult.SUCCESS ? Status.SUCCESS : Status.CANCELED;
    }

    public static final Creator<AuthAccountResult> CREATOR = findCreator(AuthAccountResult.class);
}
