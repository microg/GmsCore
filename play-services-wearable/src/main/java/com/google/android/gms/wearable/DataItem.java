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

import com.google.android.gms.common.data.Freezable;

import org.microg.gms.common.PublicApi;

import java.util.Map;

/**
 * The base object of data stored in the Android Wear network. {@link DataItem} are replicated
 * across all devices in the network. It contains a small blob of data and associated assets.
 * <p/>
 * A {@link DataItem} is identified by its Uri, which contains its creator and a path.
 */
@PublicApi
public interface DataItem extends Freezable<DataItem> {
    /**
     * A map of assets associated with this data item.
     */
    Map<String, DataItemAsset> getAssets();

    /**
     * An array of data stored at the specified {@link Uri}.
     */
    byte[] getData();

    /**
     * Returns the DataItem's Uri. {@link Uri#getHost()} returns the id of the node that created it.
     */
    Uri getUri();

    /**
     * Sets the data in a data item.
     * <p/>
     * The current maximum data item size limit is approximately 100k. Data items should generally be much smaller than this limit.
     */
    DataItem setData(byte[] data);
}
