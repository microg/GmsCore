/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.internal.connect;

import android.content.Intent;
import androidx.annotation.NonNull;
import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

public class SignInResolutionResult extends AutoSafeParcelable {
    @Field(1)
    public Intent resultData;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("SignInResolutionResult").field("resultData", resultData).end();
    }

    public static final Creator<SignInResolutionResult> CREATOR = findCreator(SignInResolutionResult.class);
}
