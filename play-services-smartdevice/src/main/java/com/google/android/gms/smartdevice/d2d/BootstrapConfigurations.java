/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.smartdevice.d2d;

import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.common.server.response.FastJsonResponse;
import com.google.android.gms.smartdevice.directtransfer.AccountPickerOptions;
import com.google.android.gms.smartdevice.directtransfer.UiCustomization;
import com.google.android.gms.smartdevice.utils.BinarySerializableFastSafeParcelableJson;

import org.microg.gms.common.Hide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SafeParcelable.Class
@Hide
public class BootstrapConfigurations extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> FIELD_MAPPINGS;
    static {
        Map<String, FastJsonResponse.Field<?, ?>> fields = new LinkedHashMap<>();
        fields.put("wifiNetworkSsid", new FastJsonResponse.Field<>(7, false, 7, false, "wifiNetworkSsid", 2, null));
        fields.put("wifiNetworkPassword", new FastJsonResponse.Field<>(7, false, 7, false, "wifiNetworkPassword", 3, null));
        fields.put("wifiNetworkSecurity", new FastJsonResponse.Field<>(7, false, 7, false, "wifiNetworkSecurity", 4, null));
        fields.put("isLockScreenShown", new FastJsonResponse.Field<>(6, false, 6, false, "isLockScreenShown", 5, null));
        fields.put("bootstrapAccounts", new FastJsonResponse.Field<>(11, true, 11, true, "bootstrapAccounts", 6, BootstrapAccount.class));
        fields.put("extraParameters", new FastJsonResponse.Field<>(10, false, 10, false, "extraParameters", 7, null));
        fields.put("hasUserConfirmed", new FastJsonResponse.Field<>(6, false, 6, false, "hasUserConfirmed", 8, null));
        fields.put("supportsUnencryptedCommunication", new FastJsonResponse.Field<>(6, false, 6, false, "supportsUnencryptedCommunication", 9, null));
        fields.put("maxPacketSize", new FastJsonResponse.Field<>(0, false, 0, false, "maxPacketSize", 10, null));
        fields.put("optionFlags", new FastJsonResponse.Field<>(2, false, 2, false, "optionFlags", 11, null));
        fields.put("optionFlagSetIndicators", new FastJsonResponse.Field<>(2, false, 2, false, "optionFlagSetIndicators", 12, null));
        fields.put("deviceDetails", new FastJsonResponse.Field<>(11, false, 11, false, "deviceDetails", 13, DeviceDetails.class));
        fields.put("nearbyDirectTransfer", new FastJsonResponse.Field<>(6, false, 6, false, "nearbyDirectTransfer", 14, null));
        fields.put("targetNearbyDirectTransfer", new FastJsonResponse.Field<>(6, false, 6, false, "targetNearbyDirectTransfer", 15, null));
        fields.put("hideSkipAccount", new FastJsonResponse.Field<>(6, false, 6, false, "hideSkipAccount", 16, null));
        fields.put("accountPickerEnabled", new FastJsonResponse.Field<>(6, false, 6, false, "accountPickerEnabled", 17, null));
        fields.put("accountPickerOptions", new FastJsonResponse.Field<>(11, false, 11, false, "accountPickerOptions", 18, AccountPickerOptions.class));
        fields.put("authenticatingUser", new FastJsonResponse.Field<>(11, false, 11, false, "authenticatingUser", 19, AuthenticatingUser.class));
        fields.put("unicornChallengeDeduplicationEnabled", new FastJsonResponse.Field<>(6, false, 6, false, "unicornChallengeDeduplicationEnabled", 20, null));
        fields.put("uiCustomization", new FastJsonResponse.Field<>(11, false, 11, false, "uiCustomization", 21, UiCustomization.class));
        FIELD_MAPPINGS = Collections.unmodifiableMap(fields);
    }

    @SafeParcelable.Indicator
    final Set<Integer> presentFields;
    @SafeParcelable.Field(value = 2, mayNull = true) public String wifiNetworkSsid;
    @SafeParcelable.Field(value = 3, mayNull = true) public String wifiNetworkPassword;
    @SafeParcelable.Field(value = 4, mayNull = true) public String wifiNetworkSecurity;
    @SafeParcelable.Field(5) public boolean isLockScreenShown;
    @SafeParcelable.Field(value = 6, subClass = BootstrapAccount.class, mayNull = true) public List<BootstrapAccount> bootstrapAccounts;
    @SafeParcelable.Field(value = 7, mayNull = true) public Bundle extraParameters;
    @SafeParcelable.Field(8) public boolean hasUserConfirmed;
    @SafeParcelable.Field(9) public boolean supportsUnencryptedCommunication;
    @SafeParcelable.Field(10) public int maxPacketSize;
    @SafeParcelable.Field(11) public long optionFlags;
    @SafeParcelable.Field(12) public long optionFlagSetIndicators;
    @SafeParcelable.Field(value = 13, mayNull = true) public DeviceDetails deviceDetails;
    @SafeParcelable.Field(14)
    public boolean nearbyDirectTransfer;
    @SafeParcelable.Field(15) public boolean targetNearbyDirectTransfer;
    @SafeParcelable.Field(16) public boolean hideSkipAccount;
    @SafeParcelable.Field(17) public boolean accountPickerEnabled;
    @SafeParcelable.Field(value = 18, mayNull = true) public AccountPickerOptions accountPickerOptions;
    @SafeParcelable.Field(value = 19, mayNull = true) public AuthenticatingUser authenticatingUser;
    @SafeParcelable.Field(20) public boolean unicornChallengeDeduplicationEnabled;
    @SafeParcelable.Field(value = 21, mayNull = true) public UiCustomization uiCustomization;

    public BootstrapConfigurations() { presentFields = new HashSet<>(); }

    public BootstrapConfigurations(String wifiNetworkSsid, String wifiNetworkPassword,
                                   String wifiNetworkSecurity, boolean isLockScreenShown,
                                   ArrayList<BootstrapAccount> bootstrapAccounts,
                                   Map<String, String> extraParameters, long optionFlags,
                                   long optionFlagSetIndicators) {
        this();
        this.wifiNetworkSsid = wifiNetworkSsid;
        this.wifiNetworkPassword = wifiNetworkPassword;
        this.wifiNetworkSecurity = wifiNetworkSecurity;
        this.isLockScreenShown = isLockScreenShown;
        this.bootstrapAccounts = bootstrapAccounts;
        this.hasUserConfirmed = false;
        this.supportsUnencryptedCommunication = false;
        this.maxPacketSize = 0;
        this.optionFlags = optionFlags;
        this.optionFlagSetIndicators = optionFlagSetIndicators;
        setExtraParameters(extraParameters);
        this.accountPickerEnabled = false;
        this.accountPickerOptions = null;
        this.authenticatingUser = null;
        this.unicornChallengeDeduplicationEnabled = false;
        this.uiCustomization = null;
        for (int fieldId = 2; fieldId <= 12; fieldId++) presentFields.add(fieldId);
        for (int fieldId = 17; fieldId <= 21; fieldId++) presentFields.add(fieldId);
    }

    @SafeParcelable.Constructor
    BootstrapConfigurations(@SafeParcelable.Indicator Set<Integer> presentFields,
                            @SafeParcelable.Param(2) String wifiNetworkSsid,
                            @SafeParcelable.Param(3) String wifiNetworkPassword,
                            @SafeParcelable.Param(4) String wifiNetworkSecurity,
                            @SafeParcelable.Param(5) boolean isLockScreenShown,
                            @SafeParcelable.Param(6) List<BootstrapAccount> bootstrapAccounts,
                            @SafeParcelable.Param(7) Bundle extraParameters,
                            @SafeParcelable.Param(8) boolean hasUserConfirmed,
                            @SafeParcelable.Param(9) boolean supportsUnencryptedCommunication,
                            @SafeParcelable.Param(10) int maxPacketSize,
                            @SafeParcelable.Param(11) long optionFlags,
                            @SafeParcelable.Param(12) long optionFlagSetIndicators,
                            @SafeParcelable.Param(13) DeviceDetails deviceDetails,
                            @SafeParcelable.Param(14) boolean nearbyDirectTransfer,
                            @SafeParcelable.Param(15) boolean targetNearbyDirectTransfer,
                            @SafeParcelable.Param(16) boolean hideSkipAccount,
                            @SafeParcelable.Param(17) boolean accountPickerEnabled,
                            @SafeParcelable.Param(18) AccountPickerOptions accountPickerOptions,
                            @SafeParcelable.Param(19) AuthenticatingUser authenticatingUser,
                            @SafeParcelable.Param(20) boolean unicornChallengeDeduplicationEnabled,
                            @SafeParcelable.Param(21) UiCustomization uiCustomization) {
        this.presentFields = presentFields;
        this.wifiNetworkSsid = wifiNetworkSsid;
        this.wifiNetworkPassword = wifiNetworkPassword;
        this.wifiNetworkSecurity = wifiNetworkSecurity;
        this.isLockScreenShown = isLockScreenShown;
        this.bootstrapAccounts = bootstrapAccounts;
        this.extraParameters = extraParameters;
        this.hasUserConfirmed = hasUserConfirmed;
        this.supportsUnencryptedCommunication = supportsUnencryptedCommunication;
        this.maxPacketSize = maxPacketSize;
        this.optionFlags = optionFlags;
        this.optionFlagSetIndicators = optionFlagSetIndicators;
        this.deviceDetails = deviceDetails;
        this.nearbyDirectTransfer = nearbyDirectTransfer;
        this.targetNearbyDirectTransfer = targetNearbyDirectTransfer;
        this.hideSkipAccount = hideSkipAccount;
        this.accountPickerEnabled = accountPickerEnabled;
        this.accountPickerOptions = accountPickerOptions;
        this.authenticatingUser = authenticatingUser;
        this.unicornChallengeDeduplicationEnabled = unicornChallengeDeduplicationEnabled;
        this.uiCustomization = uiCustomization;
    }

    @Override public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() { return FIELD_MAPPINGS; }

    @Override
    protected Object getFieldValue(FastJsonResponse.Field<?, ?> field) {
        switch (field.safeParcelFieldId) {
            case 2: return wifiNetworkSsid;
            case 3: return wifiNetworkPassword;
            case 4: return wifiNetworkSecurity;
            case 5: return isLockScreenShown;
            case 6: return bootstrapAccounts;
            case 7: return getExtraParameters();
            case 8: return hasUserConfirmed;
            case 9: return supportsUnencryptedCommunication;
            case 10: return maxPacketSize;
            case 11: return optionFlags;
            case 12: return optionFlagSetIndicators;
            case 13: return deviceDetails;
            case 14: return nearbyDirectTransfer;
            case 15: return targetNearbyDirectTransfer;
            case 16: return hideSkipAccount;
            case 17: return accountPickerEnabled;
            case 18: return accountPickerOptions;
            case 19: return authenticatingUser;
            case 20: return unicornChallengeDeduplicationEnabled;
            case 21: return uiCustomization;
            default: throw new IllegalStateException("Unknown SafeParcelable id=" + field.safeParcelFieldId);
        }
    }

    @Override protected boolean isFieldSet(FastJsonResponse.Field<?, ?> field) { return presentFields.contains(field.safeParcelFieldId); }

    @Override
    protected void setString(FastJsonResponse.Field<?, ?> field, String value) {
        switch (field.safeParcelFieldId) {
            case 2: wifiNetworkSsid = value; break;
            case 3: wifiNetworkPassword = value; break;
            case 4: wifiNetworkSecurity = value; break;
            default: throw new IllegalArgumentException("Field is not a string: " + field.outputFieldName);
        }
        presentFields.add(field.safeParcelFieldId);
    }

    @Override
    protected void setBoolean(FastJsonResponse.Field<?, ?> field, boolean value) {
        switch (field.safeParcelFieldId) {
            case 5: isLockScreenShown = value; break;
            case 8: hasUserConfirmed = value; break;
            case 9: supportsUnencryptedCommunication = value; break;
            case 14: nearbyDirectTransfer = value; break;
            case 15: targetNearbyDirectTransfer = value; break;
            case 16: hideSkipAccount = value; break;
            case 17: accountPickerEnabled = value; break;
            case 20: unicornChallengeDeduplicationEnabled = value; break;
            default: throw new IllegalArgumentException("Field is not a boolean: " + field.outputFieldName);
        }
        presentFields.add(field.safeParcelFieldId);
    }

    @Override protected void setInteger(FastJsonResponse.Field<?, ?> field, int value) {
        if (field.safeParcelFieldId != 10) throw new IllegalArgumentException("Field is not maxPacketSize");
        maxPacketSize = value;
        presentFields.add(10);
    }

    @Override protected void setLong(FastJsonResponse.Field<?, ?> field, long value) {
        if (field.safeParcelFieldId == 11) optionFlags = value;
        else if (field.safeParcelFieldId == 12) optionFlagSetIndicators = value;
        else throw new IllegalArgumentException("Field is not a long: " + field.outputFieldName);
        presentFields.add(field.safeParcelFieldId);
    }

    @Override protected void setStringMap(FastJsonResponse.Field<?, ?> field, Map<String, String> value) {
        if (field.safeParcelFieldId != 7) throw new IllegalArgumentException("Field is not extraParameters");
        setExtraParameters(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void addConcreteTypeArray(FastJsonResponse.Field<?, ?> field, List<? extends FastJsonResponse> value) {
        if (field.safeParcelFieldId != 6) throw new IllegalArgumentException("Field is not bootstrapAccounts");
        bootstrapAccounts = (List<BootstrapAccount>) value;
        presentFields.add(6);
    }

    @Override protected void addConcreteType(FastJsonResponse.Field<?, ?> field, FastJsonResponse value) {
        switch (field.safeParcelFieldId) {
            case 13: deviceDetails = (DeviceDetails) value; break;
            case 18: accountPickerOptions = (AccountPickerOptions) value; break;
            case 19: authenticatingUser = (AuthenticatingUser) value; break;
            case 21: uiCustomization = (UiCustomization) value; break;
            default: throw new IllegalArgumentException("Field is not a concrete type: " + field.outputFieldName);
        }
        presentFields.add(field.safeParcelFieldId);
    }

    public Map<String, String> getExtraParameters() {
        if (extraParameters == null) return new HashMap<>();
        Map<String, String> result = new HashMap<>();
        for (String key : extraParameters.keySet()) {
            String value = extraParameters.getString(key);
            if (value != null) result.put(key, value);
        }
        return result;
    }

    public void setExtraParameters(Map<String, String> value) {
        if (value == null) {
            extraParameters = null;
        } else {
            Bundle bundle = new Bundle();
            for (Map.Entry<String, String> entry : value.entrySet()) bundle.putString(entry.getKey(), entry.getValue());
            extraParameters = bundle;
        }
        presentFields.add(7);
    }

    public void setBootstrapAccounts(List<BootstrapAccount> value) { bootstrapAccounts = value; presentFields.add(6); }
    public void setLockScreenShown(boolean value) { isLockScreenShown = value; presentFields.add(5); }
    public void setHasUserConfirmed(boolean value) { hasUserConfirmed = value; presentFields.add(8); }
    public void setSupportsUnencryptedCommunication(boolean value) { supportsUnencryptedCommunication = value; presentFields.add(9); }
    public void setHideSkipAccount(boolean value) { hideSkipAccount = value; presentFields.add(16); }
    public void setAccountPickerEnabled(boolean value) { accountPickerEnabled = value; presentFields.add(17); }
    public void setAccountPickerOptions(AccountPickerOptions value) { accountPickerOptions = value; presentFields.add(18); }
    public void setDeviceDetails(DeviceDetails value) { deviceDetails = value; presentFields.add(13); }
    public void setAuthenticatingUser(AuthenticatingUser value) { authenticatingUser = value; presentFields.add(19); }
    public void setMaxPacketSize(int value) { maxPacketSize = value; presentFields.add(10); }
    public void setUnicornChallengeDeduplicationEnabled(boolean value) { unicornChallengeDeduplicationEnabled = value; presentFields.add(20); }
    public void setUiCustomization(UiCustomization value) { uiCustomization = value; presentFields.add(21); }

    /**
     * Returns the target-facing DirectTransfer configuration.
     */
    @NonNull
    public BootstrapConfigurations copyForDirectTransferWire() {
        Set<Integer> wireFields = new HashSet<>(presentFields);
        wireFields.remove(6);  // bootstrapAccounts
        wireFields.remove(7);  // extraParameters
        wireFields.remove(17); // accountPickerEnabled
        wireFields.remove(18); // accountPickerOptions
        wireFields.remove(21); // uiCustomization
        return new BootstrapConfigurations(
                wireFields,
                wifiNetworkSsid,
                wifiNetworkPassword,
                wifiNetworkSecurity,
                isLockScreenShown,
                null,
                null,
                hasUserConfirmed,
                supportsUnencryptedCommunication,
                maxPacketSize,
                optionFlags,
                optionFlagSetIndicators,
                deviceDetails,
                nearbyDirectTransfer,
                targetNearbyDirectTransfer,
                hideSkipAccount,
                false,
                null,
                authenticatingUser,
                unicornChallengeDeduplicationEnabled,
                null
        );
    }

    @Override public void writeToParcel(@NonNull Parcel dest, int flags) { CREATOR.writeToParcel(this, dest, flags); }

    public static final SafeParcelableCreatorAndWriter<BootstrapConfigurations> CREATOR = findCreator(BootstrapConfigurations.class);
}
