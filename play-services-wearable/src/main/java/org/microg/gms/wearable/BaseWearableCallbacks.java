/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.wearable;

import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.wearable.internal.AddLocalCapabilityResponse;
import com.google.android.gms.wearable.internal.AppRecommendationsResponse;
import com.google.android.gms.wearable.internal.BooleanResponse;
import com.google.android.gms.wearable.internal.ChannelReceiveFileResponse;
import com.google.android.gms.wearable.internal.ChannelSendFileResponse;
import com.google.android.gms.wearable.internal.CloseChannelResponse;
import com.google.android.gms.wearable.internal.ConsentResponse;
import com.google.android.gms.wearable.internal.DeleteDataItemsResponse;
import com.google.android.gms.wearable.internal.GetAllCapabilitiesResponse;
import com.google.android.gms.wearable.internal.GetAppThemeResponse;
import com.google.android.gms.wearable.internal.GetBackupSettingsSupportedResponse;
import com.google.android.gms.wearable.internal.GetCapabilityResponse;
import com.google.android.gms.wearable.internal.GetChannelInputStreamResponse;
import com.google.android.gms.wearable.internal.GetChannelOutputStreamResponse;
import com.google.android.gms.wearable.internal.GetCloudSyncOptInOutDoneResponse;
import com.google.android.gms.wearable.internal.GetCloudSyncOptInStatusResponse;
import com.google.android.gms.wearable.internal.GetCloudSyncSettingResponse;
import com.google.android.gms.wearable.internal.GetCompanionPackageForNodeResponse;
import com.google.android.gms.wearable.internal.GetConfigResponse;
import com.google.android.gms.wearable.internal.GetConfigsResponse;
import com.google.android.gms.wearable.internal.GetConnectedNodesResponse;
import com.google.android.gms.wearable.internal.GetDataItemResponse;
import com.google.android.gms.wearable.internal.GetEapIdResponse;
import com.google.android.gms.wearable.internal.GetFastpairAccountKeyByAccountResponse;
import com.google.android.gms.wearable.internal.GetFastpairAccountKeysResponse;
import com.google.android.gms.wearable.internal.GetFdForAssetResponse;
import com.google.android.gms.wearable.internal.GetLocalNodeResponse;
import com.google.android.gms.wearable.internal.GetNodeIdResponse;
import com.google.android.gms.wearable.internal.GetRestoreStateResponse;
import com.google.android.gms.wearable.internal.GetRestoreSupportedResponse;
import com.google.android.gms.wearable.internal.GetTermsResponse;
import com.google.android.gms.wearable.internal.IWearableCallbacks;
import com.google.android.gms.wearable.internal.OpenChannelResponse;
import com.google.android.gms.wearable.internal.PerformEapAkaResponse;
import com.google.android.gms.wearable.internal.PutDataResponse;
import com.google.android.gms.wearable.internal.RemoveLocalCapabilityResponse;
import com.google.android.gms.wearable.internal.RpcResponse;
import com.google.android.gms.wearable.internal.SendMessageResponse;
import com.google.android.gms.wearable.internal.StorageInfoResponse;

public class BaseWearableCallbacks extends IWearableCallbacks.Stub {
    private static final String TAG = "GmsWearBaseCallback";

    @Override
    public void onGetConfigResponse(GetConfigResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onGetConfigResponse");

    }

    @Override
    public void onPutDataResponse(PutDataResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onPutDataResponse");

    }

    @Override
    public void onGetDataItemResponse(GetDataItemResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onGetDataItemResponse");

    }

