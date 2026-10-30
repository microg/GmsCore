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

/**
 * Data structure holding references to a set of events.
 */
@PublicApi
public class DataEventBuffer extends AbstractDataBuffer<DataEvent> implements Result {
    private Status status;

    @PublicApi(exclude = true)
    public DataEventBuffer(DataHolder dataHolder) {
        super(dataHolder);
        status = new Status(dataHolder != null ? dataHolder.getStatusCode() : 0);
    }

    @Override
    public DataEvent get(int position) {
        if (dataHolder == null) return null;
        int windowIndex = dataHolder.getWindowIndex(position);
        int eventType = dataHolder.hasColumn("event_type") ? dataHolder.getInteger("event_type", position, windowIndex) : 1;
        String path = dataHolder.hasColumn("path") ? dataHolder.getString("path", position, windowIndex) : "";
        byte[] data = dataHolder.hasColumn("data") ? dataHolder.getByteArray("data", position, windowIndex) : null;

        Uri uri = path != null ? Uri.parse(path) : Uri.EMPTY;
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
        return new DataEventImpl(eventType, item);
    }

    @Override
    public Status getStatus() {
        return status;
    }

    private static class DataEventImpl implements DataEvent {
        private final int type;
        private final DataItem dataItem;

        public DataEventImpl(int type, DataItem dataItem) {
            this.type = type;
            this.dataItem = dataItem;
        }

        @Override
        public DataItem getDataItem() {
            return dataItem;
        }

        @Override
        public int getType() {
            return type;
        }

        @Override
        public DataEvent freeze() {
            return this;
        }

        @Override
        public boolean isDataValid() {
            return true;
        }
    }
}
