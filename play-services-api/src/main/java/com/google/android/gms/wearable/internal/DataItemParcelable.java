/*
 * Copyright 2013-2015 µg Project Team
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

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.HashMap;
import java.util.Map;

public class DataItemParcelable extends AutoSafeParcelable {
    @SafeParceled(1)
    private int versionCode = 1;
    @SafeParceled(2)
    public final Uri uri;
    @SafeParceled(4)
    private final Bundle assets;
    @SafeParceled(5)
    public byte[] data;

    private DataItemParcelable() {
        uri = null;
        assets = new Bundle();
        data = null;
    }

    public DataItemParcelable(Uri uri) {
        this(uri, new HashMap<String, DataItemAssetParcelable>());
    }

    public DataItemParcelable(Uri uri, Map<String, DataItemAssetParcelable> assets) {
        this.uri = uri;
        this.assets = new Bundle();
        for (String key : assets.keySet()) {
            this.assets.putParcelable(key, assets.get(key));
        }
        data = null;
    }

    public Map<String, DataItemAssetParcelable> getAssets() {
        Map<String, DataItemAssetParcelable> assets = new HashMap<String, DataItemAssetParcelable>();
        this.assets.setClassLoader(DataItemAssetParcelable.class.getClassLoader());
        for (String key : this.assets.keySet()) {
            assets.put(key, (DataItemAssetParcelable) this.assets.getParcelable(key));
        }
        return assets;
    }

    public static final Creator<DataItemParcelable> CREATOR = new AutoCreator<DataItemParcelable>(DataItemParcelable.class);
}
