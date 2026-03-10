/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.data;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

@SafeParcelable.Class
public class BitmapTeleporter extends AbstractSafeParcelable {

    @Field(1)
    public int versionCode;
    @Field(2)
    public ParcelFileDescriptor fileDescriptor;
    @Field(3)
    public int status;
    public boolean isParceled;
    public Bitmap targetBitmap;
    public File targetDirectory;

    public BitmapTeleporter() {
    }

    public BitmapTeleporter(int version, ParcelFileDescriptor parcelFileDescriptor, int status) {
        this.versionCode = version;
        this.fileDescriptor = parcelFileDescriptor;
        this.status = status;
        this.targetBitmap = null;
        this.isParceled = false;
    }

    public BitmapTeleporter(Bitmap bitmap) {
        this.versionCode = 1;
        this.fileDescriptor = null;
        this.status = 0;
        this.targetBitmap = bitmap;
        this.isParceled = true;
    }

    public final Bitmap createTargetBitmap() {
        if (!this.isParceled) {
            ParcelFileDescriptor parcelFileDescriptor = this.fileDescriptor;
            if (parcelFileDescriptor == null) {
                throw new NullPointerException("null reference");
            }
            DataInputStream dataInputStream = new DataInputStream(new ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor));
            try {
                try {
                    byte[] bArr = new byte[dataInputStream.readInt()];
                    int readInt = dataInputStream.readInt();
                    int readInt2 = dataInputStream.readInt();
                    Bitmap.Config valueOf = Bitmap.Config.valueOf(dataInputStream.readUTF());
                    dataInputStream.read(bArr);
                    close(dataInputStream);
                    ByteBuffer wrap = ByteBuffer.wrap(bArr);
                    Bitmap createBitmap = Bitmap.createBitmap(readInt, readInt2, valueOf);
                    createBitmap.copyPixelsFromBuffer(wrap);
                    this.targetBitmap = createBitmap;
                    this.isParceled = true;
                } catch (IOException e) {
                    throw new IllegalStateException("Could not read from parcel file descriptor", e);
                }
            } catch (Throwable th) {
                close(dataInputStream);
                throw th;
            }
        }
        return this.targetBitmap;
    }

    public final void setTargetDirectory(File file) {
        if (file == null) {
            throw new NullPointerException("Cannot set null temp directory");
        }
        this.targetDirectory = file;
    }

    private static void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            Log.w("BitmapTeleporter", "Could not close stream", e);
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<BitmapTeleporter> CREATOR = findCreator(BitmapTeleporter.class);

}
