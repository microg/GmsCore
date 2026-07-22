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

import java.util.List;

@SafeParcelable.Class
public class CableAuthenticationExtension extends AbstractSafeParcelable {
    @Field(1)
    @NonNull
    List<CableAuthenticationData> cableAuthentication;

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CableAuthenticationExtension> CREATOR = findCreator(CableAuthenticationExtension.class);
}
