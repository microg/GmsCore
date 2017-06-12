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

import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataBuffer;
import com.google.android.gms.common.data.DataHolder;

import org.microg.gms.common.PublicApi;

/**
 * Data structure holding references to a set of events.
 */
@PublicApi
public class DataEventBuffer extends DataBuffer<DataEvent> implements Result {
    private Status status;

    @PublicApi(exclude = true)
    public DataEventBuffer(DataHolder dataHolder) {
        super(dataHolder);
        status = new Status(dataHolder.getStatusCode());
    }

    @Override
    public DataEvent get(int position) {
        return null;
    }

    @Override
    public Status getStatus() {
        return null;
    }
}
