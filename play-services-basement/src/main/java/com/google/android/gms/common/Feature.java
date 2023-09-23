/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class Feature extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getName")
    private String name;
    @Field(2)
    int oldVersion;
    @Field(value = 3, getterName = "getVersion", defaultValue = "-1")
    private long version = -1;

    private Feature() {
    }

    @Constructor
    public Feature(@Param(1) String name, @Param(3) long version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public long getVersion() {
        if (version == -1) return oldVersion;
        return version;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Feature> CREATOR = findCreator(Feature.class);
}
