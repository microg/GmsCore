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
public class EsimActivationInfo extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> FIELD_MAPPINGS;
    static {
        Map<String, FastJsonResponse.Field<?, ?>> fields = new LinkedHashMap<>();
        fields.put("eid", new FastJsonResponse.Field<>(7, false, 7, false, "eid", 2, null));
        fields.put("imei", new FastJsonResponse.Field<>(7, false, 7, false, "imei", 3, null));
        fields.put("carrierName", new FastJsonResponse.Field<>(7, false, 7, false, "carrierName", 4, null));
        fields.put("carrierId", new FastJsonResponse.Field<>(7, false, 7, false, "carrierId", 5, null));
        FIELD_MAPPINGS = Collections.unmodifiableMap(fields);
    }

    @SafeParcelable.Indicator final Set<Integer> presentFields;
    @SafeParcelable.Field(value = 2, mayNull = true) public String eid;
    @SafeParcelable.Field(value = 3, mayNull = true) public String imei;
    @SafeParcelable.Field(value = 4, mayNull = true) public String carrierName;
    @SafeParcelable.Field(value = 5, mayNull = true) public String carrierId;

    public EsimActivationInfo() {
        presentFields = new HashSet<>();
    }

    @SafeParcelable.Constructor
    EsimActivationInfo(@SafeParcelable.Indicator Set<Integer> presentFields,
                       @SafeParcelable.Param(2) String eid, @SafeParcelable.Param(3) String imei,
                       @SafeParcelable.Param(4) String carrierName, @SafeParcelable.Param(5) String carrierId) {
        this.presentFields = presentFields;
        this.eid = eid;
        this.imei = imei;
        this.carrierName = carrierName;
        this.carrierId = carrierId;
    }

    @Override public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() { return FIELD_MAPPINGS; }
    @Override protected boolean isFieldSet(FastJsonResponse.Field<?, ?> field) { return presentFields.contains(field.safeParcelFieldId); }
    @Override protected Object getFieldValue(FastJsonResponse.Field<?, ?> field) {
        switch (field.safeParcelFieldId) {
            case 2: return eid;
            case 3: return imei;
            case 4: return carrierName;
            case 5: return carrierId;
            default: throw new IllegalStateException("Unknown SafeParcelable id=" + field.safeParcelFieldId);
        }
    }
    @Override protected void setString(FastJsonResponse.Field<?, ?> field, String value) {
        if (value == null) return;
        switch (field.safeParcelFieldId) {
            case 2: eid = value; break;
            case 3: imei = value; break;
            case 4: carrierName = value; break;
            case 5: carrierId = value; break;
            default: throw new IllegalArgumentException("Field is not a string: " + field.safeParcelFieldId);
        }
        presentFields.add(field.safeParcelFieldId);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<EsimActivationInfo> CREATOR = findCreator(EsimActivationInfo.class);
}
