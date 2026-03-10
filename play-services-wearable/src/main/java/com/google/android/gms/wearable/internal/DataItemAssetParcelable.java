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

import com.google.android.gms.wearable.DataItemAsset;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class DataItemAssetParcelable extends AutoSafeParcelable implements DataItemAsset {

    @SafeParceled(1)
    private int versionCode = 1;
    @SafeParceled(2)
    private String id;
    @SafeParceled(3)
    private String key;

    private DataItemAssetParcelable() {
    }

    public DataItemAssetParcelable(String id, String key) {
        this.id = id;
        this.key = key;
    }

    @Override
    public String getDataItemKey() {
        return key;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public DataItemAsset freeze() {
        return this;
    }

    @Override
    public boolean isDataValid() {
        return true;
    }

    public static final Creator<DataItemAssetParcelable> CREATOR = new AutoCreator<DataItemAssetParcelable>(DataItemAssetParcelable.class);
}
