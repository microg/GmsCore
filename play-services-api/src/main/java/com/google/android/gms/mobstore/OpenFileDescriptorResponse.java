/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.mobstore;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class OpenFileDescriptorResponse extends AbstractSafeParcelable {

    @Field(value = 1, getterName = "getFileDescriptor")
    private ParcelFileDescriptor fileDescriptor;

    public ParcelFileDescriptor getFileDescriptor() {
        return fileDescriptor;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<OpenFileDescriptorResponse> CREATOR = findCreator(OpenFileDescriptorResponse.class);

}
