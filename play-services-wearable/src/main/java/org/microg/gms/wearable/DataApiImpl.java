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

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItemAsset;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.internal.PutDataRequest;

public class DataApiImpl implements DataApi {
    @Override
    public PendingResult<Status> addListener(GoogleApiClient client, DataListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PendingResult<DeleteDataItemsResult> deleteDataItems(GoogleApiClient client, Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PendingResult<DataItemResult> getDataItem(GoogleApiClient client, Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PendingResult<DataItemBuffer> getDataItems(GoogleApiClient client) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PendingResult<DataItemBuffer> getDataItems(GoogleApiClient client, Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PendingResult<GetFdForAssetResult> getFdForAsset(GoogleApiClient client, DataItemAsset asset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PendingResult<GetFdForAssetResult> getFdForAsset(GoogleApiClient client, Asset asset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PendingResult<DataItemResult> putDataItem(GoogleApiClient client, PutDataRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PendingResult<Status> removeListener(GoogleApiClient client, DataListener listener) {
        throw new UnsupportedOperationException();
    }
}
