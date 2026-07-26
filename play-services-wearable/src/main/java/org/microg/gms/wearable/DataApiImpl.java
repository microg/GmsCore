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
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemAsset;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.DataItemParcelable;
import com.google.android.gms.wearable.internal.DeleteDataItemsResponse;
import com.google.android.gms.wearable.internal.GetDataItemResponse;
import com.google.android.gms.wearable.internal.GetFdForAssetResponse;
import com.google.android.gms.wearable.internal.IWearableListener;
import com.google.android.gms.wearable.internal.PutDataRequest;
import com.google.android.gms.wearable.internal.PutDataResponse;
import com.google.android.gms.wearable.internal.AddListenerRequest;
import com.google.android.gms.wearable.internal.RemoveListenerRequest;

import org.microg.gms.common.GmsConnector;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataApiImpl implements DataApi {
    private final Map<DataListener, IWearableListener> listenerWrappers = new ConcurrentHashMap<DataListener, IWearableListener>();

    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, final DataListener listener) {
        final IWearableListener wrapper = new DataListenerWrapper(listener);
        listenerWrappers.put(listener, wrapper);
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<Status> resultProvider) throws RemoteException {
                client.getServiceInterface().addListener(new BaseWearableCallbacks() {
                    @Override
                    public void onStatus(Status status) throws RemoteException {
                        resultProvider.onResultAvailable(status);
                    }
                }, new AddListenerRequest(wrapper, null, null));
            }
        });
    }

    @Override
    public PendingResult<DeleteDataItemsResult> deleteDataItems(GoogleApiClient client, final Uri uri) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, DeleteDataItemsResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<DeleteDataItemsResult> resultProvider) throws RemoteException {
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
    public PendingResult<DataItemResult> getDataItem(GoogleApiClient client, final Uri uri) {
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
                    public void onDataItemChanged(DataHolder dataHolder) throws RemoteException {
                        resultProvider.onResultAvailable(new DataItemBuffer(dataHolder));
                    }
                });
            }
        });
    }

    @Override
    public PendingResult<DataItemBuffer> getDataItems(GoogleApiClient client, final Uri uri) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, DataItemBuffer>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<DataItemBuffer> resultProvider) throws RemoteException {
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
    public PendingResult<GetFdForAssetResult> getFdForAsset(GoogleApiClient client, final DataItemAsset asset) {
        final Asset assetObj = Asset.createFromRef(asset.getId());
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, GetFdForAssetResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<GetFdForAssetResult> resultProvider) throws RemoteException {
                client.getServiceInterface().getFdForAsset(new BaseWearableCallbacks() {
                    @Override
                    public void onGetFdForAssetResponse(GetFdForAssetResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new GetFdForAssetResultImpl(response));
                    }
                }, assetObj);
            }
        });
    }

    @Override
    public PendingResult<GetFdForAssetResult> getFdForAsset(GoogleApiClient client, final Asset asset) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, GetFdForAssetResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<GetFdForAssetResult> resultProvider) throws RemoteException {
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
    public PendingResult<DataItemResult> putDataItem(GoogleApiClient client, final PutDataRequest request) {
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
    public PendingResult<Status> removeListener(GoogleApiClient client, final DataListener listener) {
        final IWearableListener wrapper = listenerWrappers.remove(listener);
        if (wrapper == null) {
            return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
                @Override
                public void onClientAvailable(WearableClientImpl client, final ResultProvider<Status> resultProvider) throws RemoteException {
                    resultProvider.onResultAvailable(Status.SUCCESS);
                }
            });
        }
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<Status> resultProvider) throws RemoteException {
                client.getServiceInterface().removeListener(new BaseWearableCallbacks() {
                    @Override
                    public void onStatus(Status status) throws RemoteException {
                        resultProvider.onResultAvailable(status);
                    }
                }, new RemoveListenerRequest(wrapper));
            }
        });
    }

    public static class DataItemResultImpl implements DataItemResult {
        private Status status;
        private DataItem dataItem;

        public DataItemResultImpl(GetDataItemResponse response) {
            this.status = new Status(response.statusCode);
            this.dataItem = response.dataItem;
        }

        public DataItemResultImpl(PutDataResponse response) {
            this.status = new Status(response.statusCode);
            this.dataItem = response.dataItem;
        }

        @Override
        public DataItem getDataItem() {
            return dataItem;
        }

        @Override
        public Status getStatus() {
            return status;
        }
    }

    public static class DeleteDataItemsResultImpl implements DeleteDataItemsResult {
        private Status status;

        public DeleteDataItemsResultImpl(DeleteDataItemsResponse response) {
            this.status = Status.SUCCESS;
        }

        @Override
        public int getNumDeleted() {
            return -1;
        }

        @Override
        public Status getStatus() {
            return status;
        }
    }

    public static class GetFdForAssetResultImpl implements GetFdForAssetResult {
        private Status status;
        private ParcelFileDescriptor pfd;

        public GetFdForAssetResultImpl(GetFdForAssetResponse response) {
            this.status = new Status(response.statusCode);
            this.pfd = response.pfd;
        }

        @Override
        public ParcelFileDescriptor getFd() {
            return pfd;
        }

        @Override
        public InputStream getInputStream() {
            if (pfd != null) {
                return new ParcelFileDescriptor.AutoCloseInputStream(pfd);
            }
            return null;
        }

        @Override
        public Status getStatus() {
            return status;
        }
    }

    private static class DataListenerWrapper extends IWearableListener.Stub {
        private final DataListener listener;

        DataListenerWrapper(DataListener listener) {
            this.listener = listener;
        }

        @Override
        public void onDataChanged(DataHolder dataHolder) throws RemoteException {
            DataEventBuffer buffer = new DataEventBuffer(dataHolder);
            listener.onDataChanged(buffer);
        }
    }
}
