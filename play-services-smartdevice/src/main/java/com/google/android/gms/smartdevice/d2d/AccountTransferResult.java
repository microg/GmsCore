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
public class AccountTransferResult extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> FIELD_MAPPINGS;
    static {
        Map<String, FastJsonResponse.Field<?, ?>> fields = new LinkedHashMap<>();
        fields.put("bootstrapAccount", new FastJsonResponse.Field<>(11, false, 11, false, "bootstrapAccount", 2, BootstrapAccount.class));
        fields.put("RESULT", new FastJsonResponse.Field<>(0, false, 0, false, "RESULT", 3, null));
        fields.put("lockScreenAuthenticationType", new FastJsonResponse.Field<>(0, false, 0, false, "lockScreenAuthenticationType", 4, null));
        fields.put("supervisedAccountInfo", new FastJsonResponse.Field<>(11, false, 11, false, "supervisedAccountInfo", 5, SupervisedAccountInfo.class));
        FIELD_MAPPINGS = Collections.unmodifiableMap(fields);
    }

    @SafeParcelable.Indicator public final Set<Integer> presentFields;
    @SafeParcelable.Field(value = 2, mayNull = true) public BootstrapAccount bootstrapAccount;
    @SafeParcelable.Field(3) public int result;
    @SafeParcelable.Field(4) public int lockScreenAuthenticationType;
    @SafeParcelable.Field(value = 5, mayNull = true) public SupervisedAccountInfo supervisedAccountInfo;

    public AccountTransferResult() { presentFields = new HashSet<>(); }

    public AccountTransferResult(BootstrapAccount bootstrapAccount, int result) {
        this();
        if (bootstrapAccount == null) throw new NullPointerException("bootstrapAccount");
        this.bootstrapAccount = bootstrapAccount;
        this.result = result;
        presentFields.add(3);
        presentFields.add(2);
    }

    @SafeParcelable.Constructor
    AccountTransferResult(@SafeParcelable.Indicator Set<Integer> presentFields,
                          @SafeParcelable.Param(2) BootstrapAccount bootstrapAccount,
                          @SafeParcelable.Param(3) int result,
                          @SafeParcelable.Param(4) int lockScreenAuthenticationType,
                          @SafeParcelable.Param(5) SupervisedAccountInfo supervisedAccountInfo) {
        this.presentFields = presentFields;
        this.bootstrapAccount = bootstrapAccount;
        this.result = result;
        this.lockScreenAuthenticationType = lockScreenAuthenticationType;
        this.supervisedAccountInfo = supervisedAccountInfo;
    }

    @Override public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() { return FIELD_MAPPINGS; }
    @Override protected boolean isFieldSet(FastJsonResponse.Field<?, ?> field) { return presentFields.contains(field.safeParcelFieldId); }
    @Override protected Object getFieldValue(FastJsonResponse.Field<?, ?> field) {
        switch (field.safeParcelFieldId) {
            case 2: return bootstrapAccount;
            case 3: return result;
            case 4: return lockScreenAuthenticationType;
            case 5: return supervisedAccountInfo;
            default: throw new IllegalStateException("Unknown SafeParcelable id=" + field.safeParcelFieldId);
        }
    }
    @Override protected void setInteger(FastJsonResponse.Field<?, ?> field, int value) {
        if (field.safeParcelFieldId == 3) result = value;
        else if (field.safeParcelFieldId == 4) lockScreenAuthenticationType = value;
        else throw new IllegalArgumentException("Field is not an integer: " + field.outputFieldName);
        presentFields.add(field.safeParcelFieldId);
    }
    @Override protected void addConcreteType(FastJsonResponse.Field<?, ?> field, FastJsonResponse value) {
        if (field.safeParcelFieldId == 2) bootstrapAccount = (BootstrapAccount) value;
        else if (field.safeParcelFieldId == 5) supervisedAccountInfo = (SupervisedAccountInfo) value;
        else throw new IllegalArgumentException("Field is not a concrete type: " + field.outputFieldName);
        presentFields.add(field.safeParcelFieldId);
    }
    public void setLockScreenAuthenticationType(int value) { lockScreenAuthenticationType = value; presentFields.add(4); }
    public void setSupervisedAccountInfo(SupervisedAccountInfo value) { supervisedAccountInfo = value; presentFields.add(5); }
    @Override public void writeToParcel(@NonNull Parcel dest, int flags) { CREATOR.writeToParcel(this, dest, flags); }
    public static final SafeParcelableCreatorAndWriter<AccountTransferResult> CREATOR = findCreator(AccountTransferResult.class);
}
