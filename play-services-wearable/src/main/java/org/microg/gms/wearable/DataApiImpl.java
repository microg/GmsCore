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
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItemAsset;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.DataItemParcelable;
import com.google.android.gms.wearable.internal.DeleteDataItemsResponse;
import com.google.android.gms.wearable.internal.GetCloudSyncOptInOutDoneResponse;
import com.google.android.gms.wearable.internal.GetDataItemResponse;
import com.google.android.gms.wearable.internal.GetFdForAssetResponse;
import com.google.android.gms.wearable.internal.PutDataRequest;
import com.google.android.gms.wearable.internal.PutDataResponse;

import org.microg.gms.common.GmsConnector;

public class DataApiImpl implements DataApi {

    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, DataListener listener) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<Status> resultProvider) throws RemoteException {
                // TODO: Implement DataListener registration
                resultProvider.onResultAvailable(Status.RESULT_SUCCESS);
            }
        });
    }

    @Override
    public PendingResult<DeleteDataItemsResult> deleteDataItems(GoogleApiClient client, Uri uri) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, DeleteDataItemsResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<DeleteDataItemsResult> resultProvider) throws RemoteException {
                client.getServiceInterface().deleteDataItems(new BaseWearableCallbacks() {
                    @Override
                    public void onDeleteDataItemsResponse(DeleteDataItemsResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new DeleteDataItemsResultImpl(response));
                    }
                }, uri, 0);
            }
        });
    }

    @Override
    public PendingResult<DataItemResult> getDataItem(GoogleApiClient client, Uri uri) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, DataItemResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<DataItemResult> resultProvider) throws RemoteException {
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
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<DataItemBuffer> resultProvider) throws RemoteException {
                client.getServiceInterface().getDataItems(new BaseWearableCallbacks() {
                    @Override
                    public void onDataItemChanged(DataItemBuffer dataItems) throws RemoteException {
                        resultProvider.onResultAvailable(dataItems);
                    }
                });
            }
        });
    }

    @Override
    public PendingResult<DataItemBuffer> getDataItems(GoogleApiClient client, Uri uri) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, DataItemBuffer>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<DataItemBuffer> resultProvider) throws RemoteException {
                client.getServiceInterface().getDataItemsByUri(new BaseWearableCallbacks() {
                    @Override
                    public void onDataItemChanged(DataItemBuffer dataItems) throws RemoteException {
                        resultProvider.onResultAvailable(dataItems);
                    }
                }, uri);
            }
        });
    }

    @Override
    public PendingResult<GetFdForAssetResult> getFdForAsset(GoogleApiClient client, DataItemAsset asset) {
        return getFdForAssetInternal(client, asset.getId());
    }

    @Override
    public PendingResult<GetFdForAssetResult> getFdForAsset(GoogleApiClient client, Asset asset) {
        return getFdForAssetInternal(client, asset.getDigest());
    }

    private PendingResult<GetFdForAssetResult> getFdForAssetInternal(GoogleApiClient client, String digest) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, GetFdForAssetResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<GetFdForAssetResult> resultProvider) throws RemoteException {
                client.getServiceInterface().getFdForAsset(new BaseWearableCallbacks() {
                    @Override
                    public void onGetFdForAssetResponse(GetFdForAssetResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new GetFdForAssetResultImpl(response));
                    }
                }, digest);
            }
        });
    }

    @Override
    public PendingResult<DataItemResult> putDataItem(GoogleApiClient client, PutDataRequest request) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, DataItemResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<DataItemResult> resultProvider) throws RemoteException {
                client.getServiceInterface().putData(new BaseWearableCallbacks() {
                    @Override
                    public void onPutDataResponse(PutDataResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new DataItemResultImpl(response));
                    }
                }, request);
            }
        });
    }

    @Override
    public PendingResult<Status> removeListener(GoogleApiClient client, DataListener listener) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<Status> resultProvider) throws RemoteException {
                // TODO: Implement listener removal
                resultProvider.onResultAvailable(Status.RESULT_SUCCESS);
            }
        });
    }

    private static class DataItemResultImpl implements DataItemResult {
        private final Status status;
        private final DataItemParcelable item;

        DataItemResultImpl(GetDataItemResponse response) {
            this.status = new Status(response.statusCode);
            this.item = response.dataItem;
        }

        DataItemResultImpl(PutDataResponse response) {
            this.status = new Status(response.statusCode);
            this.item = response.dataItem;
        }

        @Override
        public DataItemParcelable getDataItem() {
            return item;
        }

        @Override
        public Status getStatus() {
            return status;
        }
    }

    private static class DeleteDataItemsResultImpl implements DeleteDataItemsResult {
        private final DeleteDataItemsResponse response;

        DeleteDataItemsResultImpl(DeleteDataItemsResponse response) {
            this.response = response;
        }

        @Override
        public int getNumDeleted() {
            return response.numDeleted;
        }

        @Override
        public Status getStatus() {
            return new Status(response.statusCode);
        }
    }

    private static class GetFdForAssetResultImpl implements GetFdForAssetResult {
        private final GetFdForAssetResponse response;

        GetFdForAssetResultImpl(GetFdForAssetResponse response) {
            this.response = response;
        }

        @Override
        public android.os.ParcelFileDescriptor getFd() {
            return response.fd;
        }

        @Override
        public android.content.res.AssetFileDescriptor getAssetFd() {
            if (response.fd != null) {
                return new android.content.res.AssetFileDescriptor(response.fd, 0, -1);
            }
            return null;
        }

        @Override
        public Status getStatus() {
            return new Status(response.statusCode);
        }
    }
}
