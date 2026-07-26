/*
 * Copyright (C) 2013-2025 microG Project Team
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
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemAsset;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.DataItemAssetParcelable;
import com.google.android.gms.wearable.internal.DataItemParcelable;
import com.google.android.gms.wearable.internal.GetDataItemResponse;
import com.google.android.gms.wearable.internal.GetFdForAssetResponse;
import com.google.android.gms.wearable.internal.PutDataRequest;
import com.google.android.gms.wearable.internal.PutDataResponse;
import com.google.android.gms.wearable.internal.DeleteDataItemsResponse;

import org.microg.gms.common.GmsConnector;
import org.microg.gms.common.api.BasePendingResult;
import org.microg.gms.common.api.InstantPendingResult;

import java.util.ArrayList;
import java.util.List;

public class DataApiImpl implements DataApi {

    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, final DataListener listener) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<Status> resultProvider) throws RemoteException {
                client.getServiceInterface().addListener(new BaseWearableCallbacks() {
                    @Override
                    public void onStatus(Status status) throws RemoteException {
                        resultProvider.onResultAvailable(status);
                    }
                }, new com.google.android.gms.wearable.internal.AddListenerRequest(listener));
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
                        resultProvider.onResultAvailable(new DeleteDataItemsResult() {
                            @Override
                            public int getNumDeleted() {
                                return response.numDeleted;
                            }

                            @Override
                            public Status getStatus() {
                                return new Status(response.statusCode);
                            }
                        });
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
                        resultProvider.onResultAvailable(new DataItemResult() {
                            @Override
                            public DataItem getDataItem() {
                                return response.dataItem;
                            }

                            @Override
                            public Status getStatus() {
                                return new Status(response.statusCode);
                            }
                        });
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
                    public void onDataItemChanged(com.google.android.gms.common.data.DataHolder dataHolder) throws RemoteException {
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
                    public void onDataItemChanged(com.google.android.gms.common.data.DataHolder dataHolder) throws RemoteException {
                        resultProvider.onResultAvailable(new DataItemBuffer(dataHolder));
                    }
                }, uri);
            }
        });
    }

    @Override
    public PendingResult<GetFdForAssetResult> getFdForAsset(GoogleApiClient client, final DataItemAsset asset) {
        return getFdForAsset(client, Asset.createFromRef(asset.getDataItemKey()));
    }

    @Override
    public PendingResult<GetFdForAssetResult> getFdForAsset(GoogleApiClient client, final Asset asset) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, GetFdForAssetResult>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<GetFdForAssetResult> resultProvider) throws RemoteException {
                client.getServiceInterface().getFdForAsset(new BaseWearableCallbacks() {
                    @Override
                    public void onGetFdForAssetResponse(GetFdForAssetResponse response) throws RemoteException {
                        resultProvider.onResultAvailable(new GetFdForAssetResult() {
                            @Override
                            public android.os.ParcelFileDescriptor getFd() {
                                return response.fd;
                            }

                            @Override
                            public Status getStatus() {
                                return new Status(response.statusCode);
                            }
                        });
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
                        resultProvider.onResultAvailable(new DataItemResult() {
                            @Override
                            public DataItem getDataItem() {
                                return response.dataItem;
                            }

                            @Override
                            public Status getStatus() {
                                return new Status(response.statusCode);
                            }
                        });
                    }
                }, request);
            }
        });
    }

    @Override
    public PendingResult<Status> removeListener(GoogleApiClient client, final DataListener listener) {
        return GmsConnector.call(client, Wearable.API, new GmsConnector.Callback<WearableClientImpl, Status>() {
            @Override
            public void onClientAvailable(WearableClientImpl client, final ResultProvider<Status> resultProvider) throws RemoteException {
                client.getServiceInterface().removeListener(new BaseWearableCallbacks() {
                    @Override
                    public void onStatus(Status status) throws RemoteException {
                        resultProvider.onResultAvailable(status);
                    }
                }, new com.google.android.gms.wearable.internal.RemoveListenerRequest(listener));
            }
        });
    }
}