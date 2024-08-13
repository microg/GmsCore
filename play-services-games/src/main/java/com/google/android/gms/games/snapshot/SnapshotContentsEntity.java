/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.snapshot;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.Preconditions;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.drive.Contents;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

@SafeParcelable.Class
public class SnapshotContentsEntity extends AbstractSafeParcelable implements SnapshotContents {
    private static final Object LOCK = new Object();

    @Field(value = 1, getterName = "getContents")
    private Contents contents;

    @Constructor
    public SnapshotContentsEntity(@Param(value = 1) Contents contents) {
        this.contents = contents;
    }

    public final ParcelFileDescriptor getParcelFileDescriptor() {
        Preconditions.checkState(!this.isClosed(), "Cannot mutate closed contents!");
        return this.contents.getParcelFileDescriptor();
    }

    public final Contents getContents() {
        return this.contents;
    }

    public final void close() {
        this.contents = null;
    }

    public final boolean isClosed() {
        return this.contents == null;
    }

    public final byte[] readFully() throws IOException {
        Preconditions.checkState(!this.isClosed(), "Must provide a previously opened Snapshot");
        synchronized (LOCK) {
            ParcelFileDescriptor parcelFileDescriptor = this.contents.getParcelFileDescriptor();
            FileInputStream fileInputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

            byte[] bytes;
            try {
                fileInputStream.getChannel().position(0L);

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                copyStream(bufferedInputStream, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                fileInputStream.getChannel().position(0L);
                bytes = byteArray;
            } catch (IOException ioException) {
                Log.d("SnapshotContentsEntity", "Failed to read snapshot data", ioException);
                throw ioException;
            }

            return bytes;
        }
    }

    private void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] bytes = new byte[1024];
        long l = 0L;

        int i;
        try {
            while((i = inputStream.read(bytes, 0, 1024)) != -1) {
                l += (long)i;
                outputStream.write(bytes, 0, i);
            }
        } catch (IOException ioException) {
            Log.d("SnapshotContentsEntity", "Failed to copyStream ", ioException);
            throw ioException;
        }
    }

    public final boolean writeBytes(byte[] bytes) {
        return this.doWrite(0, bytes, 0, bytes.length, true);
    }

    public final boolean modifyBytes(int var1, byte[] var2, int var3, int var4) {
        return this.doWrite(var1, var2, var3, var2.length, false);
    }

    private boolean doWrite(int var1, byte[] var2, int var3, int var4, boolean var5) {
        Preconditions.checkState(!this.isClosed(), "Must provide a previously opened SnapshotContents");
        synchronized (LOCK) {
            ParcelFileDescriptor parcelFileDescriptor = this.contents.getParcelFileDescriptor();
            FileOutputStream fileOutputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

            try {
                FileChannel fileChannel;
                (fileChannel = fileOutputStream.getChannel()).position((long) var1);
                bufferedOutputStream.write(var2, var3, var4);
                if (var5) {
                    fileChannel.truncate((long) var2.length);
                }

                bufferedOutputStream.flush();
            } catch (IOException var12) {
                Log.d("SnapshotContentsEntity", "Failed to write snapshot data", var12);
                return false;
            }

            return true;
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SnapshotContentsEntity> CREATOR = findCreator(SnapshotContentsEntity.class);
}
