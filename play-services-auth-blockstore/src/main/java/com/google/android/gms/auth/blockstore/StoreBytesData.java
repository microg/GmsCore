/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.blockstore;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;

/**
 * Data passed by apps to Block Store.
 */
@SafeParcelable.Class
public class StoreBytesData extends AbstractSafeParcelable {

    @Field(value = 1, getterName = "getBytes")
    @NonNull
    private final byte[] bytes;

    @Field(value = 2, getterName = "shouldBackupToCloud")
    private final boolean shouldBackupToCloud;

    @Field(value = 3, getterName = "getKey")
    private final String key;

    @Constructor
    StoreBytesData(@NonNull @Param(1) byte[] bytes, @Param(2) boolean shouldBackupToCloud, @Param(3) String key) {
        this.bytes = bytes;
        this.shouldBackupToCloud = shouldBackupToCloud;
        this.key = key;
    }

    /**
     * Raw bytes passed from apps to Block Store.
     */
    @NonNull
    public byte[] getBytes() {
        return bytes;
    }

    /**
     * The key with which the bytes are associated.
     * <p>
     * If the key was never explicitly set when building the {@code StoreBytesData}, then the default key {@link BlockstoreClient#DEFAULT_BYTES_DATA_KEY}
     * is associated with the {@code bytes} and therefore will be returned.
     */
    @NonNull
    public String getKey() {
        return key;
    }

    /**
     * Whether the bytes to be stored should be backed up to the cloud in the next sync.
     */
    public boolean shouldBackupToCloud() {
        return shouldBackupToCloud;
    }

    public static final SafeParcelableCreatorAndWriter<StoreBytesData> CREATOR = findCreator(StoreBytesData.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    @NonNull
    @Override
    @Hide
    public String toString() {
        return ToStringHelper.name("StoreBytesData").field("bytes", bytes != null ? bytes.length : 0).field("shouldBackupToCloud", shouldBackupToCloud).field("key", key).end();
    }

    /**
     * A builder for {@link StoreBytesData} objects.
     */
    public static class Builder {
        private byte[] bytes;
        private boolean shouldBackupToCloud = false;
        private String key = BlockstoreClient.DEFAULT_BYTES_DATA_KEY;

        /**
         * Constructor for the {@link StoreBytesData.Builder} class.
         */
        public Builder() {
        }

        /**
         * Builds and returns the {@link StoreBytesData} object.
         */
        public StoreBytesData build() {
            return new StoreBytesData(bytes, shouldBackupToCloud, key);
        }

        /**
         * Sets the raw bytes to be stored with Block Store. See {@link BlockstoreClient#MAX_SIZE} for the maximum size allowed for a key-bytes entry.
         */
        public Builder setBytes(byte[] bytes) {
            this.bytes = bytes;
            return this;
        }

        /**
         * Sets the key with which the {@code bytes} are associated with. See {@link BlockstoreClient#MAX_SIZE} for the maximum size allowed for a key-bytes
         * entry.
         * <p>
         * If {@code setKey} is never invoked, the bytes will be associated with the default key {@link BlockstoreClient#DEFAULT_BYTES_DATA_KEY} when stored
         * into Block Store.
         */
        public Builder setKey(String key) {
            this.key = key;
            return this;
        }

        /**
         * Sets whether the bytes to be stored should be backed up to the cloud in the next sync.
         */
        public Builder setShouldBackupToCloud(boolean shouldBackupToCloud) {
            this.shouldBackupToCloud = shouldBackupToCloud;
            return this;
        }
    }
}