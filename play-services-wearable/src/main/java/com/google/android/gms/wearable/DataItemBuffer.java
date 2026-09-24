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
        status = new Status(dataHolder == null ? 8 : dataHolder.getStatusCode());
    }

    @Override
    public DataItem get(int position) {
        if (dataHolder == null || position < 0 || position >= dataHolder.getCount()) {
            return null;
        }
        int windowIndex = dataHolder.getWindowIndex(position);
        String host = optString("host", position, windowIndex);
        String path = optString("path", position, windowIndex);
        byte[] data = optBytes("data", position, windowIndex);
        Uri uri = buildWearUri(host, path);

        Map<String, DataItemAssetParcelable> assets = new HashMap<>();
        for (int i = position; i < dataHolder.getCount(); i++) {
            int w = dataHolder.getWindowIndex(i);
            if (!eq(host, optString("host", i, w)) || !eq(path, optString("path", i, w))) {
                break;
            }
            String key = optString("asset_key", i, w);
            String id = optString("asset_id", i, w);
            if (key != null && key.length() > 0 && id != null) {
                assets.put(key, new DataItemAssetParcelable(id, key));
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

    private String optString(String column, int row, int windowIndex) {
        try {
            return dataHolder.getString(column, row, windowIndex);
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] optBytes(String column, int row, int windowIndex) {
        try {
            return dataHolder.getByteArray(column, row, windowIndex);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static Uri buildWearUri(String host, String path) {
        StringBuilder sb = new StringBuilder("wear:");
        if (host != null && host.length() > 0) {
            sb.append("//").append(host);
        }
        if (path == null || path.length() == 0) {
            sb.append("/");
        } else if (path.startsWith("/")) {
            sb.append(path);
        } else {
            sb.append("/").append(path);
        }
        return Uri.parse(sb.toString());
    }
}
