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

package com.google.android.gms.wearable.internal;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;

import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemAsset;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link PutDataRequest} is used to create new data items in the Android Wear network.
 */
@PublicApi
public class PutDataRequest extends AutoSafeParcelable {
    public static final String WEAR_URI_SCHEME = "wear";
    private static final int DEFAULT_SYNC_DEADLINE = 30 * 60 * 1000;

    @SafeParceled(1)
    private int versionCode = 1;
    @SafeParceled(2)
    final Uri uri;
    @SafeParceled(4)
    private final Bundle assets;
    @SafeParceled(5)
    byte[] data;
    @SafeParceled(6)
    long syncDeadline = DEFAULT_SYNC_DEADLINE;

    private PutDataRequest() {
        uri = null;
        assets = new Bundle();
        initializeAssetsClassLoader();
    }

    private PutDataRequest(Uri uri) {
        this.uri = uri;
        assets = new Bundle();
        initializeAssetsClassLoader();
    }

    private PutDataRequest(Uri uri, Bundle assets, byte[] data, long syncDeadline) {
        this.uri = uri;
        this.assets = assets;
        this.data = data;
        this.syncDeadline = syncDeadline;
        initializeAssetsClassLoader();
    }

    private void initializeAssetsClassLoader() {
        ClassLoader classLoader = DataItemAssetParcelable.class.getClassLoader();
        if (classLoader != null) {
            assets.setClassLoader(classLoader);
        }
    }


    public static PutDataRequest create(Uri uri) {
        if (uri == null) {
            throw new IllegalArgumentException("uri must not be null");
        }
        return new PutDataRequest(uri);
    }

    public static PutDataRequest create(String path) {
        if (TextUtils.isEmpty(path)) {
            throw new IllegalArgumentException("An empty path was supplied.");
        } else if (!path.startsWith("/")) {
            throw new IllegalArgumentException("A path must start with a single / .");
        } else if (path.startsWith("//")) {
            throw new IllegalArgumentException("A path must start with a single / .");
        } else {
            return create((new Uri.Builder()).scheme(WEAR_URI_SCHEME).path(path).build());
        }
    }

    public static PutDataRequest createFromDataItem(DataItem source) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        PutDataRequest dataRequest = new PutDataRequest(source.getUri());
        dataRequest.data = source.getData();

        Map<String, DataItemAsset> sourceAssets = source.getAssets();
        if (sourceAssets != null) {
            for (Map.Entry<String, DataItemAsset> entry : sourceAssets.entrySet()) {
                DataItemAsset itemAsset = entry.getValue();
                if (itemAsset != null && itemAsset.getId() != null) {
                    Asset asset = Asset.createFromRef(itemAsset.getId());
                    dataRequest.putAsset(entry.getKey(), asset);
                }
            }
        }
        return dataRequest;
    }

    public static PutDataRequest createWithAutoAppendedId(String pathPrefix) {
        if (TextUtils.isEmpty(pathPrefix)) {
            throw new IllegalArgumentException("An empty pathPrefix was supplied.");
        } else if (!pathPrefix.startsWith("/")) {
            throw new IllegalArgumentException("A pathPrefix must start with a single / .");
        } else if (pathPrefix.startsWith("//")) {
            throw new IllegalArgumentException("A pathPrefix must start with a single / .");
        }
        String uniqueId = Long.toHexString(System.currentTimeMillis()) +
                Long.toHexString(Double.doubleToLongBits(Math.random()));
        String path = pathPrefix.endsWith("/") ? pathPrefix + uniqueId : pathPrefix + "/" + uniqueId;
        return create(path);
    }

    public Asset getAsset(String key) {
        if (key == null) {
            return null;
        }

        assets.setClassLoader(DataItemAssetParcelable.class.getClassLoader());
        return assets.getParcelable(key);
    }

    public Map<String, Asset> getAssets() {
        Map<String, Asset> map = new HashMap<String, Asset>();
        assets.setClassLoader(DataItemAssetParcelable.class.getClassLoader());
        for (String key : assets.keySet()) {
            map.put(key, (Asset) assets.getParcelable(key));
        }
        return Collections.unmodifiableMap(map);
    }

    public byte[] getData() {
        return data;
    }

    public Uri getUri() {
        return uri;
    }

    public boolean hasAsset(String key) {
        return assets.containsKey(key);
    }

    public PutDataRequest putAsset(String key, Asset value) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        assets.putParcelable(key, value);
        return this;

    }

    public PutDataRequest removeAsset(String key) {
        assets.remove(key);
        return this;
    }

    public PutDataRequest setData(byte[] data) {
        this.data = data;
        return this;
    }

    public PutDataRequest setUrgent(boolean urgent) {
        this.syncDeadline = urgent ? 0 : DEFAULT_SYNC_DEADLINE;
        return this;
    }

    public boolean isUrgent() {
        return syncDeadline == 0;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean verbose) {
        StringBuilder sb = new StringBuilder();
        sb.append("PutDataRequest[");
        sb.append("dataSz=").append(data == null ? "null" : data.length);
        sb.append(", numAssets=").append(assets.size());
        sb.append(", uri=").append(uri);
        sb.append(", syncDeadline=").append(syncDeadline);

        if (verbose && !getAssets().isEmpty()) {
            sb.append("]\n  assets: ");
            for (Map.Entry<String, Asset> entry : getAssets().entrySet()) {
                sb.append("\n    ").append(entry.getKey()).append(": ").append(entry.getValue());
            }
            sb.append("\n  ]");
        } else {
            sb.append("]");
        }
        return sb.toString();
    }

    public static final Creator<PutDataRequest> CREATOR = new AutoCreator<PutDataRequest>(PutDataRequest.class);
}
