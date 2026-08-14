/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.internal.connect;

import androidx.annotation.NonNull;
import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

public class GamesSignInRequest extends AutoSafeParcelable {
    @Field(1)
    public int signInType;
    @Field(2)
    public SignInResolutionResult previousStepResolutionResult;

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("GamesSignInRequest")
                .field("signInType", signInType)
                .field("previousStepResolutionResult", previousStepResolutionResult)
                .end();
    }

    public static final Creator<GamesSignInRequest> CREATOR = findCreator(GamesSignInRequest.class);
}
