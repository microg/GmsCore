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
public class SupervisedAccountInfo extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> FIELD_MAPPINGS;
    static {
        Map<String, FastJsonResponse.Field<?, ?>> fields = new LinkedHashMap<>();
        fields.put("parentAccountName", new FastJsonResponse.Field<>(7, false, 7, false, "parentAccountName", 1, null));
        fields.put("accountSelectionToken", new FastJsonResponse.Field<>(7, false, 7, false, "accountSelectionToken", 2, null));
        fields.put("childId", new FastJsonResponse.Field<>(7, false, 7, false, "childId", 3, null));
        FIELD_MAPPINGS = Collections.unmodifiableMap(fields);
    }

    @SafeParcelable.Indicator final Set<Integer> presentFields;
    @SafeParcelable.Field(value = 1, mayNull = true) public String parentAccountName;
    @SafeParcelable.Field(value = 2, mayNull = true) public String accountSelectionToken;
    @SafeParcelable.Field(value = 3, mayNull = true) public String childId;

    public SupervisedAccountInfo() { presentFields = new HashSet<>(); }

    public SupervisedAccountInfo(String parentAccountName, String accountSelectionToken, String childId) {
        this();
        this.parentAccountName = parentAccountName;
        this.accountSelectionToken = accountSelectionToken;
        this.childId = childId;
        presentFields.add(1);
        presentFields.add(2);
        if (childId != null) presentFields.add(3);
    }

    @SafeParcelable.Constructor
    SupervisedAccountInfo(@SafeParcelable.Indicator Set<Integer> presentFields,
                          @SafeParcelable.Param(1) String parentAccountName,
                          @SafeParcelable.Param(2) String accountSelectionToken,
                          @SafeParcelable.Param(3) String childId) {
        this.presentFields = presentFields;
        this.parentAccountName = parentAccountName;
        this.accountSelectionToken = accountSelectionToken;
        this.childId = childId;
    }

    @Override public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() { return FIELD_MAPPINGS; }
    @Override protected boolean isFieldSet(FastJsonResponse.Field<?, ?> field) { return presentFields.contains(field.safeParcelFieldId); }
    @Override protected Object getFieldValue(FastJsonResponse.Field<?, ?> field) {
        switch (field.safeParcelFieldId) {
            case 1: return parentAccountName;
            case 2: return accountSelectionToken;
            case 3: return childId;
            default: throw new IllegalStateException("Unknown SafeParcelable id=" + field.safeParcelFieldId);
        }
    }
    @Override protected void setString(FastJsonResponse.Field<?, ?> field, String value) {
        if (value == null) return;
        switch (field.safeParcelFieldId) {
            case 1: parentAccountName = value; break;
            case 2: accountSelectionToken = value; break;
            case 3: childId = value; break;
            default: throw new IllegalArgumentException("Field is not a string: " + field.outputFieldName);
        }
        presentFields.add(field.safeParcelFieldId);
    }
    @Override public void writeToParcel(@NonNull Parcel dest, int flags) { CREATOR.writeToParcel(this, dest, flags); }
    public static final SafeParcelableCreatorAndWriter<SupervisedAccountInfo> CREATOR = findCreator(SupervisedAccountInfo.class);
}
