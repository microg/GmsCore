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

package com.google.android.gms.wearable;

import android.net.Uri;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Collections;
import java.util.Map;

/**
 * {@link PutDataRequest} is used to create new data items in the Android Wear network.
 */
@PublicApi
public class PutDataRequest extends AutoSafeParcelable {
    public static final String WEAR_URI_SCHEME = "wear";

    private DataItem dataItem;

    private PutDataRequest(DataItem dataItem) {
        this.dataItem = dataItem;
    }

    public static PutDataRequest create(String path) {
        // TODO
        return new PutDataRequest(null);
    }

    public static PutDataRequest createFromDataItem(DataItem source) {
        return new PutDataRequest(source);
    }

    public static PutDataRequest createWithAutoAppendedId(String pathPrefix) {
        return new PutDataRequest(null);
    }

    public Asset getAsset(String key) {
        // TODO
        return null;
    }

    public Map<String, Asset> getAssets() {
        // TODO
        return Collections.emptyMap();
    }

    public byte[] getData() {
        return dataItem.getData();
    }

    public Uri getUri() {
        return dataItem.getUri();
    }

    public boolean hasAsset(String key) {
        return dataItem.getAssets().containsKey(key);
    }

    public PutDataRequest putAsset(String key, Asset value) {
        // TODO
        return this;
    }

    public PutDataRequest removeAsset(String key) {
        // TODO
        return this;
    }

    public PutDataRequest setData(byte[] data) {
        // TODO
        return this;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean verbose) {
        return "PutDataRequest{data=" + dataItem + "}";
    }

    public static final Creator<PutDataRequest> CREATOR = new AutoCreator<PutDataRequest>(PutDataRequest.class);
}
