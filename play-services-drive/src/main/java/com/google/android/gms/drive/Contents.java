/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.drive;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

@SafeParcelable.Class
public class Contents extends AbstractSafeParcelable {

    @Field(value = 2)
    private final ParcelFileDescriptor fileDescriptor;
    @Field(value = 3)
    final int requestId;
    @Field(value = 4)
    private final int mode;
    @Field(value = 5)
    private final DriveId driveId;
    @Field(value = 7)
    private final boolean unknownBooleanFile7;
    @Field(value = 8)
    @Nullable
    private final String unknownStringFile8;

    @Constructor
    public Contents(@Param(value = 2) ParcelFileDescriptor var1, @Param(value = 3) int var2, @Param(value = 4) int var3, @Param(value = 5) DriveId var4, @Param(value = 7) boolean var5, @Param(value = 8) @Nullable String var6) {
        this.fileDescriptor = var1;
        this.requestId = var2;
        this.mode = var3;
        this.driveId = var4;
        this.unknownBooleanFile7 = var5;
        this.unknownStringFile8 = var6;
    }

    public ParcelFileDescriptor getParcelFileDescriptor() {
        return this.fileDescriptor;
    }

    public final DriveId getDriveId() {
        return this.driveId;
    }

    public final FileInputStream getInputStream() {
        return new FileInputStream(this.fileDescriptor.getFileDescriptor());
    }

    public final OutputStream getOutputStream() {
        return new FileOutputStream(this.fileDescriptor.getFileDescriptor());
    }

    public final int getMode() {
        return this.mode;
    }

    public final int getRequestId() {
        return this.requestId;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Contents> CREATOR = findCreator(Contents.class);
}
