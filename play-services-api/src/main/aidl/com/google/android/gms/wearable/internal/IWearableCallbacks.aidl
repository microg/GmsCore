package com.google.android.gms.wearable.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.wearable.DeleteDataItemsResponse;
import com.google.android.gms.wearable.GetConfigResponse;
import com.google.android.gms.wearable.GetConfigsResponse;
import com.google.android.gms.wearable.GetConnectedNodesResponse;
import com.google.android.gms.wearable.GetDataItemResponse;
import com.google.android.gms.wearable.GetFdForAssetResponse;
import com.google.android.gms.wearable.GetLocalNodeResponse;
import com.google.android.gms.wearable.PutDataResponse;
import com.google.android.gms.wearable.SendMessageResponse;
import com.google.android.gms.wearable.StorageInfoResponse;

interface IWearableCallbacks {
    void onGetConfigResponse(in GetConfigResponse response) = 1;
    void onPutDataResponse(in PutDataResponse response) = 2;
    void onGetDataItemResponse(in GetDataItemResponse response) = 3;
    void onDataHolder(in DataHolder dataHolder) = 4;
    void onDeleteDataItemsResponse(in DeleteDataItemsResponse response) = 5;
    void onSendMessageResponse(in SendMessageResponse response) = 6;
    void onGetFdForAssetResponse(in GetFdForAssetResponse response) = 7;
    void onGetLocalNodeResponse(in GetLocalNodeResponse response) = 8;
    void onGetConnectedNodesResponse(in GetConnectedNodesResponse response) = 9;
    void onStatus(in Status status) = 10;
    void onStorageInfoResponse(in StorageInfoResponse response) = 11;
    void onGetConfigsResponse(in GetConfigsResponse response) = 12;
}
