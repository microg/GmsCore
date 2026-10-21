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
public class BootstrapAccount extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> FIELD_MAPPINGS;
    static {
        Map<String, FastJsonResponse.Field<?, ?>> fields = new LinkedHashMap<>();
        fields.put("name", new FastJsonResponse.Field<>(7, false, 7, false, "name", 2, null));
        fields.put("type", new FastJsonResponse.Field<>(7, false, 7, false, "type", 3, null));
        fields.put("isManaged", new FastJsonResponse.Field<>(6, false, 6, false, "isManaged", 4, null));
        fields.put("parentId", new FastJsonResponse.Field<>(7, false, 7, false, "parentId", 5, null));
        fields.put("isSupervised", new FastJsonResponse.Field<>(6, false, 6, false, "isSupervised", 6, null));
        fields.put("displayName", new FastJsonResponse.Field<>(7, false, 7, false, "displayName", 7, null));
        fields.put("givenName", new FastJsonResponse.Field<>(7, false, 7, false, "givenName", 8, null));
        fields.put("avatarString", new FastJsonResponse.Field<>(7, false, 7, false, "avatarString", 9, null));
        FIELD_MAPPINGS = Collections.unmodifiableMap(fields);
    }

    @SafeParcelable.Indicator
    final Set<Integer> presentFields;
    @SafeParcelable.Field(value = 2, mayNull = true)
    public String name;
    @SafeParcelable.Field(value = 3, mayNull = true)
    public String type;
    @SafeParcelable.Field(value = 4)
    public boolean isManaged;
    @SafeParcelable.Field(value = 5, mayNull = true)
    public String parentId;
    @SafeParcelable.Field(value = 6)
    public boolean isSupervised;
    @SafeParcelable.Field(value = 7, mayNull = true)
    public String displayName;
    @SafeParcelable.Field(value = 8, mayNull = true)
    public String givenName;
    @SafeParcelable.Field(value = 9, mayNull = true)
    public String avatarString;

    public BootstrapAccount() {
        presentFields = new HashSet<>();
    }

    public BootstrapAccount(String name, String type) {
        this();
        this.name = name;
        this.type = type;
        presentFields.add(2);
        presentFields.add(3);
    }

    @SafeParcelable.Constructor
    BootstrapAccount(@SafeParcelable.Indicator Set<Integer> presentFields,
                     @SafeParcelable.Param(2) String name,
                     @SafeParcelable.Param(3) String type,
                     @SafeParcelable.Param(4) boolean isManaged,
                     @SafeParcelable.Param(5) String parentId,
                     @SafeParcelable.Param(6) boolean isSupervised,
                     @SafeParcelable.Param(7) String displayName,
                     @SafeParcelable.Param(8) String givenName,
                     @SafeParcelable.Param(9) String avatarString) {
        this.presentFields = presentFields;
        this.name = name;
        this.type = type;
        this.isManaged = isManaged;
        this.parentId = parentId;
        this.isSupervised = isSupervised;
        this.displayName = displayName;
        this.givenName = givenName;
        this.avatarString = avatarString;
    }

    @Override
    public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() { return FIELD_MAPPINGS; }

    @Override
    protected Object getFieldValue(FastJsonResponse.Field<?, ?> field) {
        switch (field.safeParcelFieldId) {
            case 2: return name;
            case 3: return type;
            case 4: return isManaged;
            case 5: return parentId;
            case 6: return isSupervised;
            case 7: return displayName;
            case 8: return givenName;
            case 9: return avatarString;
            default: throw new IllegalStateException("Unknown SafeParcelable id=" + field.safeParcelFieldId);
        }
    }

    @Override
    protected boolean isFieldSet(FastJsonResponse.Field<?, ?> field) { return presentFields.contains(field.safeParcelFieldId); }

    @Override
    protected void setString(FastJsonResponse.Field<?, ?> field, String value) {
        if (value == null) return;
        switch (field.safeParcelFieldId) {
            case 2: name = value; break;
            case 3: type = value; break;
            case 5: parentId = value; break;
            case 7: displayName = value; break;
            case 8: givenName = value; break;
            case 9: avatarString = value; break;
            default: throw new IllegalArgumentException("Field is not a string: " + field.outputFieldName);
        }
        presentFields.add(field.safeParcelFieldId);
    }

    @Override
    protected void setBoolean(FastJsonResponse.Field<?, ?> field, boolean value) {
        switch (field.safeParcelFieldId) {
            case 4: isManaged = value; break;
            case 6: isSupervised = value; break;
            default: throw new IllegalArgumentException("Field is not a boolean: " + field.outputFieldName);
        }
        presentFields.add(field.safeParcelFieldId);
    }

    public void setParentId(String value) {
        parentId = value;
        presentFields.add(5);
    }

    public void setSupervised(boolean value) {
        isSupervised = value;
        presentFields.add(6);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) { CREATOR.writeToParcel(this, dest, flags); }

    public static final SafeParcelableCreatorAndWriter<BootstrapAccount> CREATOR = findCreator(BootstrapAccount.class);
}
