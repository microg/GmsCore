/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.smartdevice.d2d.data;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.common.server.response.FastJsonResponse;
import com.google.android.gms.smartdevice.d2d.BootstrapAccount;
import com.google.android.gms.smartdevice.setup.auth.StartSessionResponse;
import com.google.android.gms.smartdevice.utils.BinarySerializableFastSafeParcelableJson;

import org.microg.gms.common.Hide;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SafeParcelable.Class
@Hide
public class SecondDeviceAuthPayload extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> FIELD_MAPPINGS;
    static {
        Map<String, FastJsonResponse.Field<?, ?>> fields = new LinkedHashMap<>();
        fields.put("startSessionResponses", new FastJsonResponse.Field<>(11, true, 11, true,
                "startSessionResponses", 2, StartSessionResponse.class));
        fields.put("sourceFallbackStatus", new FastJsonResponse.Field<>(0, false, 0, false,
                "sourceFallbackStatus", 3, null));
        fields.put("fidoMessage", new FastJsonResponse.Field<>(8, false, 8, false,
                "fidoMessage", 4, null));
        fields.put("sourceBackupAccountId", new FastJsonResponse.Field<>(7, false, 7, false,
                "fidoMessage", 5, null));
        fields.put("sourceAndroidDeviceId", new FastJsonResponse.Field<>(7, false, 7, false,
                "fidoMessage", 6, null));
        fields.put("isTransferUnicorn", new FastJsonResponse.Field<>(6, false, 6, false,
                "isTransferUnicorn", 7, null));
        fields.put("bootstrapAccounts", new FastJsonResponse.Field<>(11, true, 11, true,
                "bootstrapAccounts", 8, BootstrapAccount.class));
        FIELD_MAPPINGS = Collections.unmodifiableMap(fields);
    }

    @SafeParcelable.Indicator
    final Set<Integer> presentFields;
    @SafeParcelable.Field(value = 2, subClass = StartSessionResponse.class)
    @Nullable
    public List<StartSessionResponse> startSessionResponses;

    @SafeParcelable.Field(value = 3)
    @Deprecated
    public int sourceFallbackStatus;

    @SafeParcelable.Field(value = 4)
    @Nullable
    public byte[] fidoMessage;

    @SafeParcelable.Field(value = 5)
    @Nullable
    public String sourceBackupAccountId;

    @SafeParcelable.Field(value = 6)
    @Nullable
    public String sourceAndroidDeviceId;

    @SafeParcelable.Field(value = 7)
    public boolean isTransferUnicorn;

    @SafeParcelable.Field(value = 8, subClass = BootstrapAccount.class)
    @Nullable
    public List<BootstrapAccount> bootstrapAccounts;

    public SecondDeviceAuthPayload() {
        presentFields = new HashSet<>();
    }

    public SecondDeviceAuthPayload(@Nullable String sourceBackupAccountId,
                                   @Nullable String sourceAndroidDeviceId) {
        this();
        this.sourceBackupAccountId = sourceBackupAccountId;
        this.sourceAndroidDeviceId = sourceAndroidDeviceId;
        presentFields.add(5);
        presentFields.add(6);
    }

    public SecondDeviceAuthPayload(@Nullable List<StartSessionResponse> startSessionResponses) {
        this();
        this.startSessionResponses = startSessionResponses;
        presentFields.add(2);
    }

    public SecondDeviceAuthPayload(boolean isTransferUnicorn) {
        this();
        this.isTransferUnicorn = isTransferUnicorn;
        presentFields.add(7);
    }

    public SecondDeviceAuthPayload(@Nullable byte[] fidoMessage) {
        this();
        this.fidoMessage = fidoMessage;
        presentFields.add(4);
    }

    @SafeParcelable.Constructor
    SecondDeviceAuthPayload(
            @SafeParcelable.Indicator Set<Integer> presentFields,
            @SafeParcelable.Param(2) @Nullable List<StartSessionResponse> startSessionResponses,
            @SafeParcelable.Param(3) int sourceFallbackStatus,
            @SafeParcelable.Param(4) @Nullable byte[] fidoMessage,
            @SafeParcelable.Param(5) @Nullable String sourceBackupAccountId,
            @SafeParcelable.Param(6) @Nullable String sourceAndroidDeviceId,
            @SafeParcelable.Param(7) boolean isTransferUnicorn,
            @SafeParcelable.Param(8) @Nullable List<BootstrapAccount> bootstrapAccounts) {
        this.presentFields = presentFields;
        this.startSessionResponses = startSessionResponses;
        this.sourceFallbackStatus = sourceFallbackStatus;
        this.fidoMessage = fidoMessage;
        this.sourceBackupAccountId = sourceBackupAccountId;
        this.sourceAndroidDeviceId = sourceAndroidDeviceId;
        this.isTransferUnicorn = isTransferUnicorn;
        this.bootstrapAccounts = bootstrapAccounts;
    }

    @Override
    public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() {
        return FIELD_MAPPINGS;
    }

    @Override
    protected Object getFieldValue(FastJsonResponse.Field<?, ?> field) {
        switch (field.safeParcelFieldId) {
            case 2: return startSessionResponses;
            case 3: return sourceFallbackStatus;
            case 4: return fidoMessage;
            case 5: return sourceBackupAccountId;
            case 6: return sourceAndroidDeviceId;
            case 7: return isTransferUnicorn;
            case 8: return bootstrapAccounts;
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
        if (field.safeParcelFieldId != 3) {
            throw new IllegalArgumentException("Field is not sourceFallbackStatus");
        }
        sourceFallbackStatus = value;
        presentFields.add(3);
    }

    @Override
    protected void setDecodedBytes(FastJsonResponse.Field<?, ?> field, byte[] value) {
        if (field.safeParcelFieldId != 4) {
            throw new IllegalArgumentException("Field is not fidoMessage");
        }
        fidoMessage = value;
        presentFields.add(4);
    }

    @Override
    protected void setString(FastJsonResponse.Field<?, ?> field, String value) {
        switch (field.safeParcelFieldId) {
            case 5: sourceBackupAccountId = value; break;
            case 6: sourceAndroidDeviceId = value; break;
            default: throw new IllegalArgumentException(
                    "Field is not a string: " + field.outputFieldName);
        }
        presentFields.add(field.safeParcelFieldId);
    }

    @Override
    protected void setBoolean(FastJsonResponse.Field<?, ?> field, boolean value) {
        if (field.safeParcelFieldId != 7) {
            throw new IllegalArgumentException("Field is not isTransferUnicorn");
        }
        isTransferUnicorn = value;
        presentFields.add(7);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void addConcreteTypeArray(FastJsonResponse.Field<?, ?> field,
                                        List<? extends FastJsonResponse> value) {
        switch (field.safeParcelFieldId) {
            case 2:
                startSessionResponses = (List<StartSessionResponse>) value;
                break;
            case 8:
                bootstrapAccounts = (List<BootstrapAccount>) value;
                break;
            default:
                throw new IllegalArgumentException(
                        "Field is not a concrete array: " + field.outputFieldName);
        }
        presentFields.add(field.safeParcelFieldId);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SecondDeviceAuthPayload> CREATOR =
            findCreator(SecondDeviceAuthPayload.class);
}
