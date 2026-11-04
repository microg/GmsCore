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
public class PostTransferAction extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> FIELD_MAPPINGS;
    static {
        Map<String, FastJsonResponse.Field<?, ?>> fields = new LinkedHashMap<>();
        fields.put("uri", new FastJsonResponse.Field<>(7, false, 7, false, "uri", 2, null));
        fields.put("wifiD2D", new FastJsonResponse.Field<>(7, false, 7, false, "wifiD2D", 3, null));
        fields.put("OEM", new FastJsonResponse.Field<>(7, false, 7, false, "OEM", 4, null));
        fields.put("OEM_package", new FastJsonResponse.Field<>(7, false, 7, false, "OEM_package", 5, null));
        fields.put("target_OEM_package_version", new FastJsonResponse.Field<>(2, false, 2, false,
                "target_OEM_package_version", 6, null));
        FIELD_MAPPINGS = Collections.unmodifiableMap(fields);
    }

    @SafeParcelable.Indicator public final Set<Integer> presentFields;
    @SafeParcelable.Field(value = 2, mayNull = true) public String uri;
    @SafeParcelable.Field(value = 3, mayNull = true) public String wifiD2D;
    @SafeParcelable.Field(value = 4, mayNull = true) public String oem;
    @SafeParcelable.Field(value = 5, mayNull = true) public String oemPackage;
    @SafeParcelable.Field(6) public long targetOemPackageVersion;

    public PostTransferAction() {
        presentFields = new HashSet<>();
        targetOemPackageVersion = 0L;
    }

    @SafeParcelable.Constructor
    PostTransferAction(@SafeParcelable.Indicator Set<Integer> presentFields,
                       @SafeParcelable.Param(2) String uri, @SafeParcelable.Param(3) String wifiD2D,
                       @SafeParcelable.Param(4) String oem, @SafeParcelable.Param(5) String oemPackage,
                       @SafeParcelable.Param(6) long targetOemPackageVersion) {
        this.presentFields = presentFields;
        this.uri = uri;
        this.wifiD2D = wifiD2D;
        this.oem = oem;
        this.oemPackage = oemPackage;
        this.targetOemPackageVersion = targetOemPackageVersion;
    }

    @Override public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() { return FIELD_MAPPINGS; }
    @Override protected boolean isFieldSet(FastJsonResponse.Field<?, ?> field) { return presentFields.contains(field.safeParcelFieldId); }
    @Override protected Object getFieldValue(FastJsonResponse.Field<?, ?> field) {
        switch (field.safeParcelFieldId) {
            case 2: return uri;
            case 3: return wifiD2D;
            case 4: return oem;
            case 5: return oemPackage;
            case 6: return targetOemPackageVersion;
            default: throw new IllegalStateException("Unknown SafeParcelable id=" + field.safeParcelFieldId);
        }
    }
    @Override protected void setString(FastJsonResponse.Field<?, ?> field, String value) {
        switch (field.safeParcelFieldId) {
            case 2: uri = value; break;
            case 3: wifiD2D = value; break;
            case 4: oem = value; break;
            case 5: oemPackage = value; break;
            default: throw new IllegalArgumentException("Field is not a string: " + field.safeParcelFieldId);
        }
        presentFields.add(field.safeParcelFieldId);
    }
    @Override protected void setLong(FastJsonResponse.Field<?, ?> field, long value) {
        if (field.safeParcelFieldId != 6) throw new IllegalArgumentException("Field is not target_OEM_package_version");
        targetOemPackageVersion = value;
        presentFields.add(6);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PostTransferAction> CREATOR = findCreator(PostTransferAction.class);
}
