/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.data;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

import static com.google.android.gms.fitness.data.Field.FORMAT_FLOAT;

@Hide
@SafeParcelable.Class
public class MapValue extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getFormat")
    private final int format;
    @Field(value = 2, getterName = "getValue")
    private final float value;

    @Constructor
    public MapValue(@Param(1) int format, @Param(2) float value) {
        this.format = format;
        this.value = value;
    }

    @NonNull
    public static MapValue ofFloat(float value) {
        return new MapValue(FORMAT_FLOAT, value);
    }

    public int getFormat() {
        return format;
    }

    float getValue() {
        return value;
    }

    public float asFloat() {
        if (format != FORMAT_FLOAT) throw new IllegalStateException("MapValue is not a float");
        return value;
    }

    @Override
    public int hashCode() {
        return (int) value;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<MapValue> CREATOR = findCreator(MapValue.class);
}
