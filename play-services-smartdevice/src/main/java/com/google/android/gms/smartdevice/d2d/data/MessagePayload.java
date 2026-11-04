/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.smartdevice.d2d.data;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.common.server.response.FastJsonResponse;
import com.google.android.gms.smartdevice.d2d.AccountTransferResult;
import com.google.android.gms.smartdevice.d2d.BootstrapConfigurations;
import com.google.android.gms.smartdevice.d2d.BootstrapOptions;
import com.google.android.gms.smartdevice.d2d.EsimActivationPayload;
import com.google.android.gms.smartdevice.d2d.PostTransferAction;
import com.google.android.gms.smartdevice.quickstart.QuickStartHandshakePayload;
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
public class MessagePayload extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> FIELD_MAPPINGS;

    static {
        Map<String, FastJsonResponse.Field<?, ?>> fields = new LinkedHashMap<>();
        fields.put("protocolVersion", new FastJsonResponse.Field<>(0, false, 0, false, "protocolVersion", 7, null));
        fields.put("bootstrapState", new FastJsonResponse.Field<>(0, false, 0, false, "bootstrapState", 2, null));
        fields.put("bootstrapOptions", new FastJsonResponse.Field<>(11, false, 11, false, "bootstrapOptions", 3, BootstrapOptions.class));
        fields.put("bootstrapConfigurations", new FastJsonResponse.Field<>(11, false, 11, false, "bootstrapConfigurations", 4, BootstrapConfigurations.class));
        fields.put("displayText", new FastJsonResponse.Field<>(11, false, 11, false, "displayText", 5, DisplayText.class));
        fields.put("accountBootstrapPayload", new FastJsonResponse.Field<>(11, false, 11, false, "accountBootstrapPayload", 6, AccountBootstrapPayload.class));
        fields.put("progressEvent", new FastJsonResponse.Field<>(11, false, 11, false, "progressEvent", 8, ProgressEvent.class));
        fields.put("priorityMessage", new FastJsonResponse.Field<>(6, false, 6, false, "priorityMessage", 9, null));
        fields.put("accountTransferResults", new FastJsonResponse.Field<>(11, true, 11, true, "accountTransferResults", 10, AccountTransferResult.class));
        fields.put("accountTransferMsg", new FastJsonResponse.Field<>(11, false, 11, false, "accountTransferMsg", 11, AccountTransferPayload.class));
        fields.put("deviceStatus", new FastJsonResponse.Field<>(11, false, 11, false, "deviceStatus", 12, DeviceStatus.class));
        fields.put("blockstorePayload", new FastJsonResponse.Field<>(11, false, 11, false, "blockstorePayload", 15, BlockstorePayload.class));
        fields.put("secondDeviceAuthPayload", new FastJsonResponse.Field<>(11, false, 11, false, "secondDeviceAuthPayload", 16, SecondDeviceAuthPayload.class));
        fields.put("quickStartPayload", new FastJsonResponse.Field<>(8, false, 8, false, "quickStartPayload", 18, null));
        fields.put("PostTransferAction", new FastJsonResponse.Field<>(11, false, 11, false, "PostTransferAction", 21, PostTransferAction.class));
        FIELD_MAPPINGS = Collections.unmodifiableMap(fields);
    }

    @SafeParcelable.Indicator
    final Set<Integer> presentFields = new HashSet<>();
    private boolean jsonFieldParsed;
    private boolean protocolVersionParsed;
    @SafeParcelable.Field(value = 1)
    public int legacyMarker = 3;
    @SafeParcelable.Field(value = 2)
    public int bootstrapState;
    @SafeParcelable.Field(value = 3, mayNull = true)
    public BootstrapOptions bootstrapOptions;
    @SafeParcelable.Field(value = 4, mayNull = true)
    public BootstrapConfigurations bootstrapConfigurations;
    @SafeParcelable.Field(value = 5, mayNull = true)
    public DisplayText displayText;
    @SafeParcelable.Field(value = 6, mayNull = true)
    public AccountBootstrapPayload accountBootstrapPayload;
    @SafeParcelable.Field(value = 7, getterName = "getEffectiveProtocolVersion")
    public int protocolVersion = 3;
    @SafeParcelable.Field(value = 8, mayNull = true)
    public ProgressEvent progressEvent;
    @SafeParcelable.Field(value = 9)
    public boolean priorityMessage;
    @SafeParcelable.Field(value = 10, subClass = AccountTransferResult.class, mayNull = true)
    public List<AccountTransferResult> accountTransferResults;
    @SafeParcelable.Field(value = 11, mayNull = true)
    public AccountTransferPayload accountTransferMsg;
    @SafeParcelable.Field(value = 12, mayNull = true)
    public DeviceStatus deviceStatus;
    @SafeParcelable.Field(value = 13, mayNull = true)
    public WorkProfilePayload workProfilePayload;
    @SafeParcelable.Field(value = 14, subClass = EsimActivationPayload.class, mayNull = true)
    public List<EsimActivationPayload> esimActivationPayloads;
    @SafeParcelable.Field(value = 15, mayNull = true)
    public BlockstorePayload blockstorePayload;
    @SafeParcelable.Field(value = 16, mayNull = true)
    public SecondDeviceAuthPayload secondDeviceAuthPayload;
    @SafeParcelable.Field(value = 17, subClass = FolsomDataPayload.class, mayNull = true)
    public List<FolsomDataPayload> folsomDataPayload;
    @SafeParcelable.Field(value = 18, mayNull = true)
    public byte[] quickStartPayload;
    @SafeParcelable.Field(value = 19, mayNull = true)
    public StartServicePayload startServicePayload;
    @SafeParcelable.Field(value = 20, mayNull = true)
    public QuickStartHandshakePayload quickStartHandshakePayload;
    @SafeParcelable.Field(value = 21, mayNull = true)
    public PostTransferAction postTransferAction;

    public MessagePayload() {
        presentFields.add(7);
    }

    public MessagePayload(@SafeParcelable.Param(1) int legacyMarker, @SafeParcelable.Param(2) int bootstrapState,
                          @SafeParcelable.Param(3) BootstrapOptions bootstrapOptions,
                          @SafeParcelable.Param(4) BootstrapConfigurations bootstrapConfigurations,
                          @SafeParcelable.Param(5) DisplayText displayText,
                          @SafeParcelable.Param(6) AccountBootstrapPayload accountBootstrapPayload,
                          @SafeParcelable.Param(7) int protocolVersion,
                          @SafeParcelable.Param(8) ProgressEvent progressEvent,
                          @SafeParcelable.Param(9) boolean priorityMessage,
                          @SafeParcelable.Param(10) List<AccountTransferResult> accountTransferResults,
                          @SafeParcelable.Param(11) AccountTransferPayload accountTransferMsg,
                          @SafeParcelable.Param(12) DeviceStatus deviceStatus,
                          @SafeParcelable.Param(13) WorkProfilePayload workProfilePayload,
                          @SafeParcelable.Param(14) List<EsimActivationPayload> esimActivationPayloads,
                          @SafeParcelable.Param(15) BlockstorePayload blockstorePayload,
                          @SafeParcelable.Param(16) SecondDeviceAuthPayload secondDeviceAuthPayload,
                          @SafeParcelable.Param(17) List<FolsomDataPayload> folsomDataPayload,
                          @SafeParcelable.Param(18) byte[] quickStartPayload,
                          @SafeParcelable.Param(19) StartServicePayload startServicePayload,
                          @SafeParcelable.Param(20) QuickStartHandshakePayload quickStartHandshakePayload,
                          @SafeParcelable.Param(21) PostTransferAction postTransferAction) {
        this.legacyMarker = legacyMarker;
        this.bootstrapState = bootstrapState;
        this.bootstrapOptions = bootstrapOptions;
        this.bootstrapConfigurations = bootstrapConfigurations;
        this.displayText = displayText;
        this.accountBootstrapPayload = accountBootstrapPayload;
        this.protocolVersion = protocolVersion;
        this.progressEvent = progressEvent;
        this.priorityMessage = priorityMessage;
        this.accountTransferResults = accountTransferResults;
        this.accountTransferMsg = accountTransferMsg;
        this.deviceStatus = deviceStatus;
        this.workProfilePayload = workProfilePayload;
        this.esimActivationPayloads = esimActivationPayloads;
        this.blockstorePayload = blockstorePayload;
        this.secondDeviceAuthPayload = secondDeviceAuthPayload;
        this.folsomDataPayload = folsomDataPayload;
        this.quickStartPayload = quickStartPayload;
        this.startServicePayload = startServicePayload;
        this.quickStartHandshakePayload = quickStartHandshakePayload;
        this.postTransferAction = postTransferAction;
        presentFields.add(7);
        if (bootstrapState != 0) presentFields.add(2);
        if (bootstrapOptions != null) presentFields.add(3);
        if (bootstrapConfigurations != null) presentFields.add(4);
        if (displayText != null) presentFields.add(5);
        if (accountBootstrapPayload != null) presentFields.add(6);
        if (progressEvent != null) presentFields.add(8);
        if (priorityMessage) presentFields.add(9);
        if (accountTransferResults != null) presentFields.add(10);
        if (accountTransferMsg != null) presentFields.add(11);
        if (deviceStatus != null) presentFields.add(12);
        if (workProfilePayload != null) presentFields.add(13);
        if (esimActivationPayloads != null) presentFields.add(14);
        if (blockstorePayload != null) presentFields.add(15);
        if (secondDeviceAuthPayload != null) presentFields.add(16);
        if (folsomDataPayload != null) presentFields.add(17);
        if (quickStartPayload != null) presentFields.add(18);
        if (startServicePayload != null) presentFields.add(19);
        if (quickStartHandshakePayload != null) presentFields.add(20);
        if (postTransferAction != null) presentFields.add(21);
    }

    @SafeParcelable.Constructor
    MessagePayload(@SafeParcelable.Indicator Set<Integer> presentFields,
                   @SafeParcelable.Param(1) int legacyMarker,
                   @SafeParcelable.Param(2) int bootstrapState,
                   @SafeParcelable.Param(3) BootstrapOptions bootstrapOptions,
                   @SafeParcelable.Param(4) BootstrapConfigurations bootstrapConfigurations,
                   @SafeParcelable.Param(5) DisplayText displayText,
                   @SafeParcelable.Param(6) AccountBootstrapPayload accountBootstrapPayload,
                   @SafeParcelable.Param(7) int protocolVersion,
                   @SafeParcelable.Param(8) ProgressEvent progressEvent,
                   @SafeParcelable.Param(9) boolean priorityMessage,
                   @SafeParcelable.Param(10) List<AccountTransferResult> accountTransferResults,
                   @SafeParcelable.Param(11) AccountTransferPayload accountTransferMsg,
                   @SafeParcelable.Param(12) DeviceStatus deviceStatus,
                   @SafeParcelable.Param(13) WorkProfilePayload workProfilePayload,
                   @SafeParcelable.Param(14) List<EsimActivationPayload> esimActivationPayloads,
                   @SafeParcelable.Param(15) BlockstorePayload blockstorePayload,
                   @SafeParcelable.Param(16) SecondDeviceAuthPayload secondDeviceAuthPayload,
                   @SafeParcelable.Param(17) List<FolsomDataPayload> folsomDataPayload,
                   @SafeParcelable.Param(18) byte[] quickStartPayload,
                   @SafeParcelable.Param(19) StartServicePayload startServicePayload,
                   @SafeParcelable.Param(20) QuickStartHandshakePayload quickStartHandshakePayload,
                   @SafeParcelable.Param(21) PostTransferAction postTransferAction) {
        this(legacyMarker, bootstrapState, bootstrapOptions, bootstrapConfigurations, displayText,
                accountBootstrapPayload, protocolVersion, progressEvent, priorityMessage,
                accountTransferResults, accountTransferMsg, deviceStatus, workProfilePayload,
                esimActivationPayloads, blockstorePayload, secondDeviceAuthPayload,
                folsomDataPayload, quickStartPayload, startServicePayload,
                quickStartHandshakePayload, postTransferAction);
        this.presentFields.clear();
        this.presentFields.addAll(presentFields);
    }

    @Override
    public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() {
        return FIELD_MAPPINGS;
    }

    @Override
    protected Object getFieldValue(FastJsonResponse.Field<?, ?> field) {
        switch (field.safeParcelFieldId) {
            case 2: return bootstrapState;
            case 3: return bootstrapOptions;
            case 4: return bootstrapConfigurations;
            case 5: return displayText;
            case 6: return accountBootstrapPayload;
            case 7: return getEffectiveProtocolVersion();
            case 8: return progressEvent;
            case 9: return priorityMessage;
            case 10: return accountTransferResults;
            case 11: return accountTransferMsg;
            case 12: return deviceStatus;
            case 13: return workProfilePayload;
            case 14: return esimActivationPayloads;
            case 15: return blockstorePayload;
            case 16: return secondDeviceAuthPayload;
            case 17: return folsomDataPayload;
            case 18: return quickStartPayload;
            case 19: return startServicePayload;
            case 20: return quickStartHandshakePayload;
            case 21: return postTransferAction;
            default: throw new IllegalStateException("Unknown SafeParcelable id=" + field.safeParcelFieldId);
        }
    }

    @Override
    protected boolean isFieldSet(FastJsonResponse.Field<?, ?> field) {
        return presentFields.contains(field.safeParcelFieldId);
    }

    public int getEffectiveProtocolVersion() {
        return !jsonFieldParsed || protocolVersionParsed ? protocolVersion : 0;
    }

    public void setBootstrapState(int value) { bootstrapState = value; presentFields.add(2); }
    public void setBootstrapOptions(BootstrapOptions value) { bootstrapOptions = value; presentFields.add(3); }
    public void setBootstrapConfigurations(BootstrapConfigurations value) { bootstrapConfigurations = value; presentFields.add(4); }
    public void setDisplayText(DisplayText value) { displayText = value; presentFields.add(5); }
    public void setAccountBootstrapPayload(AccountBootstrapPayload value) { accountBootstrapPayload = value; presentFields.add(6); }
    public void setProtocolVersion(int value) { protocolVersion = value; presentFields.add(7); }
    public void setProgressEvent(ProgressEvent value) { progressEvent = value; presentFields.add(8); }
    public void setPriorityMessage(boolean value) { priorityMessage = value; presentFields.add(9); }
    public void setAccountTransferResults(List<AccountTransferResult> value) { accountTransferResults = value; presentFields.add(10); }
    public void setAccountTransferMsg(AccountTransferPayload value) { accountTransferMsg = value; presentFields.add(11); }
    public void setDeviceStatus(DeviceStatus value) { deviceStatus = value; presentFields.add(12); }
    public void setBlockstorePayload(BlockstorePayload value) { blockstorePayload = value; presentFields.add(15); }
    public void setSecondDeviceAuthPayload(SecondDeviceAuthPayload value) { secondDeviceAuthPayload = value; presentFields.add(16); }
    public void setQuickStartPayload(byte[] value) { quickStartPayload = value; presentFields.add(18); }
    public void setPostTransferAction(PostTransferAction value) { postTransferAction = value; presentFields.add(21); }

    @Override protected void setInteger(FastJsonResponse.Field<?, ?> field, int value) {
        jsonFieldParsed = true;
        if (field.safeParcelFieldId == 2) setBootstrapState(value);
        else if (field.safeParcelFieldId == 7) {
            protocolVersionParsed = true;
            setProtocolVersion(value);
        }
        else throw new IllegalArgumentException("Unsupported integer field: " + field.outputFieldName);
    }

    @Override protected void setBoolean(FastJsonResponse.Field<?, ?> field, boolean value) {
        jsonFieldParsed = true;
        if (field.safeParcelFieldId != 9) throw new IllegalArgumentException("Unsupported boolean field: " + field.outputFieldName);
        setPriorityMessage(value);
    }

    @Override protected void setDecodedBytes(FastJsonResponse.Field<?, ?> field, byte[] value) {
        jsonFieldParsed = true;
        if (field.safeParcelFieldId != 18) throw new IllegalArgumentException("Unsupported bytes field: " + field.outputFieldName);
        setQuickStartPayload(value);
    }

    @Override protected void addConcreteType(FastJsonResponse.Field<?, ?> field, FastJsonResponse value) {
        jsonFieldParsed = true;
        switch (field.safeParcelFieldId) {
            case 3: setBootstrapOptions((BootstrapOptions) value); break;
            case 4: setBootstrapConfigurations((BootstrapConfigurations) value); break;
            case 5: setDisplayText((DisplayText) value); break;
            case 6: setAccountBootstrapPayload((AccountBootstrapPayload) value); break;
            case 8: setProgressEvent((ProgressEvent) value); break;
            case 11: setAccountTransferMsg((AccountTransferPayload) value); break;
            case 12: setDeviceStatus((DeviceStatus) value); break;
            case 15: setBlockstorePayload((BlockstorePayload) value); break;
            case 16: setSecondDeviceAuthPayload((SecondDeviceAuthPayload) value); break;
            case 21: setPostTransferAction((PostTransferAction) value); break;
            default: throw new IllegalArgumentException("Unsupported concrete field: " + field.outputFieldName);
        }
    }

    @Override @SuppressWarnings("unchecked")
    protected void addConcreteTypeArray(FastJsonResponse.Field<?, ?> field, List<? extends FastJsonResponse> value) {
        jsonFieldParsed = true;
        if (field.safeParcelFieldId != 10) throw new IllegalArgumentException("Unsupported concrete array: " + field.outputFieldName);
        setAccountTransferResults((List<AccountTransferResult>) value);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<MessagePayload> CREATOR = findCreator(MessagePayload.class);
}
