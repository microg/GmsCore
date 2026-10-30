/*
 * Copyright (C) 2026 microG Project Team
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

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.PatternMatcher;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemAsset;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.internal.AddListenerRequest;
import com.google.android.gms.wearable.internal.DeleteDataItemsResponse;
import com.google.android.gms.wearable.internal.GetDataItemResponse;
import com.google.android.gms.wearable.internal.IWearableListener;
import com.google.android.gms.wearable.internal.PutDataRequest;
import com.google.android.gms.wearable.internal.PutDataResponse;
import com.google.android.gms.wearable.internal.RemoveListenerRequest;

import java.io.IOException;
import java.io.InputStream;

public class DataClientImpl extends DataClient {
    private static final String LISTENER_TYPE = "data";

    public DataClientImpl(@NonNull Context context, @NonNull Wearable.WearableOptions options) {
        super(context, options);
    }

    public DataClientImpl(@NonNull Activity activity, @NonNull Wearable.WearableOptions options) {
        super(activity, options);
    }

    @NonNull
    @Override
    public Task<DataItem> putDataItem(@NonNull PutDataRequest request) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<DataItem> completionSource) -> {
            client.getServiceInterface().putData(new BaseWearableCallbacks() {
                @Override
                public void onPutDataResponse(PutDataResponse response) {
                    WearableTasks.complete(completionSource, response.statusCode, response.dataItem);
                }
            }, request);
        });
    }

    @NonNull
    @Override
    public Task<DataItem> getDataItem(@NonNull Uri uri) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<DataItem> completionSource) -> {
            client.getServiceInterface().getDataItem(new BaseWearableCallbacks() {
                @Override
                public void onGetDataItemResponse(GetDataItemResponse response) {
                    WearableTasks.complete(completionSource, response.statusCode, response.dataItem);
                }
            }, uri);
        });
    }

    @NonNull
    @Override
    public Task<DataItemBuffer> getDataItems() {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<DataItemBuffer> completionSource) -> {
            client.getServiceInterface().getDataItems(new BaseWearableCallbacks() {
                @Override
                public void onDataItemChanged(DataHolder dataHolder) {
                    completionSource.setResult(new DataItemBuffer(dataHolder));
                }
            });
        });
    }

    @NonNull
    @Override
    public Task<DataItemBuffer> getDataItems(@NonNull Uri uri) {
        return getDataItems(uri, FILTER_LITERAL);
    }

    @NonNull
    @Override
    public Task<DataItemBuffer> getDataItems(@NonNull Uri uri, int filterType) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<DataItemBuffer> completionSource) -> {
            client.getServiceInterface().getDataItemsByUriWithFilter(new BaseWearableCallbacks() {
                @Override
                public void onDataItemChanged(DataHolder dataHolder) {
                    completionSource.setResult(new DataItemBuffer(dataHolder));
                }
            }, uri, filterType);
        });
    }

    @NonNull
    @Override
    public Task<Integer> deleteDataItems(@NonNull Uri uri) {
        return deleteDataItems(uri, FILTER_LITERAL);
    }

    @NonNull
    @Override
    public Task<Integer> deleteDataItems(@NonNull Uri uri, int filterType) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<Integer> completionSource) -> {
            client.getServiceInterface().deleteDataItemsWithFilter(new BaseWearableCallbacks() {
                @Override
                public void onDeleteDataItemsResponse(DeleteDataItemsResponse response) {
                    WearableTasks.complete(completionSource, response.getStatusCode(), response.getNumDeleted());
                }
            }, uri, filterType);
        });
    }

    @NonNull
    @Override
    public Task<GetFdForAssetResponse> getFdForAsset(@NonNull Asset asset) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<GetFdForAssetResponse> completionSource) -> {
            client.getServiceInterface().getFdForAsset(new BaseWearableCallbacks() {
                @Override
                public void onGetFdForAssetResponse(com.google.android.gms.wearable.internal.GetFdForAssetResponse response) {
                    if (response.statusCode == 0 && response.pfd == null) {
                        WearableTasks.complete(completionSource, CommonStatusCodes.ERROR, null);
                        return;
                    }
                    DataApi.GetFdForAssetResult result = new GetFdForAssetResultImpl(response.pfd, new Status(response.statusCode));
                    GetFdForAssetResponse resp = new GetFdForAssetResponse(result);
                    WearableTasks.complete(completionSource, response.statusCode, resp);
                }
            }, asset);
        });
    }

    @NonNull
    @Override
    public Task<GetFdForAssetResponse> getFdForAsset(@NonNull DataItemAsset asset) {
        return getFdForAsset(Asset.createFromRef(asset.getId()));
    }

    @NonNull
    @Override
    public Task<Void> addListener(@NonNull OnDataChangedListener listener) {
        return addListener(listener, null, FILTER_LITERAL);
    }

    @NonNull
    @Override
    public Task<Void> addListener(@NonNull OnDataChangedListener listener, Uri uri, int filterType) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<Void> completionSource) -> {
            IntentFilter filter = new IntentFilter(ACTION_DATA_CHANGED);
            if (uri != null) {
                if (uri.getScheme() != null) filter.addDataScheme(uri.getScheme());
                if (uri.getAuthority() != null) filter.addDataAuthority(uri.getAuthority(), null);
                if (uri.getPath() != null) {
                    int patternType = (filterType == FILTER_PREFIX) ? PatternMatcher.PATTERN_PREFIX : PatternMatcher.PATTERN_LITERAL;
                    filter.addDataPath(uri.getPath(), patternType);
                }
            } else {
                filter.addDataScheme("wear");
                filter.addDataAuthority("*", null);
            }
            IWearableListener stub = WearableListenerAdapter.getOrCreate(listener, LISTENER_TYPE, () -> new WearableListenerAdapter() {
                @Override
                public void onDataChanged(DataHolder data) throws RemoteException {
                    WearableListenerAdapter.postOnMainThread(data, () -> {
                        DataEventBuffer buffer = new DataEventBuffer(data);
                        try {
                            listener.onDataChanged(buffer);
                        } finally {
                            buffer.release();
                        }
                    });
                }
            });
            client.getServiceInterface().addListener(new BaseWearableCallbacks() {
                @Override
                public void onStatus(Status status) {
                    WearableTasks.completeVoid(completionSource, status.getStatusCode());
                }
            }, new AddListenerRequest(stub, new IntentFilter[]{filter}, null));
        });
    }

    @NonNull
    @Override
    public Task<Boolean> removeListener(@NonNull OnDataChangedListener listener) {
        return scheduleTask((WearableClientImpl client, TaskCompletionSource<Boolean> completionSource) -> {
            IWearableListener stub = WearableListenerAdapter.remove(listener, LISTENER_TYPE);
            if (stub == null) {
                completionSource.setResult(false);
                return;
            }
            client.getServiceInterface().removeListener(new BaseWearableCallbacks() {
                @Override
                public void onStatus(Status status) {
                    WearableTasks.complete(completionSource, status.getStatusCode(), true);
                }
            }, new RemoveListenerRequest(stub));
        });
    }

    private static class GetFdForAssetResultImpl implements DataApi.GetFdForAssetResult {
        private final ParcelFileDescriptor pfd;
        private final Status status;

        public GetFdForAssetResultImpl(ParcelFileDescriptor pfd, Status status) {
            this.pfd = pfd;
            this.status = status;
        }

        @Override
        public Status getStatus() {
            return status;
        }

        @Override
        public ParcelFileDescriptor getFd() {
            return pfd;
        }

        @Override
        public InputStream getInputStream() {
            return pfd != null ? new ParcelFileDescriptor.AutoCloseInputStream(pfd) : null;
        }

        @Override
        public void release() {
            if (pfd != null) {
                try {
                    pfd.close();
                } catch (IOException ignored) {}
            }
        }
    }
}
