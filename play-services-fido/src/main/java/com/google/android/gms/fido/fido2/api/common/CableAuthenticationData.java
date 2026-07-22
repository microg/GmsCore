/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class CableAuthenticationData extends AbstractSafeParcelable {
    @Field(1)
    long version;
    @Field(2)
    @NonNull
    byte[] clientEid;
    @Field(3)
    @NonNull
    byte[] authenticatorEid;
    @Field(4)
    @NonNull
    byte[] sessionPreKey;

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CableAuthenticationData> CREATOR = findCreator(CableAuthenticationData.class);
}
