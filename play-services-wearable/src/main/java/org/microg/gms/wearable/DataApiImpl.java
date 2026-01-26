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
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemAsset;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.DeleteDataItemsResponse;
import com.google.android.gms.wearable.internal.GetFdForAssetResponse;
import com.google.android.gms.wearable.internal.GetDataItemResponse;
import com.google.android.gms.wearable.internal.PutDataRequest;
import com.google.android.gms.wearable.internal.PutDataResponse;

import org.microg.gms.common.GmsConnector;

import java.io.IOException;
import java.io.InputStream;

public class DataApiImpl implements DataApi {
    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, DataListener listener) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            resultProvider.onResultAvailable(Status.SUCCESS);
        });
    }

    @Override
    public PendingResult<DeleteDataItemsResult> deleteDataItems(GoogleApiClient client, final Uri uri) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            wearable.getServiceInterface().deleteDataItems(new BaseWearableCallbacks() {
                @Override
                public void onDeleteDataItemsResponse(DeleteDataItemsResponse response) throws RemoteException {
                    resultProvider.onResultAvailable(new DeleteDataItemsResultImpl(new Status(response.statusCode), response.numDeleted));
                }
            }, uri);
        });
    }

    @Override
    public PendingResult<DataItemResult> getDataItem(GoogleApiClient client, final Uri uri) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            wearable.getServiceInterface().getDataItem(new BaseWearableCallbacks() {
                @Override
                public void onGetDataItemResponse(GetDataItemResponse response) throws RemoteException {
                    resultProvider.onResultAvailable(new DataItemResultImpl(new Status(response.statusCode), response.dataItem));
                }
            }, uri);
        });
    }

    @Override
    public PendingResult<DataItemBuffer> getDataItems(GoogleApiClient client) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            resultProvider.onResultAvailable(new DataItemBuffer(com.google.android.gms.common.data.DataHolder.empty(0)));
        });
    }

    @Override
    public PendingResult<DataItemBuffer> getDataItems(GoogleApiClient client, Uri uri) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            resultProvider.onResultAvailable(new DataItemBuffer(com.google.android.gms.common.data.DataHolder.empty(0)));
        });
    }

    @Override
    public PendingResult<GetFdForAssetResult> getFdForAsset(GoogleApiClient client, DataItemAsset asset) {
        return getFdForAsset(client, Asset.createFromRef(asset.getId()));
    }

    @Override
    public PendingResult<GetFdForAssetResult> getFdForAsset(GoogleApiClient client, final Asset asset) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            wearable.getServiceInterface().getFdForAsset(new BaseWearableCallbacks() {
                @Override
                public void onGetFdForAssetResponse(GetFdForAssetResponse response) throws RemoteException {
                    resultProvider.onResultAvailable(new GetFdForAssetResultImpl(new Status(response.statusCode), response.pfd));
                }
            }, asset);
        });
    }

    @Override
    public PendingResult<DataItemResult> putDataItem(GoogleApiClient client, final PutDataRequest request) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            wearable.getServiceInterface().putData(new BaseWearableCallbacks() {
                @Override
                public void onPutDataResponse(PutDataResponse response) throws RemoteException {
                    resultProvider.onResultAvailable(new DataItemResultImpl(new Status(response.statusCode), response.dataItem));
                }
            }, request);
        });
    }

    @Override
    public PendingResult<Status> removeListener(GoogleApiClient client, DataListener listener) {
        return GmsConnector.call(client, Wearable.API, (wearable, resultProvider) -> {
            resultProvider.onResultAvailable(Status.SUCCESS);
        });
    }

    private static class DataItemResultImpl implements DataItemResult {
        private final Status status;
        private final DataItem dataItem;

        private DataItemResultImpl(Status status, DataItem dataItem) {
            this.status = status;
            this.dataItem = dataItem;
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

    private static class DeleteDataItemsResultImpl implements DeleteDataItemsResult {
        private final Status status;
        private final int numDeleted;

        private DeleteDataItemsResultImpl(Status status, int numDeleted) {
            this.status = status;
            this.numDeleted = numDeleted;
        }

        @Override
        public int getNumDeleted() {
            return numDeleted;
        }

        @Override
        public Status getStatus() {
            return status;
        }
    }

    private static class GetFdForAssetResultImpl implements GetFdForAssetResult {
        private final Status status;
        private final ParcelFileDescriptor pfd;

        private GetFdForAssetResultImpl(Status status, ParcelFileDescriptor pfd) {
            this.status = status;
            this.pfd = pfd;
        }

        @Override
        public ParcelFileDescriptor getFd() {
            return pfd;
        }

        @Override
        public InputStream getInputStream() {
            return new ParcelFileDescriptor.AutoCloseInputStream(pfd);
        }

        @Override
        public Status getStatus() {
            return status;
        }

        @Override
        public void release() {
            try {
                if (pfd != null) pfd.close();
            } catch (IOException ignored) {
            }
        }
    }
}
