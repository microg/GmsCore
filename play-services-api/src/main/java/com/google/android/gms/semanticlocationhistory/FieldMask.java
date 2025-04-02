/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.semanticlocationhistory;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

import java.util.List;

@SafeParcelable.Class
public class FieldMask extends AbstractSafeParcelable {

    @Field(1)
    public List<String> list;

    public FieldMask() {
    }

    @Constructor
    public FieldMask(@Param(1) List<String> list) {
        this.list = list;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<FieldMask> CREATOR = findCreator(FieldMask.class);

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("FieldMask").field("list", list).end();
    }
}
