/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.mobstore;

import android.net.Uri;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class RenameRequest extends AbstractSafeParcelable {

    @Field(value = 1, getterName = "getLocalFileUri")
    private Uri localFileUri;
    @Field(value = 2, getterName = "getTargetFileUri")
    private Uri targetFileUri;

    public Uri getLocalFileUri() {
        return localFileUri;
    }

    public Uri getTargetFileUri() {
        return targetFileUri;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<RenameRequest> CREATOR = findCreator(RenameRequest.class);

}
