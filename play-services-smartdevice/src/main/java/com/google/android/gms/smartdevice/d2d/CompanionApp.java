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
public class CompanionApp extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> FIELD_MAPPINGS;
    static {
        Map<String, FastJsonResponse.Field<?, ?>> fields = new LinkedHashMap<>();
        fields.put("appName", new FastJsonResponse.Field<>(7, false, 7, false, "appName", 2, null));
        fields.put("packageName", new FastJsonResponse.Field<>(7, false, 7, false, "packageName", 3, null));
        fields.put("installTitle", new FastJsonResponse.Field<>(7, false, 7, false, "installTitle", 4, null));
        fields.put("installMessage", new FastJsonResponse.Field<>(7, false, 7, false, "installMessage", 5, null));
        fields.put("isOptional", new FastJsonResponse.Field<>(6, false, 6, false, "isOptional", 6, null));
        FIELD_MAPPINGS = Collections.unmodifiableMap(fields);
    }

    @SafeParcelable.Indicator final Set<Integer> presentFields;
    @SafeParcelable.Field(value = 2, mayNull = true) public String appName;
    @SafeParcelable.Field(value = 3, mayNull = true) public String packageName;
    @SafeParcelable.Field(value = 4, mayNull = true) public String installTitle;
    @SafeParcelable.Field(value = 5, mayNull = true) public String installMessage;
    @SafeParcelable.Field(6) public boolean isOptional;

    public CompanionApp() {
        presentFields = new HashSet<>();
    }

    @SafeParcelable.Constructor
    CompanionApp(@SafeParcelable.Indicator Set<Integer> presentFields,
                 @SafeParcelable.Param(2) String appName, @SafeParcelable.Param(3) String packageName,
                 @SafeParcelable.Param(4) String installTitle, @SafeParcelable.Param(5) String installMessage,
                 @SafeParcelable.Param(6) boolean isOptional) {
        this.presentFields = presentFields;
        this.appName = appName;
        this.packageName = packageName;
        this.installTitle = installTitle;
        this.installMessage = installMessage;
        this.isOptional = isOptional;
    }

    @Override public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() { return FIELD_MAPPINGS; }
    @Override protected boolean isFieldSet(FastJsonResponse.Field<?, ?> field) { return presentFields.contains(field.safeParcelFieldId); }
    @Override protected Object getFieldValue(FastJsonResponse.Field<?, ?> field) {
        switch (field.safeParcelFieldId) {
            case 2: return appName;
            case 3: return packageName;
            case 4: return installTitle;
            case 5: return installMessage;
            case 6: return isOptional;
            default: throw new IllegalStateException("Unknown SafeParcelable id=" + field.safeParcelFieldId);
        }
    }
    @Override protected void setString(FastJsonResponse.Field<?, ?> field, String value) {
        if (value == null) return;
        switch (field.safeParcelFieldId) {
            case 2: appName = value; break;
            case 3: packageName = value; break;
            case 4: installTitle = value; break;
            case 5: installMessage = value; break;
            default: throw new IllegalArgumentException("Field is not a string: " + field.safeParcelFieldId);
        }
        presentFields.add(field.safeParcelFieldId);
    }
    @Override protected void setBoolean(FastJsonResponse.Field<?, ?> field, boolean value) {
        if (field.safeParcelFieldId != 6) throw new IllegalArgumentException("Field is not isOptional");
        isOptional = value;
        presentFields.add(6);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<CompanionApp> CREATOR = findCreator(CompanionApp.class);
}
