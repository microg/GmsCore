/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.identity;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.ArrayList;

@SafeParcelable.Class
public class GoogleIdTokenRequestOptions extends AbstractSafeParcelable {
    @Field(1)
    public boolean idTokenRequested;
    @Field(2)
    public String clientId;
    @Field(3)
    public String requestToken;
    @Field(4)
    public boolean serverAuthCodeRequested;
    @Field(5)
    public String serverClientId;
    @Field(6)
    public ArrayList<String> scopes;
    @Field(7)
    public boolean forceCodeForRefreshToken;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GoogleIdTokenRequestOptions> CREATOR = findCreator(GoogleIdTokenRequestOptions.class);
}
