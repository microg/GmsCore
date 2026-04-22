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
import android.os.RemoteException;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItemAsset;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.AddListenerRequest;
import com.google.android.gms.wearable.internal.DeleteDataItemsResponse;
import com.google.android.gms.wearable.internal.GetDataItemResponse;
import com.google.android.gms.wearable.internal.GetFdForAssetResponse;
import com.google.android.gms.wearable.internal.PutDataRequest;
import com.google.android.gms.wearable.internal.PutDataResponse;
import com.google.android.gms.wearable.internal.RemoveListenerRequest;

import org.microg.gms.common.GmsConnector;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

public class DataApiImpl implements DataApi {

    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, DataListener listener) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, GmsConnector.Callback.ResultProvider<Status> resultProvider) throws RemoteException {
                AddListenerRequest request = new AddListenerRequest(listener, null);
                client.getServiceInterface().addListener(new BaseWearableCallbacks() {
                    @Override
                    public void onStatus(Status status) throws RemoteException {
                        resultProvider.onResultAvailable(status);
                    }
                }, request);
            }
        });
    }

    @Override
    public PendingResult<DeleteDataItemsResult> deleteDataItems(GoogleApiClient client, Uri uri) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, DeleteDataItemsResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, GmsConnector.Callback.ResultProvider<DeleteDataItemsResult> resultProvider) throws RemoteException {
                client.getServiceInterface().deleteDataItems(new BaseWearableCallbacks() {
                    @Override
                    public void onDeleteDataItemsResponse(DeleteDataItemsResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new DeleteDataItemsResultImpl(response));
                    }
                }, uri);
            }
        });
    }

    @Override
    public PendingResult<DataItemResult> getDataItem(GoogleApiClient client, Uri uri) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, DataItemResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, GmsConnector.Callback.ResultProvider<DataItemResult> resultProvider) throws RemoteException {
                client.getServiceInterface().getDataItem(new BaseWearableCallbacks() {
                    @Override
                    public void onGetDataItemResponse(GetDataItemResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new DataItemResultImpl(response));
                    }
                }, uri);
            }
        });
    }

    @Override
    public PendingResult<DataItemBuffer> getDataItems(GoogleApiClient client) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, DataItemBuffer>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, GmsConnector.Callback.ResultProvider<DataItemBuffer> resultProvider) throws RemoteException {
                client.getServiceInterface().getDataItems(new BaseWearableCallbacks() {
                    @Override
                    public void onDataItemChanged(DataHolder dataHolder) throws RemoteException {
                        resultProvider.onResultAvailable(new DataItemBuffer(dataHolder));
                    }
                });
            }
        });
    }

    @Override
    public PendingResult<DataItemBuffer> getDataItems(GoogleApiClient client, Uri uri) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, DataItemBuffer>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, GmsConnector.Callback.ResultProvider<DataItemBuffer> resultProvider) throws RemoteException {
                client.getServiceInterface().getDataItemsByUri(new BaseWearableCallbacks() {
                    @Override
                    public void onDataItemChanged(DataHolder dataHolder) throws RemoteException {
                        resultProvider.onResultAvailable(new DataItemBuffer(dataHolder));
                    }
                }, uri);
            }
        });
    }

    @Override
    public PendingResult<GetFdForAssetResult> getFdForAsset(GoogleApiClient client, DataItemAsset asset) {
        return getFdForAsset(client, asset.getAsset());
    }

    @Override
    public PendingResult<GetFdForAssetResult> getFdForAsset(GoogleApiClient client, Asset asset) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, GetFdForAssetResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, GmsConnector.Callback.ResultProvider<GetFdForAssetResult> resultProvider) throws RemoteException {
                client.getServiceInterface().getFdForAsset(new BaseWearableCallbacks() {
                    @Override
                    public void onGetFdForAssetResponse(GetFdForAssetResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new GetFdForAssetResultImpl(response));
                    }
                }, asset);
            }
        });
    }

    @Override
    public PendingResult<DataItemResult> putDataItem(GoogleApiClient client, PutDataRequest request) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, DataItemResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, GmsConnector.Callback.ResultProvider<DataItemResult> resultProvider) throws RemoteException {
                client.getServiceInterface().putData(new BaseWearableCallbacks() {
                    @Override
                    public void onPutDataResponse(PutDataResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new DataItemResultImpl(response.statusCode, response.dataItem));
                    }
                }, request);
            }
        });
    }

    @Override
    public PendingResult<Status> removeListener(GoogleApiClient client, DataListener listener) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, GmsConnector.Callback.ResultProvider<Status> resultProvider) throws RemoteException {
                RemoveListenerRequest request = new RemoveListenerRequest(listener);
                client.getServiceInterface().removeListener(new BaseWearableCallbacks() {
                    @Override
                    public void onStatus(Status status) throws RemoteException {
                        resultProvider.onResultAvailable(status);
                    }
                }, request);
            }
        });
    }

    public static class DataItemResultImpl implements DataItemResult {
        private final int statusCode;
        private final com.google.android.gms.wearable.DataItem dataItem;

        public DataItemResultImpl(GetDataItemResponse response) {
            this.statusCode = response.statusCode;
            this.dataItem = response.dataItem;
        }

        public DataItemResultImpl(int statusCode, com.google.android.gms.wearable.DataItem dataItem) {
            this.statusCode = statusCode;
            this.dataItem = dataItem;
        }

        @Override
        public com.google.android.gms.wearable.DataItem getDataItem() {
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
            return response.count;
        }

        @Override
        public Status getStatus() {
            return new Status(response.status);
        }
    }

    public static class GetFdForAssetResultImpl implements GetFdForAssetResult {
        private final GetFdForAssetResponse response;

        public GetFdForAssetResultImpl(GetFdForAssetResponse response) {
            this.response = response;
        }

        @Override
        public java.io.FileDescriptor getFd() {
            return response.pfd != null ? response.pfd.getFileDescriptor() : null;
        }

        @Override
        public java.io.InputStream getInputStream() {
            if (response.pfd != null) {
                try {
                    return new FileInputStream(response.pfd.getFileDescriptor());
                } catch (IOException e) {
                    return null;
                }
            }
            return null;
        }

        @Override
        public Status getStatus() {
            return new Status(response.statusCode);
        }
    }
}