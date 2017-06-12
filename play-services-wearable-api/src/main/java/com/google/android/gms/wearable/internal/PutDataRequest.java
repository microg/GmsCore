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

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

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
    }

    private PutDataRequest(Uri uri) {
        this.uri = uri;
        assets = new Bundle();
    }

    public static PutDataRequest create(Uri uri) {
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
        PutDataRequest dataRequest = new PutDataRequest(source.getUri());
        dataRequest.data = source.getData();
        // TODO: assets
        return dataRequest;
    }

    public static PutDataRequest createWithAutoAppendedId(String pathPrefix) {
        return new PutDataRequest(null);
    }

    public Asset getAsset(String key) {
        return assets.getParcelable(key);
    }

    public Map<String, Asset> getAssets() {
        Map<String, Asset> map = new HashMap<String, Asset>();
        assets.setClassLoader(DataItemAssetParcelable.class.getClassLoader());
        for (String key : assets.keySet()) {
            map.put(key, (Asset) assets.getParcelable(key));
        }
        return map;
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

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean verbose) {
        StringBuilder sb = new StringBuilder();
        sb.append("PutDataRequest[uri=").append(uri)
                .append(", data=").append(data == null ? "null" : Base64.encodeToString(data, Base64.NO_WRAP))
                .append(", numAssets=").append(getAssets().size());
        if (verbose && !getAssets().isEmpty()) {
            sb.append(", assets=[");
            for (String key : getAssets().keySet()) {
                sb.append(key).append('=').append(getAsset(key)).append(", ");
            }
            sb.delete(sb.length() - 2, sb.length()).append(']');
        }
        sb.append("]");
        return sb.toString();
    }

    public static final Creator<PutDataRequest> CREATOR = new AutoCreator<PutDataRequest>(PutDataRequest.class);
}
