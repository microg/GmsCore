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

import com.google.android.gms.wearable.internal.PutDataRequest;

import org.microg.gms.common.PublicApi;

/**
 * PutDataMapRequest is a DataMap-aware version of {@link PutDataRequest}.
 */
@PublicApi
public class PutDataMapRequest {

    private DataMapItem dataMapItem;

    private PutDataMapRequest(DataMapItem dataMapItem) {
        this.dataMapItem = dataMapItem;
    }

    /**
     * Creates a {@link PutDataRequest} containing the data and assets in this
     * {@link PutDataMapRequest}.
     */
    public PutDataRequest asPutDataRequest() {
        // TODO
        return PutDataRequest.create((Uri) null);
    }

    /**
     * Creates a {@link PutDataMapRequest} with the provided, complete, path.
     */
    public static PutDataMapRequest create(String path) {
        // TODO
        return new PutDataMapRequest(null);
    }

    /**
     * Creates a {@link PutDataMapRequest} from a {@link DataMapItem} using the provided source.
     */
    public static PutDataMapRequest createFromDataMapItem(DataMapItem source) {
        return new PutDataMapRequest(source);
    }

    /**
     * Creates a {@link PutDataMapRequest} with a randomly generated id prefixed with the provided
     * path.
     */
    public static PutDataMapRequest createWithAutoAppendedId(String pathPrefix) {
        // TODO
        return new PutDataMapRequest(null);
    }

    /**
     * @return the structured data associated with this data item.
     */
    public DataMap getDataMap() {
        return dataMapItem.getDataMap();
    }

    /**
     * @return a {@link Uri} for the pending data item. If this is a modification of an existing
     * data item, {@link Uri#getHost()} will return the id of the node that originally created it.
     * Otherwise, a new data item will be created with the requesting device's node.
     */
    public Uri getUri() {
        return dataMapItem.getUri();
    }
}
