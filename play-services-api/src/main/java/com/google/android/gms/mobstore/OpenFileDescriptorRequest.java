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
public class OpenFileDescriptorRequest extends AbstractSafeParcelable {

    @Field(value = 1, getterName = "getFileUri")
    private Uri fileUri;
    @Field(value = 2, getterName = "getFlag")
    private int flag;

    public Uri getFileUri() {
        return fileUri;
    }

    public int getFlag() {
        return flag;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<OpenFileDescriptorRequest> CREATOR = findCreator(OpenFileDescriptorRequest.class);
}
