/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.smartdevice.directtransfer;

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
import java.util.Objects;
import java.util.Set;

@SafeParcelable.Class
@Hide
public class UiCustomization extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> FIELD_MAPPINGS;
    static {
        Map<String, FastJsonResponse.Field<?, ?>> fields = new LinkedHashMap<>();
        fields.put("alignment", new FastJsonResponse.Field<>(0, false, 0, false, "alignment", 2, null));
        fields.put("iconSize", new FastJsonResponse.Field<>(0, false, 0, false, "iconSize", 3, null));
        fields.put("titleSize", new FastJsonResponse.Field<>(0, false, 0, false, "titleSize", 4, null));
        FIELD_MAPPINGS = Collections.unmodifiableMap(fields);
    }

    @SafeParcelable.Indicator
    final Set<Integer> presentFields;
    @SafeParcelable.Field(2)
    public int alignment;
    @SafeParcelable.Field(3)
    public int iconSize;
    @SafeParcelable.Field(4)
    public int titleSize;

    public UiCustomization() { presentFields = new HashSet<>(); }

    @SafeParcelable.Constructor
    UiCustomization(@SafeParcelable.Indicator Set<Integer> presentFields,
                    @SafeParcelable.Param(2) int alignment,
                    @SafeParcelable.Param(3) int iconSize,
                    @SafeParcelable.Param(4) int titleSize) {
        this.presentFields = presentFields;
        this.alignment = alignment;
        this.iconSize = iconSize;
        this.titleSize = titleSize;
    }

    @Override
    public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() { return FIELD_MAPPINGS; }

    @Override
    protected Object getFieldValue(FastJsonResponse.Field<?, ?> field) {
        switch (field.safeParcelFieldId) {
            case 2: return alignment;
            case 3: return iconSize;
            case 4: return titleSize;
            default: throw new IllegalStateException("Unknown SafeParcelable id=" + field.safeParcelFieldId);
        }
    }

    @Override
    protected boolean isFieldSet(FastJsonResponse.Field<?, ?> field) { return presentFields.contains(field.safeParcelFieldId); }

    @Override
    protected void setInteger(FastJsonResponse.Field<?, ?> field, int value) {
        switch (field.safeParcelFieldId) {
            case 2: alignment = value; break;
            case 3: iconSize = value; break;
            case 4: titleSize = value; break;
            default: throw new IllegalStateException("Field is not an integer: " + field.outputFieldName);
        }
        presentFields.add(field.safeParcelFieldId);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof UiCustomization)) return false;
        UiCustomization that = (UiCustomization) other;
        return alignment == that.alignment && iconSize == that.iconSize && titleSize == that.titleSize;
    }

    @Override
    public int hashCode() { return Objects.hash(alignment, iconSize, titleSize); }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) { CREATOR.writeToParcel(this, dest, flags); }

    public static final SafeParcelableCreatorAndWriter<UiCustomization> CREATOR = findCreator(UiCustomization.class);
}
