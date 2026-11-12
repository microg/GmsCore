/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.smartdevice.d2d.data;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.common.server.response.FastJsonResponse;
import com.google.android.gms.smartdevice.utils.BinarySerializableFastSafeParcelableJson;

import org.microg.gms.common.Hide;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@SafeParcelable.Class
@Hide
public class BlockstorePayload extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> FIELD_MAPPINGS;

    static {
        Map<String, FastJsonResponse.Field<?, ?>> fields = new LinkedHashMap<>();
        fields.put("blockstoreData", new FastJsonResponse.Field<>(
                8, false, 8, false, "blockstoreData", 2, null));
        FIELD_MAPPINGS = Collections.unmodifiableMap(fields);
    }

    @SafeParcelable.Indicator
    final Set<Integer> presentFields;

    @SafeParcelable.Field(value = 2, mayNull = true)
    @Nullable
    public byte[] blockstoreData;

    public BlockstorePayload() {
        presentFields = new HashSet<>();
    }

    public BlockstorePayload(@Nullable byte[] blockstoreData) {
        this();
        this.blockstoreData = blockstoreData;
        presentFields.add(2);
    }

    @SafeParcelable.Constructor
    BlockstorePayload(
            @SafeParcelable.Indicator Set<Integer> presentFields,
            @SafeParcelable.Param(2) @Nullable byte[] blockstoreData
    ) {
        this.presentFields = presentFields;
        this.blockstoreData = blockstoreData;
    }

    @Override
    public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() {
        return FIELD_MAPPINGS;
    }

    @Override
    protected Object getFieldValue(FastJsonResponse.Field<?, ?> field) {
        if (field.safeParcelFieldId != 2) {
            throw new IllegalStateException(
                    "Unknown SafeParcelable id=" + field.safeParcelFieldId);
        }
        return blockstoreData;
    }

    @Override
    protected boolean isFieldSet(FastJsonResponse.Field<?, ?> field) {
        return presentFields.contains(field.safeParcelFieldId);
    }

    @Override
    protected void setDecodedBytes(FastJsonResponse.Field<?, ?> field, byte[] value) {
        if (field.safeParcelFieldId != 2) {
            throw new IllegalArgumentException(
                    "Field with id=" + field.safeParcelFieldId + " is not blockstoreData");
        }
        if (value != null) {
            blockstoreData = value;
            presentFields.add(2);
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<BlockstorePayload> CREATOR =
            findCreator(BlockstorePayload.class);
}
