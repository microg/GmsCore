/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.blockstore;

import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * App data retrieved from BlockStore.
 */
@SafeParcelable.Class
public class RetrieveBytesResponse extends AbstractSafeParcelable {

    @Deprecated
    @Field(value = 1, getterName = "getInternalBlockstoreDataBundle")
    private final Bundle internalBlockstoreDataBundle;

    @Field(value = 2, getterName = "getInternalBlockstoreDataList")
    private final List<BlockstoreData> internalBlockstoreDataList;

    private final Map<String, BlockstoreData> blockstoreDataMap;

    @Constructor
    @Hide
    public RetrieveBytesResponse(@Param(1) Bundle internalBlockstoreDataBundle, @Param(2) List<BlockstoreData> internalBlockstoreDataList) {
        this.internalBlockstoreDataBundle = internalBlockstoreDataBundle;
        this.internalBlockstoreDataList = internalBlockstoreDataList;
        HashMap<String, BlockstoreData> blockstoreDataMap = new HashMap<>();
        for (BlockstoreData blockstoreData : internalBlockstoreDataList) {
            blockstoreDataMap.put(blockstoreData.key, blockstoreData);
        }
        this.blockstoreDataMap = blockstoreDataMap;
    }

    /**
     * A mapping from app data key to {@link RetrieveBytesResponse.BlockstoreData} found based on a {@link RetrieveBytesRequest}.
     * <p>
     * The app data key is the value provided when storing the data via {@link BlockstoreClient#storeBytes(StoreBytesData)}, as
     * {@code StoreBytesData.key}.
     * <p>
     * Note that the app data that was stored without an explicit key is associated with the default key
     * {@link BlockstoreClient#DEFAULT_BYTES_DATA_KEY}.
     */
    public Map<String, RetrieveBytesResponse.BlockstoreData> getBlockstoreDataMap() {
        return blockstoreDataMap;
    }

    @Hide
    public Bundle getInternalBlockstoreDataBundle() {
        return internalBlockstoreDataBundle;
    }

    @Hide
    public List<BlockstoreData> getInternalBlockstoreDataList() {
        return internalBlockstoreDataList;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    @NonNull
    @Override
    @Hide
    public String toString() {
        return ToStringHelper.name("RetrieveBytesResponse").value(blockstoreDataMap).end();
    }

    public static final SafeParcelableCreatorAndWriter<RetrieveBytesResponse> CREATOR = findCreator(RetrieveBytesResponse.class);

    /**
     * A block of app data previously stored to Block Store.
     */
    @SafeParcelable.Class
    public static class BlockstoreData extends AbstractSafeParcelable {
        @Field(value = 1, getterName = "getBytes")
        @NonNull
        private final byte[] bytes;

        @Field(value = 2, getterName = "getKey")
        @NonNull
        private final String key;

        @Constructor
        @Hide
        public BlockstoreData(@NonNull @Param(1) byte[] bytes, @NonNull @Param(2) String key) {
            this.bytes = bytes;
            this.key = key;
        }

        /**
         * Raw bytes passed from the app to Block Store.
         */
        @NonNull
        public byte[] getBytes() {
            return bytes;
        }

        @Hide
        @NonNull
        public String getKey() {
            return key;
        }

        @NonNull
        @Override
        @Hide
        public String toString() {
            return ToStringHelper.name("BlockstoreData").value(key).field("bytes", bytes, true).end();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        public static final SafeParcelableCreatorAndWriter<BlockstoreData> CREATOR = findCreator(BlockstoreData.class);
    }
}