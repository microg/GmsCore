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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SafeParcelable.Class
@Hide
public class BootstrapOptions extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> FIELD_MAPPINGS;
    static {
        Map<String, FastJsonResponse.Field<?, ?>> fields = new LinkedHashMap<>();
        fields.put("protocol", new FastJsonResponse.Field<>(0, false, 0, false, "protocol", 2, null));
        fields.put("accountRequirement", new FastJsonResponse.Field<>(0, false, 0, false, "accountRequirement", 3, null));
        fields.put("isWiFiBootstrappable", new FastJsonResponse.Field<>(6, false, 6, false, "isWiFiBootstrappable", 4, null));
        fields.put("visibleWiFiSSIDs", new FastJsonResponse.Field<>(7, true, 7, true, "visibleWiFiSSIDs", 5, null));
        fields.put("deviceType", new FastJsonResponse.Field<>(0, false, 0, false, "deviceType", 6, null));
        fields.put("deviceName", new FastJsonResponse.Field<>(7, false, 7, false, "deviceName", 7, null));
        fields.put("companionApp", new FastJsonResponse.Field<>(11, false, 11, false, "companionApp", 8, CompanionApp.class));
        fields.put("isSourceSideChallengeRequired", new FastJsonResponse.Field<>(6, false, 6, false, "isSourceSideChallengeRequired", 9, null));
        fields.put("flowType", new FastJsonResponse.Field<>(0, false, 0, false, "flowType", 10, null));
        fields.put("transportMedium", new FastJsonResponse.Field<>(0, false, 0, false, "transportMedium", 11, null));
        fields.put("sessionId", new FastJsonResponse.Field<>(2, false, 2, false, "sessionId", 12, null));
        fields.put("supportsDevicePolicySetup", new FastJsonResponse.Field<>(6, false, 6, false, "supportsDevicePolicySetup", 13, null));
        fields.put("supportsAccountTransferImport", new FastJsonResponse.Field<>(6, false, 6, false, "supportsAccountTransferImport", 14, null));
        fields.put("isAccountTransferImportAllowed", new FastJsonResponse.Field<>(6, false, 6, false, "isAccountTransferImportAllowed", 15, null));
        fields.put("supportsPacketMode", new FastJsonResponse.Field<>(6, false, 6, false, "supportsPacketMode", 16, null));
        fields.put("maxPacketSize", new FastJsonResponse.Field<>(0, false, 0, false, "maxPacketSize", 17, null));
        fields.put("optionFlags", new FastJsonResponse.Field<>(2, false, 2, false, "optionFlags", 18, null));
        fields.put("gmsVersion", new FastJsonResponse.Field<>(0, false, 0, false, "gmsVersion", 19, null));
        fields.put("optionFlagsSetIndicator", new FastJsonResponse.Field<>(2, false, 2, false, "optionFlagsSetIndicator", 20, null));
        fields.put("PostTransferAction", new FastJsonResponse.Field<>(11, false, 11, false, "PostTransferAction", 21, PostTransferAction.class));
        fields.put("SharedSecret", new FastJsonResponse.Field<>(8, false, 8, false, "SharedSecret", 22, null));
        fields.put("esimActivationInfo", new FastJsonResponse.Field<>(11, false, 11, false, "esimActivationInfo", 23, EsimActivationInfo.class));
        fields.put("advertisementOptions", new FastJsonResponse.Field<>(11, false, 11, false, "advertisementOptions", 24, AdvertisementOptions.class));
        fields.put("moduleVersion", new FastJsonResponse.Field<>(0, false, 0, false, "moduleVersion", 25, null));
        fields.put("nearbyDirectTransfer", new FastJsonResponse.Field<>(6, false, 6, false, "nearbyDirectTransfer", 26, null));
        fields.put("deeplinkPairingCode", new FastJsonResponse.Field<>(7, false, 7, false, "deeplinkPairingCode", 27, null));
        fields.put("targetSuwStatus", new FastJsonResponse.Field<>(0, false, 0, false, "targetSuwStatus", 28, null));
        fields.put("accountsOnTarget", new FastJsonResponse.Field<>(11, true, 11, true, "accountsOnTarget", 29, BootstrapAccount.class));
        fields.put("targetDeviceFingerprint", new FastJsonResponse.Field<>(7, false, 7, false, "targetDeviceFingerprint", 30, null));
        FIELD_MAPPINGS = Collections.unmodifiableMap(fields);
    }

    @SafeParcelable.Indicator
    final Set<Integer> presentFields;
    @SafeParcelable.Field(value = 2)
    public int protocol;
    @SafeParcelable.Field(value = 3)
    public int accountRequirement;
    @SafeParcelable.Field(value = 4)
    public boolean isWiFiBootstrappable;
    @SafeParcelable.Field(value = 5, mayNull = true)
    public List<String> visibleWiFiSSIDs;
    @SafeParcelable.Field(value = 6)
    public byte deviceType;
    @SafeParcelable.Field(value = 7, mayNull = true)
    public String deviceName;
    @SafeParcelable.Field(value = 8, mayNull = true)
    public CompanionApp companionApp;
    @SafeParcelable.Field(value = 9)
    public boolean isSourceSideChallengeRequired;
    @SafeParcelable.Field(value = 10)
    public int flowType;
    @SafeParcelable.Field(value = 11)
    public int transportMedium;
    @SafeParcelable.Field(value = 12)
    public long sessionId;
    @SafeParcelable.Field(value = 13)
    public boolean supportsDevicePolicySetup;
    @SafeParcelable.Field(value = 14)
    public boolean supportsAccountTransferImport;
    @SafeParcelable.Field(value = 15)
    public boolean isAccountTransferImportAllowed;
    @SafeParcelable.Field(value = 16)
    public boolean supportsPacketMode;
    @SafeParcelable.Field(value = 17)
    public int maxPacketSize;
    @SafeParcelable.Field(value = 18)
    public long optionFlags;
    @SafeParcelable.Field(value = 19)
    public int gmsVersion;
    @SafeParcelable.Field(value = 20)
    public long optionFlagsSetIndicator;
    @SafeParcelable.Field(value = 21, mayNull = true)
    public PostTransferAction postTransferAction;
    @SafeParcelable.Field(value = 22, mayNull = true)
    public byte[] sharedSecret;
    @SafeParcelable.Field(value = 23, mayNull = true)
    public EsimActivationInfo esimActivationInfo;
    @SafeParcelable.Field(value = 24, mayNull = true)
    public AdvertisementOptions advertisementOptions;
    @SafeParcelable.Field(value = 25)
    public int moduleVersion;
    @SafeParcelable.Field(value = 26)
    public boolean nearbyDirectTransfer;
    @SafeParcelable.Field(value = 27, mayNull = true)
    public String deeplinkPairingCode;
    @SafeParcelable.Field(value = 28)
    public byte targetSuwStatus;
    @SafeParcelable.Field(value = 29, subClass = BootstrapAccount.class, mayNull = true)
    public List<BootstrapAccount> accountsOnTarget;
    @SafeParcelable.Field(value = 30, mayNull = true)
    public String targetDeviceFingerprint;

    public BootstrapOptions() {
        presentFields = new HashSet<>();
    }

    @SafeParcelable.Constructor
    BootstrapOptions(@SafeParcelable.Indicator Set<Integer> presentFields,
                            @SafeParcelable.Param(2) int protocol, @SafeParcelable.Param(3) int accountRequirement,
                            @SafeParcelable.Param(4) boolean isWiFiBootstrappable, @SafeParcelable.Param(5) List<String> visibleWiFiSSIDs,
                            @SafeParcelable.Param(6) byte deviceType, @SafeParcelable.Param(7) String deviceName,
                            @SafeParcelable.Param(8) CompanionApp companionApp, @SafeParcelable.Param(9) boolean isSourceSideChallengeRequired,
                            @SafeParcelable.Param(10) int flowType, @SafeParcelable.Param(11) int transportMedium,
                            @SafeParcelable.Param(12) long sessionId, @SafeParcelable.Param(13) boolean supportsDevicePolicySetup,
                            @SafeParcelable.Param(14) boolean supportsAccountTransferImport,
                            @SafeParcelable.Param(15) boolean isAccountTransferImportAllowed,
                            @SafeParcelable.Param(16) boolean supportsPacketMode, @SafeParcelable.Param(17) int maxPacketSize,
                            @SafeParcelable.Param(18) long optionFlags, @SafeParcelable.Param(19) int gmsVersion,
                            @SafeParcelable.Param(20) long optionFlagsSetIndicator,
                            @SafeParcelable.Param(21) PostTransferAction postTransferAction,
                            @SafeParcelable.Param(22) byte[] sharedSecret,
                            @SafeParcelable.Param(23) EsimActivationInfo esimActivationInfo,
                            @SafeParcelable.Param(24) AdvertisementOptions advertisementOptions,
                            @SafeParcelable.Param(25) int moduleVersion, @SafeParcelable.Param(26) boolean nearbyDirectTransfer,
                            @SafeParcelable.Param(27) String deeplinkPairingCode, @SafeParcelable.Param(28) byte targetSuwStatus,
                            @SafeParcelable.Param(29) List<BootstrapAccount> accountsOnTarget,
                            @SafeParcelable.Param(30) String targetDeviceFingerprint) {
        this.presentFields = presentFields;
        this.protocol = protocol;
        this.accountRequirement = accountRequirement;
        this.isWiFiBootstrappable = isWiFiBootstrappable;
        this.visibleWiFiSSIDs = visibleWiFiSSIDs;
        this.deviceType = deviceType;
        this.deviceName = deviceName;
        this.companionApp = companionApp;
        this.isSourceSideChallengeRequired = isSourceSideChallengeRequired;
        this.flowType = flowType;
        this.transportMedium = transportMedium;
        this.sessionId = sessionId;
        this.supportsDevicePolicySetup = supportsDevicePolicySetup;
        this.supportsAccountTransferImport = supportsAccountTransferImport;
        this.isAccountTransferImportAllowed = isAccountTransferImportAllowed;
        this.supportsPacketMode = supportsPacketMode;
        this.maxPacketSize = maxPacketSize;
        this.optionFlags = optionFlags;
        this.gmsVersion = gmsVersion;
        this.optionFlagsSetIndicator = optionFlagsSetIndicator;
        this.postTransferAction = postTransferAction;
        this.sharedSecret = sharedSecret;
        this.esimActivationInfo = esimActivationInfo;
        this.advertisementOptions = advertisementOptions;
        this.moduleVersion = moduleVersion;
        this.nearbyDirectTransfer = nearbyDirectTransfer;
        this.deeplinkPairingCode = deeplinkPairingCode;
        this.targetSuwStatus = targetSuwStatus;
        this.accountsOnTarget = accountsOnTarget;
        this.targetDeviceFingerprint = targetDeviceFingerprint;
    }

    @Override
    public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() {
        return FIELD_MAPPINGS;
    }

    @Override
    protected Object getFieldValue(FastJsonResponse.Field<?, ?> field) {
        switch (field.safeParcelFieldId) {
            case 2: return protocol;
            case 3: return accountRequirement;
            case 4: return isWiFiBootstrappable;
            case 5: return visibleWiFiSSIDs;
            case 6: return deviceType;
            case 7: return deviceName;
            case 8: return companionApp;
            case 9: return isSourceSideChallengeRequired;
            case 10: return flowType;
            case 11: return transportMedium;
            case 12: return sessionId;
            case 13: return supportsDevicePolicySetup;
            case 14: return supportsAccountTransferImport;
            case 15: return isAccountTransferImportAllowed;
            case 16: return supportsPacketMode;
            case 17: return maxPacketSize;
            case 18: return optionFlags;
            case 19: return gmsVersion;
            case 20: return optionFlagsSetIndicator;
            case 21: return postTransferAction;
            case 22: return sharedSecret;
            case 23: return esimActivationInfo;
            case 24: return advertisementOptions;
            case 25: return moduleVersion;
            case 26: return nearbyDirectTransfer;
            case 27: return deeplinkPairingCode;
            case 28: return targetSuwStatus;
            case 29: return accountsOnTarget;
            case 30: return targetDeviceFingerprint;
            default: throw new IllegalStateException("Unknown SafeParcelable id=" + field.safeParcelFieldId);
        }
    }

    @Override
    protected boolean isFieldSet(FastJsonResponse.Field<?, ?> field) {
        return presentFields.contains(field.safeParcelFieldId);
    }

    @Override
    protected void setInteger(FastJsonResponse.Field<?, ?> field, int value) {
        switch (field.safeParcelFieldId) {
            case 2: protocol = value; break;
            case 3: accountRequirement = value; break;
            case 6: deviceType = (byte) value; break;
            case 10: flowType = value; break;
            case 11: transportMedium = value; break;
            case 17: maxPacketSize = value; break;
            case 19: gmsVersion = value; break;
            case 25: moduleVersion = value; break;
            case 28: targetSuwStatus = (byte) value; break;
            default: throw new IllegalArgumentException("Field is not an integer: " + field.safeParcelFieldId);
        }
        presentFields.add(field.safeParcelFieldId);
    }

    @Override
    protected void setLong(FastJsonResponse.Field<?, ?> field, long value) {
        switch (field.safeParcelFieldId) {
            case 12: sessionId = value; break;
            case 18: optionFlags = value; break;
            case 20: optionFlagsSetIndicator = value; break;
            default: throw new IllegalArgumentException("Field is not a long: " + field.safeParcelFieldId);
        }
        presentFields.add(field.safeParcelFieldId);
    }

    @Override
    protected void setBoolean(FastJsonResponse.Field<?, ?> field, boolean value) {
        switch (field.safeParcelFieldId) {
            case 4: isWiFiBootstrappable = value; break;
            case 9: isSourceSideChallengeRequired = value; break;
            case 13: supportsDevicePolicySetup = value; break;
            case 14: supportsAccountTransferImport = value; break;
            case 15: isAccountTransferImportAllowed = value; break;
            case 16: supportsPacketMode = value; break;
            case 26: nearbyDirectTransfer = value; break;
            default: throw new IllegalArgumentException("Field is not a boolean: " + field.safeParcelFieldId);
        }
        presentFields.add(field.safeParcelFieldId);
    }

    @Override
    protected void setString(FastJsonResponse.Field<?, ?> field, String value) {
        switch (field.safeParcelFieldId) {
            case 7:
                if (value == null) return;
                deviceName = value;
                break;
            case 27: deeplinkPairingCode = value; break;
            case 30: targetDeviceFingerprint = value; break;
            default: throw new IllegalArgumentException("Field is not a string: " + field.safeParcelFieldId);
        }
        presentFields.add(field.safeParcelFieldId);
    }

    @Override
    protected void setStringArray(FastJsonResponse.Field<?, ?> field, List<String> value) {
        if (field.safeParcelFieldId != 5) throw new IllegalArgumentException("Field is not visibleWiFiSSIDs");
        visibleWiFiSSIDs = value;
        presentFields.add(5);
    }

    @Override
    protected void setDecodedBytes(FastJsonResponse.Field<?, ?> field, byte[] value) {
        if (field.safeParcelFieldId != 22) throw new IllegalArgumentException("Field is not SharedSecret");
        sharedSecret = value;
        presentFields.add(22);
    }

    @Override
    protected void addConcreteType(FastJsonResponse.Field<?, ?> field, FastJsonResponse value) {
        switch (field.safeParcelFieldId) {
            case 8: companionApp = (CompanionApp) value; break;
            case 21: postTransferAction = (PostTransferAction) value; break;
            case 23: esimActivationInfo = (EsimActivationInfo) value; break;
            case 24: advertisementOptions = (AdvertisementOptions) value; break;
            default: throw new IllegalArgumentException("Field is not a concrete type: " + field.safeParcelFieldId);
        }
        presentFields.add(field.safeParcelFieldId);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void addConcreteTypeArray(FastJsonResponse.Field<?, ?> field,
                                        List<? extends FastJsonResponse> value) {
        if (field.safeParcelFieldId != 29) throw new IllegalArgumentException("Field is not accountsOnTarget");
        accountsOnTarget = (List<BootstrapAccount>) (List<?>) value;
        presentFields.add(29);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<BootstrapOptions> CREATOR = findCreator(BootstrapOptions.class);
}
