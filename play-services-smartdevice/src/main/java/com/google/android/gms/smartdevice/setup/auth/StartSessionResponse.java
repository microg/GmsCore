/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.smartdevice.setup.auth;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

/**
 * Result for one second-device-auth fallback session.
 */
@SafeParcelable.Class
@Hide
public class StartSessionResponse extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> FIELD_MAPPINGS;
    static {
        Map<String, FastJsonResponse.Field<?, ?>> fields = new LinkedHashMap<>();
        fields.put("status", new FastJsonResponse.Field<>(0, false, 0, false,
                "status", 2, null));
        fields.put("rejectionReason", new FastJsonResponse.Field<>(0, false, 0, false,
                "rejectionReason", 3, null));
        fields.put("credential", new FastJsonResponse.Field<>(7, false, 7, false,
                "credential", 4, null));
        fields.put("targetFallbackUrl", new FastJsonResponse.Field<>(7, false, 7, false,
                "targetFallbackUrl", 5, null));
        fields.put("targetSessionId", new FastJsonResponse.Field<>(7, false, 7, false,
                "targetSessionId", 6, null));
        fields.put("sourceFallbackUrl", new FastJsonResponse.Field<>(7, false, 7, false,
                "sourceFallbackUrl", 7, null));
        fields.put("accountIdentifier", new FastJsonResponse.Field<>(7, false, 7, false,
                "accountIdentifier", 8, null));
        FIELD_MAPPINGS = Collections.unmodifiableMap(fields);
    }

    @SafeParcelable.Indicator
    final Set<Integer> presentFields;
    @SafeParcelable.Field(value = 2)
    public int status;
    @SafeParcelable.Field(value = 3)
    public int rejectionReason;
    @SafeParcelable.Field(value = 4)
    @Nullable
    public String credential;
    @SafeParcelable.Field(value = 5)
    @Nullable
    public String targetFallbackUrl;
    @SafeParcelable.Field(value = 6)
    @Nullable
    public String targetSessionId;
    @SafeParcelable.Field(value = 7)
    @Nullable
    public String sourceFallbackUrl;
    @SafeParcelable.Field(value = 8)
    @Nullable
    public String accountIdentifier;

    public StartSessionResponse() {
        presentFields = new HashSet<>();
    }

    @SafeParcelable.Constructor
    StartSessionResponse(
            @SafeParcelable.Indicator Set<Integer> presentFields,
            @SafeParcelable.Param(2) int status,
            @SafeParcelable.Param(3) int rejectionReason,
            @SafeParcelable.Param(4) @Nullable String credential,
            @SafeParcelable.Param(5) @Nullable String targetFallbackUrl,
            @SafeParcelable.Param(6) @Nullable String targetSessionId,
            @SafeParcelable.Param(7) @Nullable String sourceFallbackUrl,
            @SafeParcelable.Param(8) @Nullable String accountIdentifier) {
        this.presentFields = presentFields;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.credential = credential;
        this.targetFallbackUrl = targetFallbackUrl;
        this.targetSessionId = targetSessionId;
        this.sourceFallbackUrl = sourceFallbackUrl;
        this.accountIdentifier = accountIdentifier;
    }

    @Override
    public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() {
        return FIELD_MAPPINGS;
    }

    @Override
    protected Object getFieldValue(FastJsonResponse.Field<?, ?> field) {
        switch (field.safeParcelFieldId) {
            case 2: return status;
            case 3: return rejectionReason;
            case 4: return credential;
            case 5: return targetFallbackUrl;
            case 6: return targetSessionId;
            case 7: return sourceFallbackUrl;
            case 8: return accountIdentifier;
            default: throw new IllegalStateException(
                    "Unknown SafeParcelable id=" + field.safeParcelFieldId);
        }
    }

    @Override
    protected boolean isFieldSet(FastJsonResponse.Field<?, ?> field) {
        return presentFields.contains(field.safeParcelFieldId);
    }

    @Override
    protected void setInteger(FastJsonResponse.Field<?, ?> field, int value) {
        switch (field.safeParcelFieldId) {
            case 2: status = value; break;
            case 3: rejectionReason = value; break;
            default: throw new IllegalArgumentException(
                    "Field is not an integer: " + field.outputFieldName);
        }
        presentFields.add(field.safeParcelFieldId);
    }

    @Override
    protected void setString(FastJsonResponse.Field<?, ?> field, String value) {
        switch (field.safeParcelFieldId) {
            case 4: credential = value; break;
            case 5: targetFallbackUrl = value; break;
            case 6:
                if (value == null) return;
                targetSessionId = value;
                break;
            case 7: sourceFallbackUrl = value; break;
            case 8: accountIdentifier = value; break;
            default: throw new IllegalArgumentException(
                    "Field is not a string: " + field.outputFieldName);
        }
        presentFields.add(field.safeParcelFieldId);
    }

    public void setTargetSessionId(String value) {
        targetSessionId = value;
        presentFields.add(6);
    }

    public void setAccountIdentifier(String value) {
        accountIdentifier = value;
        presentFields.add(8);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<StartSessionResponse> CREATOR =
            findCreator(StartSessionResponse.class);
}