    @Override
    public void onDataItemChanged(DataHolder dataHolder) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onDataItemChanged");

    }

    @Override
    public void onDeleteDataItemsResponse(DeleteDataItemsResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onDeleteDataItemsResponse");

    }

    @Override
    public void onSendMessageResponse(SendMessageResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onSendMessageResponse");

    }

    @Override
    public void onGetFdForAssetResponse(GetFdForAssetResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onGetFdForAssetResponse");

    }

    @Override
    public void onGetLocalNodeResponse(GetLocalNodeResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onGetLocalNodeResponse");

    }

    @Override
    public void onGetConnectedNodesResponse(GetConnectedNodesResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onGetConnectedNodesResponse");

    }

    @Override
    public void onOpenChannelResponse(OpenChannelResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onOpenChannelResponse");

    }

    @Override
    public void onCloseChannelResponse(CloseChannelResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onCloseChannelResponse");

    }

    @Override
    public void onCloseChannelResponse2(CloseChannelResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onCloseChannelResponse2");

    }

    @Override
    public void onGetChannelInputStreamResponse(GetChannelInputStreamResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onGetChannelInputStreamResponse");

    }

    @Override
    public void onGetChannelOutputStreamResponse(GetChannelOutputStreamResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onGetChannelOutputStreamResponse");

    }

    @Override
    public void onChannelReceiveFileResponse(ChannelReceiveFileResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onChannelReceiveFileResponse");

    }

    @Override
    public void onChannelSendFileResponse(ChannelSendFileResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onChannelSendFileResponse");

    }

    @Override
    public void onStatus(Status status) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onStatus");

    }

    @Override
    public void onStorageInfoResponse(StorageInfoResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onStorageInfoResponse");

    }

    @Override
    public void onGetCapabilityResponse(GetCapabilityResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onGetCapabilityResponse");

    }

    @Override
    public void onGetAllCapabilitiesResponse(GetAllCapabilitiesResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onGetAllCapabilitiesResponse");

    }

    @Override
    public void onAddLocalCapabilityResponse(AddLocalCapabilityResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onAddLocalCapabilityResponse");

    }

    @Override
    public void onRemoveLocalCapabilityResponse(RemoveLocalCapabilityResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onRemoveLocalCapabilityResponse");

    }

    @Override
    public void onGetTermsResponse(GetTermsResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onGetTermsResponse");

    }

    @Override
    public void onConsentResponse(ConsentResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onConsentResponse");
    }

    @Override
    public void onGetFastpairAccountKeyByAccountResponse(GetFastpairAccountKeyByAccountResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onGetFastpairAccountKeyByAccountResponse");
    }

    @Override
    public void onGetFastpairAccountKeysResponse(GetFastpairAccountKeysResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onGetFastpairAccountKeysResponse");
    }

    @Override
    public void onGetRestoreStateResponse(GetRestoreStateResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onGetRestoreStateResponse");
    }

    @Override
    public void onBooleanResponse(BooleanResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onBooleanResponse");
    }

    @Override
    public void onGetCompanionPackageForNodeResponse(GetCompanionPackageForNodeResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onGetCompanionPackageForNodeResponse");
    }

    @Override
    public void onRpcResponse(RpcResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onRpcResponse");
    }

    @Override
    public void onGetEapIdResponse(GetEapIdResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onGetEapIdResponse");
    }

    @Override
    public void onPerformEapAkaResponse(PerformEapAkaResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onPerformEapAkaResponse");
    }

    @Override
    public void onGetNodeIdResponse(GetNodeIdResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onGetNodeIdResponse");
    }

    @Override
    public void onAppRecommendationsResponse(AppRecommendationsResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onAppRecommendationsResponse");
    }

    @Override
    public void onGetAppThemeResponse(GetAppThemeResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onGetAppThemeResponse");
    }

    @Override
    public void onGetBackupSettingsSupportedResponse(GetBackupSettingsSupportedResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onGetBackupSettingsSupportedResponse");
    }

    @Override
    public void onGetRestoreSupportedResponse(GetRestoreSupportedResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onGetRestoreSupportedResponse");
    }

    @Override
    public void onGetConfigsResponse(GetConfigsResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onGetConfigsResponse");

    }

    @Override
    public void onGetCloudSyncOptInOutDoneResponse(GetCloudSyncOptInOutDoneResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onGetCloudSyncOptInOutDoneResponse");

    }

    @Override
    public void onGetCloudSyncSettingResponse(GetCloudSyncSettingResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onGetCloudSyncSettingResponse");

    }

    @Override
    public void onGetCloudSyncOptInStatusResponse(GetCloudSyncOptInStatusResponse response) throws RemoteException {
        Log.d(TAG, "unimplemented Method: onGetCloudSyncOptInStatusResponse");

    }
}
