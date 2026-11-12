/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.smartdevice.d2d.data;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.common.internal.safeparcel.SafeParcelWriter;
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
public class DeviceStatus extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> FIELD_MAPPINGS;
    static {
        Map<String, FastJsonResponse.Field<?, ?>> fields = new LinkedHashMap<>();
        fields.put("errorCode", new FastJsonResponse.Field<>(0, false, 0, false, "errorCode", 1, null));
        fields.put("errorSource", new FastJsonResponse.Field<>(0, false, 0, false, "errorSource", 2, null));
        fields.put("errorCodeFromSource", new FastJsonResponse.Field<>(0, false, 0, false, "errorCodeFromSource", 3, null));
        FIELD_MAPPINGS = Collections.unmodifiableMap(fields);
    }
    @SafeParcelable.Indicator
    final Set<Integer> presentFields;
    @SafeParcelable.Field(value = 1)
    public int errorCode;
    @SafeParcelable.Field(value = 2)
    public int errorSource;
    @SafeParcelable.Field(value = 3)
    public int errorCodeFromSource;

    public DeviceStatus() {
        presentFields = new HashSet<>();
    }

    public DeviceStatus(int errorCode, int errorSource, int errorCodeFromSource) {
        presentFields = new HashSet<>();
        this.errorCode = errorCode;
        this.errorSource = errorSource;
        this.errorCodeFromSource = errorCodeFromSource;
        presentFields.add(1);
        presentFields.add(2);
        presentFields.add(3);
    }

    @SafeParcelable.Constructor
    DeviceStatus(@SafeParcelable.Indicator Set<Integer> presentFields,
                 @SafeParcelable.Param(1) int errorCode,
                 @SafeParcelable.Param(2) int errorSource,
                 @SafeParcelable.Param(3) int errorCodeFromSource) {
        this.presentFields = presentFields;
        this.errorCode = errorCode;
        this.errorSource = errorSource;
        this.errorCodeFromSource = errorCodeFromSource;
    }

    @Override
    public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() { return FIELD_MAPPINGS; }

    @Override
    protected Object getFieldValue(FastJsonResponse.Field<?, ?> field) {
        switch (field.safeParcelFieldId) {
            case 1: return errorCode;
            case 2: return errorSource;
            case 3: return errorCodeFromSource;
            default: throw new IllegalStateException("Unknown SafeParcelable id=" + field.safeParcelFieldId);
        }
    }

    @Override
    protected boolean isFieldSet(FastJsonResponse.Field<?, ?> field) { return presentFields.contains(field.safeParcelFieldId); }

    @Override
    protected void setInteger(FastJsonResponse.Field<?, ?> field, int value) {
        switch (field.safeParcelFieldId) {
            case 1: errorCode = value; break;
            case 2: errorSource = value; break;
            case 3: errorCodeFromSource = value; break;
            default: throw new IllegalArgumentException("Field is not an integer");
        }
        presentFields.add(field.safeParcelFieldId);
    }

    public void setErrorCode(int value) {
        errorCode = value;
        presentFields.add(1);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        int header = SafeParcelWriter.writeObjectHeader(dest);
        if (presentFields.contains(1)) SafeParcelWriter.write(dest, 1, errorCode);
        if (presentFields.contains(2)) SafeParcelWriter.write(dest, 2, errorSource);
        if (presentFields.contains(3)) SafeParcelWriter.write(dest, 3, errorCodeFromSource);
        SafeParcelWriter.finishObjectHeader(dest, header);
    }

    public static final SafeParcelableCreatorAndWriter<DeviceStatus> CREATOR = findCreator(DeviceStatus.class);
}
