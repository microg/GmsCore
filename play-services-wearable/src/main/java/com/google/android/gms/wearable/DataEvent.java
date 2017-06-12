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

import com.google.android.gms.common.data.Freezable;

import org.microg.gms.common.PublicApi;

/**
 * Data interface for data events.
 */
@PublicApi
public interface DataEvent extends Freezable<DataEvent> {

    /**
     * Indicates that the enclosing {@link DataEvent} was triggered by a data item being added or
     * changed.
     */
    int TYPE_CHANGED = 1;

    /**
     * Indicates that the enclosing {@link DataEvent} was triggered by a data item being deleted.
     */
    int TYPE_DELETED = 2;

    /**
     * @return the data item modified in this event. An event of {@link #TYPE_DELETED} will only
     * have its {@link DataItem#getUri} populated.
     */
    DataItem getDataItem();

    /**
     * @return the type of event this is. One of {@link #TYPE_CHANGED}, {@link #TYPE_DELETED}.
     */
    int getType();
}
