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
package com.google.android.gms.wearable

import android.net.Uri
import com.google.android.gms.common.data.Freezable
import org.microg.gms.common.PublicApi

/**
 * The base object of data stored in the Android Wear network. [DataItem] are replicated
 * across all devices in the network. It contains a small blob of data and associated assets.
 *
 *
 * A [DataItem] is identified by its Uri, which contains its creator and a path.
 */
@PublicApi
interface DataItem : Freezable<DataItem?> {
    /**
     * A map of assets associated with this data item.
     */
    fun getAssets(): Map<String?, DataItemAsset?>?

    /**
     * An array of data stored at the specified [Uri].
     */
    fun getData(): ByteArray?

    /**
     * Returns the DataItem's Uri. [Uri.getHost] returns the id of the node that created it.
     */
    fun getUri(): Uri?

    /**
     * Sets the data in a data item.
     *
     *
     * The current maximum data item size limit is approximately 100k. Data items should generally be much smaller than this limit.
     */
    fun setData(data: ByteArray?): DataItem?
}