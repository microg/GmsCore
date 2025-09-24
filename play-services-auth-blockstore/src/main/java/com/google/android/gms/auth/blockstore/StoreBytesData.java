/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.blockstore;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class StoreBytesData extends AbstractSafeParcelable {

    @Field(value = 1)
    public final byte[] bytes;

    @Field(value = 2)
    public final boolean shouldBackupToCloud;

    @Field(value = 3)
    public final String key;

    @Constructor
    public StoreBytesData(@Param(1) byte[] bytes, @Param(2) boolean shouldBackupToCloud, @Param(3) String key) {
        this.bytes = bytes;
        this.shouldBackupToCloud = shouldBackupToCloud;
        this.key = key;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public boolean shouldBackupToCloud() {
        return shouldBackupToCloud;
    }

    public String getKey() {
        return key;
    }

    public static final SafeParcelableCreatorAndWriter<StoreBytesData> CREATOR = findCreator(StoreBytesData.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    @Override
    public String toString() {
        return ToStringHelper.name("StoreBytesData").field("bytes", bytes != null ? bytes.length : 0).field("shouldBackupToCloud", shouldBackupToCloud).field("key", key).end();
    }

    public static class Builder {
        private String key = BlockstoreClient.DEFAULT_BYTES_DATA_KEY;
        private byte[] bytes;
        private boolean shouldBackupToCloud = false;

        public Builder setBytes(byte[] bytes) {
            this.bytes = bytes;
            return this;
        }

        public Builder setKey(String key) {
            this.key = key;
            return this;
        }

        public Builder setShouldBackupToCloud(boolean shouldBackupToCloud) {
            this.shouldBackupToCloud = shouldBackupToCloud;
            return this;
        }

        public StoreBytesData build() {
            return new StoreBytesData(bytes, shouldBackupToCloud, key);
        }
    }
}