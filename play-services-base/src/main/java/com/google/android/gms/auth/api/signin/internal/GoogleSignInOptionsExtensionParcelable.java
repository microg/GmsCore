/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.api.signin.internal;

import android.os.Bundle;
import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

@Hide
@SafeParcelable.Class
public class GoogleSignInOptionsExtensionParcelable extends AbstractSafeParcelable {
    @Field(1)
    public final int versionCode;
    @Field(2)
    public final int type;
    @Field(3)
    public final Bundle bundle;

    public GoogleSignInOptionsExtensionParcelable(GoogleSignInOptionsExtension extension) {
        this(extension.getExtensionType(), extension.toBundle());
    }

    public GoogleSignInOptionsExtensionParcelable(int type, Bundle bundle) {
        this(1, type, bundle);
    }

    @Constructor
    public GoogleSignInOptionsExtensionParcelable(@Param(1) int versionCode, @Param(2) int type, @Param(3) Bundle bundle) {
        this.versionCode = versionCode;
        this.type = type;
        this.bundle = bundle;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GoogleSignInOptionsExtensionParcelable> CREATOR = findCreator(GoogleSignInOptionsExtensionParcelable.class);
}
