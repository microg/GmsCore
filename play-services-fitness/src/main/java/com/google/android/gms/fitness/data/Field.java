/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fitness.data;

import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.Hide;

@SafeParcelable.Class
@SafeParcelable.Reserved({1000})
public class Field extends AbstractSafeParcelable {

    @Field(1)
    public final String name;

    @Field(2)
    public final int format;

    public static final int FORMAT_FLOAT = 2;
    public static final int FORMAT_INT32 = 1;
    public static final int FORMAT_STRING = 3;

    @SafeParcelable.Constructor
    public Field(@SafeParcelable.Param(1) String name, @SafeParcelable.Param(2) int format) {
        this.name = name;
        this.format = format;
    }

    public String getName() {
        return name;
    }

    public int getFormat() {
        return format;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        SafeParcelableCreatorAndWriter writer = new SafeParcelableCreatorAndWriter(dest);
        writer.write(1, name);
        writer.write(2, format);
    }

    public static final Creator<Field> CREATOR = new SafeParcelableCreatorAndWriter.ReflectiveCreator<>(Field.class);
}
