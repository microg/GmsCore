/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.smartdevice.setup.accounts;

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
public class UserCredential extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> FIELD_MAPPINGS;

    static {
        Map<String, FastJsonResponse.Field<?, ?>> fields = new LinkedHashMap<>();
        fields.put("accountIdentifier", new FastJsonResponse.Field<>(7, false, 7, false, "accountIdentifier", 2, null));
        fields.put("status", new FastJsonResponse.Field<>(0, false, 0, false, "status", 3, null));
        fields.put("reason", new FastJsonResponse.Field<>(7, false, 7, false, "reason", 4, null));
        fields.put("fallbackUrl", new FastJsonResponse.Field<>(7, false, 7, false, "fallbackUrl", 5, null));
        fields.put("credential", new FastJsonResponse.Field<>(7, false, 7, false, "credential", 6, null));
        fields.put("firstName", new FastJsonResponse.Field<>(7, false, 7, false, "firstName", 7, null));
        fields.put("lastName", new FastJsonResponse.Field<>(7, false, 7, false, "lastName", 8, null));
        fields.put("obfuscatedGaiaId", new FastJsonResponse.Field<>(7, false, 7, false, "obfuscatedGaiaId", 9, null));
        FIELD_MAPPINGS = Collections.unmodifiableMap(fields);
    }

    @SafeParcelable.Indicator
    final Set<Integer> presentFields;
    @SafeParcelable.Field(value = 2, mayNull = true)
    public String accountIdentifier;
    @SafeParcelable.Field(3)
    public int status;
    @SafeParcelable.Field(value = 4, mayNull = true)
    public String reason;
    @SafeParcelable.Field(value = 5, mayNull = true)
    public String fallbackUrl;
    @SafeParcelable.Field(value = 6, mayNull = true)
    public String credential;
    @SafeParcelable.Field(value = 7, mayNull = true)
    public String firstName;
    @SafeParcelable.Field(value = 8, mayNull = true)
    public String lastName;
    @SafeParcelable.Field(value = 9, mayNull = true)
    public String obfuscatedGaiaId;

    public UserCredential() {
        presentFields = new HashSet<>();
    }

    public UserCredential(String accountIdentifier, int status, String reason, String fallbackUrl,
                          String credential, String firstName, String lastName, String obfuscatedGaiaId) {
        this();
        this.accountIdentifier = accountIdentifier;
        this.status = status;
        this.reason = reason;
        this.fallbackUrl = fallbackUrl;
        this.credential = credential;
        this.firstName = firstName;
        this.lastName = lastName;
        this.obfuscatedGaiaId = obfuscatedGaiaId;
        for (int fieldId = 2; fieldId <= 9; fieldId++) presentFields.add(fieldId);
    }

    @SafeParcelable.Constructor
    UserCredential(@SafeParcelable.Indicator Set<Integer> presentFields,
                   @SafeParcelable.Param(2) String accountIdentifier, @SafeParcelable.Param(3) int status,
                   @SafeParcelable.Param(4) String reason, @SafeParcelable.Param(5) String fallbackUrl,
                   @SafeParcelable.Param(6) String credential, @SafeParcelable.Param(7) String firstName,
                   @SafeParcelable.Param(8) String lastName, @SafeParcelable.Param(9) String obfuscatedGaiaId) {
        this.presentFields = presentFields;
        this.accountIdentifier = accountIdentifier;
        this.status = status;
        this.reason = reason;
        this.fallbackUrl = fallbackUrl;
        this.credential = credential;
        this.firstName = firstName;
        this.lastName = lastName;
        this.obfuscatedGaiaId = obfuscatedGaiaId;
    }

    @Override
    public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() {
        return FIELD_MAPPINGS;
    }

    @Override
    protected boolean isFieldSet(FastJsonResponse.Field<?, ?> field) {
        return presentFields.contains(field.safeParcelFieldId);
    }

    @Override
    protected Object getFieldValue(FastJsonResponse.Field<?, ?> field) {
        switch (field.safeParcelFieldId) {
            case 2:
                return accountIdentifier;
            case 3:
                return status;
            case 4:
                return reason;
            case 5:
                return fallbackUrl;
            case 6:
                return credential;
            case 7:
                return firstName;
            case 8:
                return lastName;
            case 9:
                return obfuscatedGaiaId;
            default:
                throw new IllegalStateException("Unknown SafeParcelable id=" + field.safeParcelFieldId);
        }
    }

    @Override
    protected void setInteger(FastJsonResponse.Field<?, ?> field, int value) {
        if (field.safeParcelFieldId != 3) throw new IllegalArgumentException("Field is not status");
        status = value;
        presentFields.add(3);
    }

    @Override
    protected void setString(FastJsonResponse.Field<?, ?> field, String value) {
        if (value == null) return;
        switch (field.safeParcelFieldId) {
            case 2:
                accountIdentifier = value;
                break;
            case 4:
                reason = value;
                break;
            case 5:
                fallbackUrl = value;
                break;
            case 6:
                credential = value;
                break;
            case 7:
                firstName = value;
                break;
            case 8:
                lastName = value;
                break;
            case 9:
                obfuscatedGaiaId = value;
                break;
            default:
                throw new IllegalArgumentException("Field is not a string: " + field.outputFieldName);
        }
        presentFields.add(field.safeParcelFieldId);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<UserCredential> CREATOR = findCreator(UserCredential.class);
}
