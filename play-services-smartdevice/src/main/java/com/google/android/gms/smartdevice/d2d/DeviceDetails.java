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
public class DeviceDetails extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> FIELD_MAPPINGS;
    static {
        Map<String, FastJsonResponse.Field<?, ?>> fields = new LinkedHashMap<>();
        fields.put("androidId", new FastJsonResponse.Field<>(2, false, 2, false, "androidId", 2, null));
        fields.put("gmsVersion", new FastJsonResponse.Field<>(0, false, 0, false, "gmsVersion", 3, null));
        fields.put("isSourceIos", new FastJsonResponse.Field<>(6, false, 6, false, "isSourceIos", 4, null));
        fields.put("useEnrollment2", new FastJsonResponse.Field<>(6, false, 6, false, "useEnrollment2", 5, null));
        fields.put("hasConnectivity", new FastJsonResponse.Field<>(6, false, 6, false, "hasConnectivity", 6, null));
        fields.put("hasLockscreen", new FastJsonResponse.Field<>(6, false, 6, false, "hasLockscreen", 7, null));
        fields.put("model", new FastJsonResponse.Field<>(7, false, 7, false, "model", 8, null));
        fields.put("deviceType", new FastJsonResponse.Field<>(0, false, 0, false, "deviceType", 9, null));
        fields.put("deviceFingerprint", new FastJsonResponse.Field<>(7, false, 7, false, "deviceFingerprint", 10, null));
        fields.put("instanceId", new FastJsonResponse.Field<>(7, false, 7, false, "instanceId", 11, null));
        FIELD_MAPPINGS = Collections.unmodifiableMap(fields);
    }

    @SafeParcelable.Indicator
    final Set<Integer> presentFields;
    @SafeParcelable.Field(value = 2)
    public long androidId;
    @SafeParcelable.Field(value = 3)
    public int gmsVersion;
    @SafeParcelable.Field(value = 4)
    public boolean isSourceIos;
    @SafeParcelable.Field(value = 5)
    public boolean useEnrollment2;
    @SafeParcelable.Field(value = 6)
    public boolean hasConnectivity;
    @SafeParcelable.Field(value = 7)
    public boolean hasLockscreen;
    @SafeParcelable.Field(value = 8, mayNull = true)
    public String model;
    @SafeParcelable.Field(value = 9)
    public byte deviceType;
    @SafeParcelable.Field(value = 10, mayNull = true)
    public String deviceFingerprint;
    @SafeParcelable.Field(value = 11, mayNull = true)
    public String instanceId;

    public DeviceDetails() { presentFields = new HashSet<>(); }

    public DeviceDetails(long androidId, int gmsVersion) {
        this();
        this.androidId = androidId;
        this.gmsVersion = gmsVersion;
        this.isSourceIos = false;
        this.useEnrollment2 = true;
        presentFields.add(2);
        presentFields.add(3);
        presentFields.add(4);
        presentFields.add(5);
    }

    @SafeParcelable.Constructor
    DeviceDetails(@SafeParcelable.Indicator Set<Integer> presentFields,
                  @SafeParcelable.Param(2) long androidId,
                  @SafeParcelable.Param(3) int gmsVersion,
                  @SafeParcelable.Param(4) boolean isSourceIos,
                  @SafeParcelable.Param(5) boolean useEnrollment2,
                  @SafeParcelable.Param(6) boolean hasConnectivity,
                  @SafeParcelable.Param(7) boolean hasLockscreen,
                  @SafeParcelable.Param(8) String model,
                  @SafeParcelable.Param(9) byte deviceType,
                  @SafeParcelable.Param(10) String deviceFingerprint,
                  @SafeParcelable.Param(11) String instanceId) {
        this.presentFields = presentFields;
        this.androidId = androidId;
        this.gmsVersion = gmsVersion;
        this.isSourceIos = isSourceIos;
        this.useEnrollment2 = useEnrollment2;
        this.hasConnectivity = hasConnectivity;
        this.hasLockscreen = hasLockscreen;
        this.model = model;
        this.deviceType = deviceType;
        this.deviceFingerprint = deviceFingerprint;
        this.instanceId = instanceId;
    }

    @Override
    public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() { return FIELD_MAPPINGS; }

    @Override
    protected Object getFieldValue(FastJsonResponse.Field<?, ?> field) {
        switch (field.safeParcelFieldId) {
            case 2: return androidId;
            case 3: return gmsVersion;
            case 4: return isSourceIos;
            case 5: return useEnrollment2;
            case 6: return hasConnectivity;
            case 7: return hasLockscreen;
            case 8: return model;
            case 9: return (int) deviceType;
            case 10: return deviceFingerprint;
            case 11: return instanceId;
            default: throw new IllegalStateException("Unknown SafeParcelable id=" + field.safeParcelFieldId);
        }
    }

    @Override
    protected boolean isFieldSet(FastJsonResponse.Field<?, ?> field) { return presentFields.contains(field.safeParcelFieldId); }

    @Override
    protected void setString(FastJsonResponse.Field<?, ?> field, String value) {
        if (value == null) return;
        switch (field.safeParcelFieldId) {
            case 8: model = value; break;
            case 10: deviceFingerprint = value; break;
            case 11: instanceId = value; break;
            default: throw new IllegalArgumentException("Field is not a string: " + field.outputFieldName);
        }
        presentFields.add(field.safeParcelFieldId);
    }

    @Override
    protected void setInteger(FastJsonResponse.Field<?, ?> field, int value) {
        switch (field.safeParcelFieldId) {
            case 3: gmsVersion = value; break;
            case 9: deviceType = (byte) value; break;
            default: throw new IllegalArgumentException("Field is not an integer: " + field.outputFieldName);
        }
        presentFields.add(field.safeParcelFieldId);
    }

    @Override
    protected void setBoolean(FastJsonResponse.Field<?, ?> field, boolean value) {
        switch (field.safeParcelFieldId) {
            case 4: isSourceIos = value; break;
            case 5: useEnrollment2 = value; break;
            case 6: hasConnectivity = value; break;
            case 7: hasLockscreen = value; break;
            default: throw new IllegalArgumentException("Field is not a boolean: " + field.outputFieldName);
        }
        presentFields.add(field.safeParcelFieldId);
    }

    @Override
    protected void setLong(FastJsonResponse.Field<?, ?> field, long value) {
        if (field.safeParcelFieldId != 2) throw new IllegalArgumentException("Field is not androidId");
        androidId = value;
        presentFields.add(2);
    }

    public void setHasConnectivity(boolean value) { hasConnectivity = value; presentFields.add(6); }
    public void setHasLockscreen(boolean value) { hasLockscreen = value; presentFields.add(7); }
    public void setModel(String value) { model = value; presentFields.add(8); }
    public void setDeviceType(byte value) { deviceType = value; presentFields.add(9); }
    public void setDeviceFingerprint(String value) { deviceFingerprint = value; presentFields.add(10); }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) { CREATOR.writeToParcel(this, dest, flags); }

    public static final SafeParcelableCreatorAndWriter<DeviceDetails> CREATOR = findCreator(DeviceDetails.class);
}
