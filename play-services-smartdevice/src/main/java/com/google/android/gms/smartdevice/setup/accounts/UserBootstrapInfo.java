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
import java.util.Objects;
import java.util.Set;

@SafeParcelable.Class
@Hide
public class UserBootstrapInfo extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> FIELD_MAPPINGS;

    static {
        Map<String, FastJsonResponse.Field<?, ?>> fields = new LinkedHashMap<>();
        fields.put("accountIdentifier", new FastJsonResponse.Field<>(7, false, 7, false, "accountIdentifier", 2, null));
        fields.put("userPublicKey", new FastJsonResponse.Field<>(8, false, 8, false, "userPublicKey", 3, null));
        FIELD_MAPPINGS = Collections.unmodifiableMap(fields);
    }

    @SafeParcelable.Indicator
    final Set<Integer> presentFields;
    @SafeParcelable.Field(value = 2, mayNull = true)
    public String accountIdentifier;
    @SafeParcelable.Field(value = 3, mayNull = true)
    public byte[] userPublicKey;

    public UserBootstrapInfo() {
        presentFields = new HashSet<>();
    }

    public UserBootstrapInfo(String accountIdentifier, byte[] userPublicKey) {
        this();
        this.accountIdentifier = accountIdentifier;
        this.userPublicKey = userPublicKey;
        presentFields.add(2);
        presentFields.add(3);
    }

    @SafeParcelable.Constructor
    UserBootstrapInfo(@SafeParcelable.Indicator Set<Integer> presentFields,
                      @SafeParcelable.Param(2) String accountIdentifier,
                      @SafeParcelable.Param(3) byte[] userPublicKey) {
        if (userPublicKey == null) throw new NullPointerException("userPublicKey");
        this.presentFields = presentFields;
        this.accountIdentifier = accountIdentifier;
        this.userPublicKey = userPublicKey;
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
        if (field.safeParcelFieldId == 2) return accountIdentifier;
        if (field.safeParcelFieldId == 3) return userPublicKey;
        throw new IllegalStateException("Unknown SafeParcelable id=" + field.safeParcelFieldId);
    }

    @Override
    protected void setString(FastJsonResponse.Field<?, ?> field, String value) {
        if (value == null) return;
        if (field.safeParcelFieldId != 2)
            throw new IllegalArgumentException("Field is not accountIdentifier");
        accountIdentifier = value;
        presentFields.add(2);
    }

    @Override
    protected void setDecodedBytes(FastJsonResponse.Field<?, ?> field, byte[] value) {
        if (value == null) return;
        if (field.safeParcelFieldId != 3)
            throw new IllegalArgumentException("Field is not userPublicKey");
        userPublicKey = value;
        presentFields.add(3);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof UserBootstrapInfo)) return false;
        UserBootstrapInfo that = (UserBootstrapInfo) other;
        return Objects.equals(accountIdentifier, that.accountIdentifier) && Objects.deepEquals(userPublicKey, that.userPublicKey);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<UserBootstrapInfo> CREATOR = findCreator(UserBootstrapInfo.class);
}
