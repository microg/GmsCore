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
import org.microg.gms.common.Hide;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

@Hide
@SafeParcelable.Class
public class Contents extends AbstractSafeParcelable {

    @Field(value = 2, getterName = "getParcelFileDescriptor")
    private final ParcelFileDescriptor fileDescriptor;
    @Field(value = 3, getterName = "getRequestId")
    private final int requestId;
    @Field(value = 4, getterName = "getMode")
    private final int mode;
    @Field(value = 5, getterName = "getDriveId")
    private final DriveId driveId;
    @Field(value = 7)
    final boolean unknown7;
    @Field(value = 8)
    @Nullable
    final String unknown8;

    @Constructor
    public Contents(@Param(value = 2) ParcelFileDescriptor fileDescriptor, @Param(value = 3) int requestId, @Param(value = 4) int mode, @Param(value = 5) DriveId driveId, @Param(value = 7) boolean unknown7, @Param(value = 8) @Nullable String unknown8) {
        this.fileDescriptor = fileDescriptor;
        this.requestId = requestId;
        this.mode = mode;
        this.driveId = driveId;
        this.unknown7 = unknown7;
        this.unknown8 = unknown8;
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
