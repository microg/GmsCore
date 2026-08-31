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

import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemAsset;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.DeleteDataItemsResponse;
import com.google.android.gms.wearable.internal.GetDataItemResponse;
import com.google.android.gms.wearable.internal.GetFdForAssetResponse;
import com.google.android.gms.wearable.internal.PutDataRequest;
import com.google.android.gms.wearable.internal.PutDataResponse;

import org.microg.gms.common.GmsConnector;

import java.io.InputStream;
import java.util.Map;
import java.util.WeakHashMap;

public class DataApiImpl implements DataApi {
    private static final Map<DataListener, WearableListenerStub> LISTENERS = new WeakHashMap<>();

    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, DataListener listener) {
        WearableListenerStub stub = LISTENERS.get(listener);
        if (stub == null) {
            stub = new WearableListenerStub(listener);
            LISTENERS.put(listener, stub);
        }
        final WearableListenerStub toAdd = stub;
        return GmsConnector.call(client, Wearable.API, (GmsConnector.Callback<WearableClientImpl, Status>) (wearableClient, resultProvider) ->
                wearableClient.getServiceInterface().addListener(WearableListenerStub.statusCallback(resultProvider), toAdd.toAddRequest()));
    }

    @Override
    public PendingResult<DeleteDataItemsResult> deleteDataItems(GoogleApiClient client, Uri uri) {
        return GmsConnector.call(client, Wearable.API, (GmsConnector.Callback<WearableClientImpl, DeleteDataItemsResult>) (wearableClient, resultProvider) ->
                wearableClient.getServiceInterface().deleteDataItems(new BaseWearableCallbacks() {
                    @Override
                    public void onDeleteDataItemsResponse(DeleteDataItemsResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new DeleteDataItemsResultImpl(response));
                    }
                }, uri));
    }

    @Override
    public PendingResult<DataItemResult> getDataItem(GoogleApiClient client, Uri uri) {
        return GmsConnector.call(client, Wearable.API, (GmsConnector.Callback<WearableClientImpl, DataItemResult>) (wearableClient, resultProvider) ->
                wearableClient.getServiceInterface().getDataItem(new BaseWearableCallbacks() {
                    @Override
                    public void onGetDataItemResponse(GetDataItemResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new DataItemResultImpl(response.statusCode, response.dataItem));
                    }
                }, uri));
    }

    @Override
    public PendingResult<DataItemBuffer> getDataItems(GoogleApiClient client) {
        return GmsConnector.call(client, Wearable.API, (GmsConnector.Callback<WearableClientImpl, DataItemBuffer>) (wearableClient, resultProvider) ->
                wearableClient.getServiceInterface().getDataItems(new BaseWearableCallbacks() {
                    @Override
                    public void onDataItemChanged(DataHolder dataHolder) throws RemoteException {
                        resultProvider.onResultAvailable(new DataItemBuffer(dataHolder));
                    }
                }));
    }

    @Override
    public PendingResult<DataItemBuffer> getDataItems(GoogleApiClient client, Uri uri) {
        return GmsConnector.call(client, Wearable.API, (GmsConnector.Callback<WearableClientImpl, DataItemBuffer>) (wearableClient, resultProvider) ->
                wearableClient.getServiceInterface().getDataItemsByUri(new BaseWearableCallbacks() {
                    @Override
                    public void onDataItemChanged(DataHolder dataHolder) throws RemoteException {
                        resultProvider.onResultAvailable(new DataItemBuffer(dataHolder));
                    }
                }, uri));
    }

    @Override
    public PendingResult<GetFdForAssetResult> getFdForAsset(GoogleApiClient client, DataItemAsset asset) {
        return getFdForAsset(client, Asset.createFromRef(asset.getId()));
    }

    @Override
    public PendingResult<GetFdForAssetResult> getFdForAsset(GoogleApiClient client, Asset asset) {
        return GmsConnector.call(client, Wearable.API, (GmsConnector.Callback<WearableClientImpl, GetFdForAssetResult>) (wearableClient, resultProvider) ->
                wearableClient.getServiceInterface().getFdForAsset(new BaseWearableCallbacks() {
                    @Override
                    public void onGetFdForAssetResponse(GetFdForAssetResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new GetFdForAssetResultImpl(response));
                    }
                }, asset));
    }

    @Override
    public PendingResult<DataItemResult> putDataItem(GoogleApiClient client, PutDataRequest request) {
        return GmsConnector.call(client, Wearable.API, (GmsConnector.Callback<WearableClientImpl, DataItemResult>) (wearableClient, resultProvider) ->
                wearableClient.getServiceInterface().putData(new BaseWearableCallbacks() {
                    @Override
                    public void onPutDataResponse(PutDataResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new DataItemResultImpl(response.statusCode, response.dataItem));
                    }
                }, request));
    }

    @Override
    public PendingResult<Status> removeListener(GoogleApiClient client, DataListener listener) {
        WearableListenerStub stub = LISTENERS.remove(listener);
        if (stub == null) {
            return GmsConnector.call(client, Wearable.API, (GmsConnector.Callback<WearableClientImpl, Status>) (wearableClient, resultProvider) ->
                    resultProvider.onResultAvailable(Status.SUCCESS));
        }
        return GmsConnector.call(client, Wearable.API, (GmsConnector.Callback<WearableClientImpl, Status>) (wearableClient, resultProvider) ->
                wearableClient.getServiceInterface().removeListener(WearableListenerStub.statusCallback(resultProvider), stub.toRemoveRequest()));
    }

    public static class DataItemResultImpl implements DataItemResult {
        private final int statusCode;
        private final DataItem dataItem;

        public DataItemResultImpl(int statusCode, DataItem dataItem) {
            this.statusCode = statusCode;
            this.dataItem = dataItem;
        }

        @Override
        public DataItem getDataItem() {
            return dataItem;
        }

        @Override
        public Status getStatus() {
            return new Status(statusCode);
        }
    }

    public static class DeleteDataItemsResultImpl implements DeleteDataItemsResult {
        private final DeleteDataItemsResponse response;

        public DeleteDataItemsResultImpl(DeleteDataItemsResponse response) {
            this.response = response;
        }

        @Override
        public int getNumDeleted() {
            return response.getNumDeleted();
        }

        @Override
        public Status getStatus() {
            return new Status(response.getStatusCode());
        }
    }

    public static class GetFdForAssetResultImpl implements GetFdForAssetResult {
        private final GetFdForAssetResponse response;

        public GetFdForAssetResultImpl(GetFdForAssetResponse response) {
            this.response = response;
        }

        @Override
        public ParcelFileDescriptor getFd() {
            return response.pfd;
        }

        @Override
        public InputStream getInputStream() {
            if (response.pfd == null) {
                return null;
            }
            return new ParcelFileDescriptor.AutoCloseInputStream(response.pfd);
        }

        @Override
        public Status getStatus() {
            return new Status(response.statusCode);
        }
    }
}
