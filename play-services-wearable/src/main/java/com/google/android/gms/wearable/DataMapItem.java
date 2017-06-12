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

import org.microg.gms.common.PublicApi;

/**
 * Creates a new dataItem-like object containing structured and serializable data.
 */
@PublicApi
public class DataMapItem {
    /**
     * Provides a {@link DataMapItem} wrapping a dataItem.
     *
     * @param dataItem the base for the wrapped {@link DataMapItem}. {@code dataItem} should not
     *                 be modified after wrapping it.
     */
    public static DataMapItem fromDataItem(DataItem dataItem) {
        return null;
    }

    public DataMap getDataMap() {
        return null;
    }

    public Uri getUri() {
        return null;
    }
}
