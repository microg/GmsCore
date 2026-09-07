/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.smartdevice.d2d;

import android.os.Parcel;

import androidx.annotation.NonNull;

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
public class AdvertisementOptions extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> FIELD_MAPPINGS;
    static {
        Map<String, FastJsonResponse.Field<?, ?>> fields = new LinkedHashMap<>();
        fields.put("verificationStyle", new FastJsonResponse.Field<>(0, false, 0, false,
                "verificationStyle", 1, null));
        FIELD_MAPPINGS = Collections.unmodifiableMap(fields);
    }

    @SafeParcelable.Indicator
    final Set<Integer> presentFields;
    @SafeParcelable.Field(1)
    public int verificationStyle;

    public AdvertisementOptions() {
        presentFields = new HashSet<>();
    }

    public AdvertisementOptions(int verificationStyle) {
        presentFields = new HashSet<>();
        this.verificationStyle = verificationStyle;
        presentFields.add(1);
    }

    @SafeParcelable.Constructor
    AdvertisementOptions(@SafeParcelable.Indicator Set<Integer> presentFields,
                         @SafeParcelable.Param(1) int verificationStyle) {
        this.presentFields = presentFields;
        this.verificationStyle = verificationStyle;
    }

    @Override
    public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() { return FIELD_MAPPINGS; }

    @Override
    protected Object getFieldValue(FastJsonResponse.Field<?, ?> field) {
        if (field.safeParcelFieldId != 1) throw new IllegalStateException("Unknown SafeParcelable id=" + field.safeParcelFieldId);
        return verificationStyle;
    }

    @Override
    protected boolean isFieldSet(FastJsonResponse.Field<?, ?> field) { return presentFields.contains(field.safeParcelFieldId); }

    @Override
    protected void setInteger(FastJsonResponse.Field<?, ?> field, int value) {
        if (field.safeParcelFieldId != 1) throw new IllegalArgumentException("Field is not verificationStyle");
        verificationStyle = value;
        presentFields.add(1);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AdvertisementOptions> CREATOR = findCreator(AdvertisementOptions.class);
}
