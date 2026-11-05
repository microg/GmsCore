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

package com.google.android.gms.wearable;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.Response;
import com.google.android.gms.tasks.Task;
import org.microg.gms.common.PublicApi;

import java.io.InputStream;

@PublicApi
public abstract class DataClient extends GoogleApi<Wearable.WearableOptions> {
    public static final String ACTION_DATA_CHANGED = "com.google.android.gms.wearable.DATA_CHANGED";
    public static final int FILTER_LITERAL = 0;
    public static final int FILTER_PREFIX = 1;

    public interface OnDataChangedListener extends DataApi.DataListener {
        @Override
        void onDataChanged(@NonNull DataEventBuffer dataEvents);
    }

    public static class GetFdForAssetResponse extends Response<DataApi.GetFdForAssetResult> {
        public GetFdForAssetResponse() {
            super();
        }

        public GetFdForAssetResponse(@NonNull DataApi.GetFdForAssetResult result) {
            super(result);
        }

        public ParcelFileDescriptor getFd() {
            return getResult().getFd();
        }

        public InputStream getInputStream() {
            return getResult().getInputStream();
        }
    }

    public DataClient(@NonNull Context context, @NonNull Wearable.WearableOptions options) {
        super(context, Wearable.API, options, Settings.DEFAULT_SETTINGS);
    }

    public DataClient(@NonNull Activity activity, @NonNull Wearable.WearableOptions options) {
        super(activity, Wearable.API, options, Settings.DEFAULT_SETTINGS);
    }

    @NonNull
    public abstract Task<DataItem> putDataItem(@NonNull PutDataRequest request);

    @NonNull
    public abstract Task<DataItem> getDataItem(@NonNull Uri uri);

    @NonNull
    public abstract Task<DataItemBuffer> getDataItems();

    @NonNull
    public abstract Task<DataItemBuffer> getDataItems(@NonNull Uri uri);

    @NonNull
    public abstract Task<DataItemBuffer> getDataItems(@NonNull Uri uri, int filterType);

    @NonNull
    public abstract Task<Integer> deleteDataItems(@NonNull Uri uri);

    @NonNull
    public abstract Task<Integer> deleteDataItems(@NonNull Uri uri, int filterType);

    @NonNull
    public abstract Task<GetFdForAssetResponse> getFdForAsset(@NonNull Asset asset);

    @NonNull
    public abstract Task<GetFdForAssetResponse> getFdForAsset(@NonNull DataItemAsset asset);

    @NonNull
    public abstract Task<Void> addListener(@NonNull OnDataChangedListener listener);

    @NonNull
    public abstract Task<Void> addListener(@NonNull OnDataChangedListener listener, Uri uri, int filterType);

    @NonNull
    public abstract Task<Boolean> removeListener(@NonNull OnDataChangedListener listener);
}
