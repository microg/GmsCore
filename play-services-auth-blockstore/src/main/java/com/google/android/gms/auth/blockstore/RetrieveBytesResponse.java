/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.blockstore;

import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import org.microg.gms.utils.ToStringHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SafeParcelable.Class
public class RetrieveBytesResponse extends AbstractSafeParcelable {

    @Deprecated
    @Field(1)
    public Bundle bundle;

    @Field(2)
    public List<BlockstoreData> dataList;

    private final Map<String, BlockstoreData> blockstoreDataMap;

    @Constructor
    public RetrieveBytesResponse(@Param(1) Bundle bundle, @Param(2) List<BlockstoreData> list) {
        this.bundle = bundle;
        this.dataList = list;
        HashMap<String, BlockstoreData> hashMap = new HashMap<>();
        for (BlockstoreData blockstoreData : list) {
            hashMap.put(blockstoreData.key, blockstoreData);
        }
        this.blockstoreDataMap = hashMap;
    }

    public Map<String, RetrieveBytesResponse.BlockstoreData> getBlockstoreDataMap() {
        return blockstoreDataMap;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    @Override
    public String toString() {
        return ToStringHelper.name("RetrieveBytesResponse").field("legacyBundle", bundle).field("dataList", dataList).end();
    }

    public static final SafeParcelableCreatorAndWriter<RetrieveBytesResponse> CREATOR = findCreator(RetrieveBytesResponse.class);

    @SafeParcelable.Class
    public static class BlockstoreData extends AbstractSafeParcelable {
        @Field(value = 1)
        public byte[] bytes;

        @Field(value = 2)
        public String key;

        @Constructor
        public BlockstoreData(@Param(1) byte[] bytes, @Param(2) String key) {
            this.bytes = bytes;
            this.key = key;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            CREATOR.writeToParcel(this, dest, flags);
        }

        @Override
        public String toString() {
            return ToStringHelper.name("BlockstoreData").field("bytes", bytes == null ? "0" : bytes.length).field("key", key).end();
        }

        public static final SafeParcelableCreatorAndWriter<BlockstoreData> CREATOR = findCreator(BlockstoreData.class);
    }
}