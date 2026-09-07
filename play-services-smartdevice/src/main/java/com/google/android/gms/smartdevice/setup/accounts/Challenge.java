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
public class Challenge extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> FIELD_MAPPINGS;

    static {
        Map<String, FastJsonResponse.Field<?, ?>> fields = new LinkedHashMap<>();
        fields.put("status", new FastJsonResponse.Field<>(0, false, 0, false, "status", 2, null));
        fields.put("accountIdentifier", new FastJsonResponse.Field<>(7, false, 7, false, "accountIdentifier", 3, null));
        fields.put("reason", new FastJsonResponse.Field<>(7, false, 7, false, "reason", 4, null));
        fields.put("challenge", new FastJsonResponse.Field<>(8, false, 8, false, "challenge", 5, null));
        fields.put("challengeSessionState", new FastJsonResponse.Field<>(8, false, 8, false, "challengeSessionState", 6, null));
        FIELD_MAPPINGS = Collections.unmodifiableMap(fields);
    }

    @SafeParcelable.Indicator
    final Set<Integer> presentFields;
    @SafeParcelable.Field(2)
    public int status;
    @SafeParcelable.Field(value = 3, mayNull = true)
    public String accountIdentifier;
    @SafeParcelable.Field(value = 4, mayNull = true)
    public String reason;
    @SafeParcelable.Field(value = 5, mayNull = true)
    public byte[] challenge;
    @SafeParcelable.Field(value = 6, mayNull = true)
    public byte[] challengeSessionState;

    public Challenge() {
        presentFields = new HashSet<>();
    }

    public Challenge(int status, String accountIdentifier, String reason, byte[] challenge, byte[] challengeSessionState) {
        this();
        this.status = status;
        this.accountIdentifier = accountIdentifier;
        this.reason = reason;
        this.challenge = challenge;
        this.challengeSessionState = challengeSessionState;
        for (int fieldId = 2; fieldId <= 6; fieldId++) presentFields.add(fieldId);
    }

    @SafeParcelable.Constructor
    Challenge(@SafeParcelable.Indicator Set<Integer> presentFields, @SafeParcelable.Param(2) int status,
              @SafeParcelable.Param(3) String accountIdentifier, @SafeParcelable.Param(4) String reason,
              @SafeParcelable.Param(5) byte[] challenge, @SafeParcelable.Param(6) byte[] challengeSessionState) {
        this.presentFields = presentFields;
        this.status = status;
        this.accountIdentifier = accountIdentifier;
        this.reason = reason;
        this.challenge = challenge;
        this.challengeSessionState = challengeSessionState;
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
                return status;
            case 3:
                return accountIdentifier;
            case 4:
                return reason;
            case 5:
                return challenge;
            case 6:
                return challengeSessionState;
            default:
                throw new IllegalStateException("Unknown SafeParcelable id=" + field.safeParcelFieldId);
        }
    }

    @Override
    protected void setInteger(FastJsonResponse.Field<?, ?> field, int value) {
        if (field.safeParcelFieldId != 2) throw new IllegalArgumentException("Field is not status");
        status = value;
        presentFields.add(2);
    }

    @Override
    protected void setString(FastJsonResponse.Field<?, ?> field, String value) {
        if (field.safeParcelFieldId == 3) {
            if (value == null) return;
            accountIdentifier = value;
        } else if (field.safeParcelFieldId == 4) reason = value;
        else throw new IllegalArgumentException("Field is not a string: " + field.outputFieldName);
        presentFields.add(field.safeParcelFieldId);
    }

    @Override
    protected void setDecodedBytes(FastJsonResponse.Field<?, ?> field, byte[] value) {
        if (value == null) return;
        if (field.safeParcelFieldId == 5) challenge = value;
        else if (field.safeParcelFieldId == 6) challengeSessionState = value;
        else throw new IllegalArgumentException("Field is not bytes: " + field.outputFieldName);
        presentFields.add(field.safeParcelFieldId);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Challenge> CREATOR = findCreator(Challenge.class);
}
