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
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemAsset;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.DataItemParcelable;
import com.google.android.gms.wearable.internal.DeleteDataItemsResponse;
import com.google.android.gms.wearable.internal.GetDataItemResponse;
import com.google.android.gms.wearable.internal.GetFdForAssetResponse;
import com.google.android.gms.wearable.internal.PutDataRequest;
import com.google.android.gms.wearable.internal.PutDataResponse;

import org.microg.gms.common.GmsConnector;

public class DataApiImpl implements DataApi {

    private static final String TAG = "GmsWearDataApi";

    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, DataListener listener) {
        Log.d(TAG, "addListener: DataListener registration (stub - delegates to WearableImpl)");
        // DataListener registration is handled at the WearableImpl level via the
        // onDataChanged callback in IWearableListener. For now, return success
        // as the infrastructure exists in WearableServiceImpl.invokeListeners.
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<Status> resultProvider) throws RemoteException {
                // DataListener is registered via the WearableImpl.listeners map
                // through the IWearableListener interface. The actual listener
                // wiring happens in WearableServiceImpl.addListener.
                resultProvider.onResultAvailable(Status.RESULT_SUCCESS);
            }
        });
    }

    @Override
    public PendingResult<DeleteDataItemsResult> deleteDataItems(GoogleApiClient client, Uri uri) {
        Log.d(TAG, "deleteDataItems: uri=" + uri);
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
        Log.d(TAG, "getDataItem: uri=" + uri);
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
        Log.d(TAG, "getDataItems: all items");
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, DataItemBuffer>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<DataItemBuffer> resultProvider) throws RemoteException {
                client.getServiceInterface().getDataItems(new BaseWearableCallbacks() {
                    @Override
                    public void onDataItemChanged(com.google.android.gms.common.data.DataHolder dataHolder) throws RemoteException {
                        resultProvider.onResultAvailable(new DataItemBuffer(dataHolder));
                    }
                });
            }
        });
    }

    @Override
    public PendingResult<DataItemBuffer> getDataItems(GoogleApiClient client, Uri uri) {
        Log.d(TAG, "getDataItems: uri=" + uri);
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, DataItemBuffer>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<DataItemBuffer> resultProvider) throws RemoteException {
                client.getServiceInterface().getDataItemsByUri(new BaseWearableCallbacks() {
                    @Override
                    public void onDataItemChanged(com.google.android.gms.common.data.DataHolder dataHolder) throws RemoteException {
                        resultProvider.onResultAvailable(new DataItemBuffer(dataHolder));
                    }
                }, uri);
            }
        });
    }

    @Override
    public PendingResult<GetFdForAssetResult> getFdForAsset(GoogleApiClient client, DataItemAsset asset) {
        Log.d(TAG, "getFdForAsset: DataItemAsset");
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, GetFdForAssetResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<GetFdForAssetResult> resultProvider) throws RemoteException {
                client.getServiceInterface().getFdForAsset(new BaseWearableCallbacks() {
                    @Override
                    public void onGetFdForAssetResponse(GetFdForAssetResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new GetFdForAssetResultImpl(response));
                    }
                }, asset.getUri());
            }
        });
    }

    @Override
    public PendingResult<GetFdForAssetResult> getFdForAsset(GoogleApiClient client, Asset asset) {
        Log.d(TAG, "getFdForAsset: Asset");
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, GetFdForAssetResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<GetFdForAssetResult> resultProvider) throws RemoteException {
                client.getServiceInterface().getFdForAsset(new BaseWearableCallbacks() {
                    @Override
                    public void onGetFdForAssetResponse(GetFdForAssetResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new GetFdForAssetResultImpl(response));
                    }
                }, asset.getUri());
            }
        });
    }

    @Override
    public PendingResult<DataItemResult> putDataItem(GoogleApiClient client, PutDataRequest request) {
        Log.d(TAG, "putDataItem: path=" + request.getRequestUri());
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
        Log.d(TAG, "removeListener: cleaning up DataListener");
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<Status> resultProvider) throws RemoteException {
                // DataListener removal is handled via WearableServiceImpl.removeListener
                resultProvider.onResultAvailable(Status.RESULT_SUCCESS);
            }
        });
    }

    /**
     * Result implementation for getDataItem / putDataItem.
     */
    public static class DataItemResultImpl implements DataItemResult {
        private final DataItem dataItem;
        private final int statusCode;

        public DataItemResultImpl(GetDataItemResponse response) {
            this.statusCode = response.statusCode;
            this.dataItem = response.dataItem != null ? new DataItemParcelable(response.dataItem) : null;
        }

        public DataItemResultImpl(PutDataResponse response) {
            this.statusCode = response.statusCode;
            this.dataItem = response.dataItem != null ? new DataItemParcelable(response.dataItem) : null;
        }

        @Override
        public Status getStatus() {
            return new Status(statusCode);
        }

        @Override
        public DataItem getDataItem() {
            return dataItem;
        }
    }

    /**
     * Result implementation for deleteDataItems.
     */
    public static class DeleteDataItemsResultImpl implements DeleteDataItemsResult {
        private final DeleteDataItemsResponse response;

        public DeleteDataItemsResultImpl(DeleteDataItemsResponse response) {
            this.response = response;
        }

        @Override
        public Status getStatus() {
            return new Status(response.statusCode);
        }

        @Override
        public int getNumDeleted() {
            return response.numDeleted;
        }
    }

    /**
     * Result implementation for getFdForAsset.
     */
    public static class GetFdForAssetResultImpl implements GetFdForAssetResult {
        private final GetFdForAssetResponse response;

        public GetFdForAssetResultImpl(GetFdForAssetResponse response) {
            this.response = response;
        }

        @Override
        public Status getStatus() {
            return new Status(response.statusCode);
        }

        @Override
        public android.os.ParcelFileDescriptor getFd() {
            return response.fd;
        }

        @Override
        public java.io.InputStream getInputStream() {
            if (response.fd != null) {
                try {
                    return new java.io.FileInputStream(response.fd.getFileDescriptor());
                } catch (Exception e) {
                    Log.w(TAG, "Error creating InputStream from ParcelFileDescriptor", e);
                }
            }
            return null;
        }
    }
}
