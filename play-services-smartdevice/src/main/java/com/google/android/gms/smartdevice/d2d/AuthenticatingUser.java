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
public class AuthenticatingUser extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> FIELD_MAPPINGS;
    static {
        Map<String, FastJsonResponse.Field<?, ?>> fields = new LinkedHashMap<>();
        fields.put("obfuscatedGaiaID", new FastJsonResponse.Field<>(7, false, 7, false, "obfuscatedGaiaID", 2, null));
        fields.put("email", new FastJsonResponse.Field<>(7, false, 7, false, "email", 3, null));
        FIELD_MAPPINGS = Collections.unmodifiableMap(fields);
    }

    @SafeParcelable.Indicator
    final Set<Integer> presentFields;
    @SafeParcelable.Field(value = 2, mayNull = true)
    public String obfuscatedGaiaID;
    @SafeParcelable.Field(value = 3, mayNull = true)
    public String email;

    public AuthenticatingUser() { presentFields = new HashSet<>(); }

    public AuthenticatingUser(String obfuscatedGaiaID, String email) {
        this();
        this.obfuscatedGaiaID = obfuscatedGaiaID;
        this.email = email;
        presentFields.add(2);
        presentFields.add(3);
    }

    @SafeParcelable.Constructor
    AuthenticatingUser(@SafeParcelable.Indicator Set<Integer> presentFields,
                       @SafeParcelable.Param(2) String obfuscatedGaiaID,
                       @SafeParcelable.Param(3) String email) {
        this.presentFields = presentFields;
        this.obfuscatedGaiaID = obfuscatedGaiaID;
        this.email = email;
    }

    @Override
    public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() { return FIELD_MAPPINGS; }

    @Override
    protected Object getFieldValue(FastJsonResponse.Field<?, ?> field) {
        switch (field.safeParcelFieldId) {
            case 2: return obfuscatedGaiaID;
            case 3: return email;
            default: throw new IllegalStateException("Unknown SafeParcelable id=" + field.safeParcelFieldId);
        }
    }

    @Override
    protected boolean isFieldSet(FastJsonResponse.Field<?, ?> field) { return presentFields.contains(field.safeParcelFieldId); }

    @Override
    protected void setString(FastJsonResponse.Field<?, ?> field, String value) {
        if (value == null) return;
        switch (field.safeParcelFieldId) {
            case 2: obfuscatedGaiaID = value; break;
            case 3: email = value; break;
            default: throw new IllegalArgumentException("Field is not a string: " + field.outputFieldName);
        }
        presentFields.add(field.safeParcelFieldId);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) { CREATOR.writeToParcel(this, dest, flags); }

    public static final SafeParcelableCreatorAndWriter<AuthenticatingUser> CREATOR = findCreator(AuthenticatingUser.class);
}
