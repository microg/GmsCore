/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.drive;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class DriveId extends AbstractSafeParcelable {
    public static final int RESOURCE_TYPE_UNKNOWN = -1;
    public static final int RESOURCE_TYPE_FILE = 0;
    public static final int RESOURCE_TYPE_FOLDER = 1;

    @Field(value = 2)
    private final String resourceId;
    @Field(value = 3)
    private final long unknownLongFile3;
    @Field(value = 4)
    private final long unknownLongFile4;
    @Field(value = 5)
    private final int resourceType;


    @Nullable
    public String getResourceId() {
        return this.resourceId;
    }

    public int getResourceType() {
        return this.resourceType;
    }

    @Constructor
    public DriveId(@Param(value = 2) String var1, @Param(value = 3) long var2, @Param(value = 4) long var4, @Param(value = 5) int var6) {
        this.resourceId = var1;
        this.unknownLongFile3 = var2;
        this.unknownLongFile4 = var4;
        this.resourceType = var6;
    }


    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<DriveId> CREATOR = findCreator(DriveId.class);
}

