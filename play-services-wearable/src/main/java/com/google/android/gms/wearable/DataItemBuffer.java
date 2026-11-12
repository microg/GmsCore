/*
 * Copyright (C) 2013-2026 microG Project Team
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

import android.net.Uri;

import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.AbstractDataBuffer;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.wearable.internal.DataItemAssetParcelable;
import com.google.android.gms.wearable.internal.DataItemParcelable;

import org.microg.gms.common.PublicApi;

import java.util.HashMap;
import java.util.Map;

@PublicApi
public class DataItemBuffer extends AbstractDataBuffer<DataItem> implements Result {
    private Status status;

    @PublicApi(exclude = true)
    public DataItemBuffer(DataHolder dataHolder) {
        super(dataHolder);
        status = new Status(dataHolder != null ? dataHolder.getStatusCode() : 0);
    }

    @Override
    public DataItem get(int position) {
        if (dataHolder == null) return null;
        int windowIndex = dataHolder.getWindowIndex(position);
        String host = dataHolder.hasColumn("host") ? dataHolder.getString("host", position, windowIndex) : "";
        String path = dataHolder.hasColumn("path") ? dataHolder.getString("path", position, windowIndex) : "";
        byte[] data = dataHolder.hasColumn("data") ? dataHolder.getByteArray("data", position, windowIndex) : null;

        Uri uri;
        if (path != null && path.startsWith("wear://")) {
            uri = Uri.parse(path);
        } else {
            uri = new Uri.Builder().scheme("wear").authority(host != null ? host : "").path(path).build();
        }

        Map<String, DataItemAssetParcelable> assets = new HashMap<>();
        if (dataHolder.hasColumn("asset_key") && dataHolder.hasColumn("asset_id")) {
            String assetKey = dataHolder.getString("asset_key", position, windowIndex);
            String assetId = dataHolder.getString("asset_id", position, windowIndex);
            if (assetKey != null && assetId != null) {
                assets.put(assetKey, new DataItemAssetParcelable(assetId, assetKey));
            }
        }

        DataItemParcelable item = new DataItemParcelable(uri, assets);
        item.data = data;
        return item;
    }

    @Override
    public Status getStatus() {
        return status;
    }
}
