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

import com.google.android.gms.wearable.Asset;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DataItemInternal {
    public final String host;
    public final String path;
    public final Uri uri;
    public byte[] data;
    private Map<String, Asset> assets = new HashMap<String, Asset>();

    public DataItemInternal(String host, String path) {
        this.host = host;
        this.path = path;
        this.uri = new Uri.Builder().scheme("wear").authority(host).path(path).build();
    }

    public DataItemInternal(Uri uri) {
        this.uri = uri;
        this.host = uri.getAuthority();
        this.path = uri.getPath();
    }

    public DataItemInternal addAsset(String key, Asset asset) {
        this.assets.put(key, asset);
        return this;
    }

    public Map<String, Asset> getAssets() {
        return Collections.unmodifiableMap(new HashMap<String, Asset>(assets));
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DataItemInternal{");
        sb.append("uri=").append(uri);
        sb.append(", assets=").append(assets.size());
        sb.append('}');
        return sb.toString();
    }
}
