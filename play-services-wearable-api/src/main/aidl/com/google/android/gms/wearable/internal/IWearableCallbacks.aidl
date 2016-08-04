package com.google.android.gms.wearable.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.wearable.internal.DeleteDataItemsResponse;
import com.google.android.gms.wearable.internal.GetCloudSyncSettingResponse;
import com.google.android.gms.wearable.internal.GetConfigResponse;
import com.google.android.gms.wearable.internal.GetConfigsResponse;
import com.google.android.gms.wearable.internal.GetConnectedNodesResponse;
import com.google.android.gms.wearable.internal.GetDataItemResponse;
import com.google.android.gms.wearable.internal.GetFdForAssetResponse;
import com.google.android.gms.wearable.internal.GetLocalNodeResponse;
import com.google.android.gms.wearable.internal.PutDataResponse;
import com.google.android.gms.wearable.internal.SendMessageResponse;
import com.google.android.gms.wearable.internal.StorageInfoResponse;

interface IWearableCallbacks {
    void onGetConfigResponse(in GetConfigResponse response) = 1;
    void onPutDataResponse(in PutDataResponse response) = 2;
    void onGetDataItemResponse(in GetDataItemResponse response) = 3;
    void onDataItemChanged(in DataHolder dataHolder) = 4;
    void onDeleteDataItemsResponse(in DeleteDataItemsResponse response) = 5;
    void onSendMessageResponse(in SendMessageResponse response) = 6;
    void onGetFdForAssetResponse(in GetFdForAssetResponse response) = 7;
    void onGetLocalNodeResponse(in GetLocalNodeResponse response) = 8;
    void onGetConnectedNodesResponse(in GetConnectedNodesResponse response) = 9;
    void onStatus(in Status status) = 10;
    void onStorageInfoResponse(in StorageInfoResponse response) = 11;
    void onGetConfigsResponse(in GetConfigsResponse response) = 12;

    void onGetCloudSyncSettingResponse(in GetCloudSyncSettingResponse response) = 28;
}
