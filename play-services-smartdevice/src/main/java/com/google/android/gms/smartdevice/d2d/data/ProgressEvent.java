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
public class ProgressEvent extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> FIELD_MAPPINGS;
    static {
        Map<String, FastJsonResponse.Field<?, ?>> fields = new LinkedHashMap<>();
        fields.put("progressType", new FastJsonResponse.Field<>(0, false, 0, false, "progressType", 2, null));
        FIELD_MAPPINGS = Collections.unmodifiableMap(fields);
    }
    @SafeParcelable.Indicator
    final Set<Integer> presentFields;
    @SafeParcelable.Field(2)
    public int progressType;

    public ProgressEvent() {
        presentFields = new HashSet<>();
    }

    public ProgressEvent(int progressType) {
        presentFields = new HashSet<>();
        this.progressType = progressType;
        presentFields.add(2);
    }

    @SafeParcelable.Constructor
    ProgressEvent(@SafeParcelable.Indicator Set<Integer> presentFields, @SafeParcelable.Param(2) int progressType) {
        this.presentFields = presentFields;
        this.progressType = progressType;
    }

    @Override
    public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() { return FIELD_MAPPINGS; }

    @Override
    protected Object getFieldValue(FastJsonResponse.Field<?, ?> field) {
        if (field.safeParcelFieldId != 2) throw new IllegalStateException("Unknown SafeParcelable id=" + field.safeParcelFieldId);
        return progressType;
    }

    @Override
    protected boolean isFieldSet(FastJsonResponse.Field<?, ?> field) { return presentFields.contains(field.safeParcelFieldId); }

    @Override
    protected void setInteger(FastJsonResponse.Field<?, ?> field, int value) {
        if (field.safeParcelFieldId != 2) throw new IllegalArgumentException("Field is not progressType");
        progressType = value;
        presentFields.add(2);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        int header = SafeParcelWriter.writeObjectHeader(dest);
        if (presentFields.contains(2)) SafeParcelWriter.write(dest, 2, progressType);
        SafeParcelWriter.finishObjectHeader(dest, header);
    }

    public static final SafeParcelableCreatorAndWriter<ProgressEvent> CREATOR = findCreator(ProgressEvent.class);
}
