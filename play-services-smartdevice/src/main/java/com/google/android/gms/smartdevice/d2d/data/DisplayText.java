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
public class DisplayText extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> FIELD_MAPPINGS;
    static {
        Map<String, FastJsonResponse.Field<?, ?>> fields = new LinkedHashMap<>();
        fields.put("text", new FastJsonResponse.Field<>(7, false, 7, false, "text", 2, null));
        FIELD_MAPPINGS = Collections.unmodifiableMap(fields);
    }
    @SafeParcelable.Indicator
    final Set<Integer> presentFields;
    @SafeParcelable.Field(value = 2, mayNull = true)
    public String text;

    public DisplayText() {
        presentFields = new HashSet<>();
    }

    public DisplayText(String text) {
        presentFields = new HashSet<>();
        this.text = text;
        presentFields.add(2);
    }

    @SafeParcelable.Constructor
    DisplayText(@SafeParcelable.Indicator Set<Integer> presentFields, @SafeParcelable.Param(2) String text) {
        this.presentFields = presentFields;
        this.text = text;
    }

    @Override
    public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() { return FIELD_MAPPINGS; }

    @Override
    protected Object getFieldValue(FastJsonResponse.Field<?, ?> field) {
        if (field.safeParcelFieldId != 2) throw new IllegalStateException("Unknown SafeParcelable id=" + field.safeParcelFieldId);
        return text;
    }

    @Override
    protected boolean isFieldSet(FastJsonResponse.Field<?, ?> field) { return presentFields.contains(field.safeParcelFieldId); }

    @Override
    protected void setString(FastJsonResponse.Field<?, ?> field, String value) {
        if (field.safeParcelFieldId != 2) throw new IllegalArgumentException("Field is not text");
        text = value;
        presentFields.add(2);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        int header = SafeParcelWriter.writeObjectHeader(dest);
        if (presentFields.contains(2)) SafeParcelWriter.write(dest, 2, text, true);
        SafeParcelWriter.finishObjectHeader(dest, header);
    }

    public static final SafeParcelableCreatorAndWriter<DisplayText> CREATOR = findCreator(DisplayText.class);
}
