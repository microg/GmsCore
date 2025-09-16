/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.identity;

import android.app.PendingIntent;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.util.List;

@SafeParcelable.Class
public class VerifyWithGoogleResult extends AbstractSafeParcelable {

    @Field(1)
    public String serverAuthToken;
    @Field(2)
    public String idToken;
    @Field(3)
    public List<Scope> grantedScopes;
    @Field(4)
    public PendingIntent pendingIntent;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<VerifyWithGoogleResult> CREATOR = findCreator(VerifyWithGoogleResult.class);

    @Override
    public String toString() {
        return "VerifyWithGoogleResult{" +
                "serverAuthToken='" + serverAuthToken + '\'' +
                ", idToken='" + idToken + '\'' +
                ", grantedScopes=" + grantedScopes +
                ", pendingIntent=" + pendingIntent +
                '}';
    }
}
