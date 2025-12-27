package com.google.android.gms.wearable.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.wearable.internal.AddLocalCapabilityResponse;
import com.google.android.gms.wearable.internal.ChannelReceiveFileResponse;
import com.google.android.gms.wearable.internal.ChannelSendFileResponse;
import com.google.android.gms.wearable.internal.CloseChannelResponse;
import com.google.android.gms.wearable.internal.DeleteDataItemsResponse;
import com.google.android.gms.wearable.internal.GetAllCapabilitiesResponse;
import com.google.android.gms.wearable.internal.GetCapabilityResponse;
import com.google.android.gms.wearable.internal.GetChannelInputStreamResponse;
import com.google.android.gms.wearable.internal.GetChannelOutputStreamResponse;
import com.google.android.gms.wearable.internal.GetCloudSyncOptInOutDoneResponse;
import com.google.android.gms.wearable.internal.GetCloudSyncOptInStatusResponse;
import com.google.android.gms.wearable.internal.GetCloudSyncSettingResponse;
import com.google.android.gms.wearable.internal.GetConfigResponse;
import com.google.android.gms.wearable.internal.GetConfigsResponse;
import com.google.android.gms.wearable.internal.GetConnectedNodesResponse;
import com.google.android.gms.wearable.internal.GetDataItemResponse;
import com.google.android.gms.wearable.internal.GetFdForAssetResponse;
import com.google.android.gms.wearable.internal.GetLocalNodeResponse;
import com.google.android.gms.wearable.internal.OpenChannelResponse;
import com.google.android.gms.wearable.internal.PutDataResponse;
import com.google.android.gms.wearable.internal.RemoveLocalCapabilityResponse;
import com.google.android.gms.wearable.internal.SendMessageResponse;
import com.google.android.gms.wearable.internal.StorageInfoResponse;

import com.google.android.gms.wearable.internal.AcceptTermsRequest;
import com.google.android.gms.wearable.internal.ConsentResponse;

interface IWearableCallbacks {
    // Config
    void onGetConfigResponse(in GetConfigResponse response) = 1;
    void onGetConfigsResponse(in GetConfigsResponse response) = 12;

    // Cloud Sync
    void onGetCloudSyncOptInOutDoneResponse(in GetCloudSyncOptInOutDoneResponse response) = 27;
    void onGetCloudSyncSettingResponse(in GetCloudSyncSettingResponse response) = 28;
    void onGetCloudSyncOptInStatusResponse(in GetCloudSyncOptInStatusResponse response) = 29;

    // Data
    void onPutDataResponse(in PutDataResponse response) = 2;
    void onGetDataItemResponse(in GetDataItemResponse response) = 3;
    void onDataItemChanged(in DataHolder dataHolder) = 4;
    void onDeleteDataItemsResponse(in DeleteDataItemsResponse response) = 5;
    void onSendMessageResponse(in SendMessageResponse response) = 6;
    void onGetFdForAssetResponse(in GetFdForAssetResponse response) = 7;
    void onGetLocalNodeResponse(in GetLocalNodeResponse response) = 8;
    void onGetConnectedNodesResponse(in GetConnectedNodesResponse response) = 9;

    // Channels
    void onOpenChannelResponse(in OpenChannelResponse response) = 13;
    void onCloseChannelResponse(in CloseChannelResponse response) = 14;
    void onCloseChannelResponse2(in CloseChannelResponse response) = 15; // found two entries in google gms
    void onGetChannelInputStreamResponse(in GetChannelInputStreamResponse response) = 16;
    void onGetChannelOutputStreamResponse(in GetChannelOutputStreamResponse response) = 17;
    void onChannelReceiveFileResponse(in ChannelReceiveFileResponse response) = 18;
    void onChannelSendFileResponse(in ChannelSendFileResponse response) = 19;

    void onStatus(in Status status) = 10;
    void onStorageInfoResponse(in StorageInfoResponse response) = 11;

    // Capabilities
    void onGetCapabilityResponse(in GetCapabilityResponse response) = 21;
    void onGetAllCapabilitiesResponse(in GetAllCapabilitiesResponse response) = 22;
    void onAddLocalCapabilityResponse(in AddLocalCapabilityResponse response) = 25;
    void onRemoveLocalCapabilityResponse(in RemoveLocalCapabilityResponse response) = 26;

    // Terms of service
    void onGetTermsResponse(in GetTermsResponse response) = 48;
    void onConsentResponse(in ConsentResponse response) = 37;

    // Fastpair
    void onGetFastpairAccountKeyByAccountResponse(in GetFastpairAccountKeyByAccountResponse response) = 49;
    void onGetFastpairAccountKeysResponse(in GetFastpairAccountKeysResponse response) = 47;

    // Uncategorized
    void onGetRestoreStateResponse(in GetRestoreStateResponse response) = 46;
    void onBooleanResponse(in BooleanResponse response) = 45;
    void onGetCompanionPackageForNodeResponse(in GetCompanionPackageForNodeResponse response) = 36;
    void onRpcResponse(in RpcResponse response) = 33;
    void onGetEapIdResponse(in GetEapIdResponse response) = 34;
    void onPerformEapAkaResponse(in PerformEapAkaResponse response) = 35;
    void onGetNodeIdResponse(in GetNodeIdResponse response) = 38;
    void onAppRecommendationsResponse(in AppRecommendationsResponse response) = 39;
    void onGetAppThemeResponse(in GetAppThemeResponse response) = 40;
    void onGetBackupSettingsSupportedResponse(in GetBackupSettingsSupportedResponse response) = 41;
    void onGetRestoreSupportedResponse(in GetRestoreSupportedResponse response) = 42;

}
