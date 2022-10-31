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
}
