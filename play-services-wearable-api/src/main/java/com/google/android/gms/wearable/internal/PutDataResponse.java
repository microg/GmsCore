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

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class PutDataResponse extends AutoSafeParcelable {
    @SafeParceled(1)
    private int versionCode = 1;
    @SafeParceled(2)
    public final int statusCode;
    @SafeParceled(3)
    public final DataItemParcelable dataItem;

    private PutDataResponse() {
        statusCode = 0;
        dataItem = null;
    }

    public PutDataResponse(int statusCode, DataItemParcelable dataItem) {
        this.statusCode = statusCode;
        this.dataItem = dataItem;
    }

    public static final Creator<PutDataResponse> CREATOR = new AutoCreator<PutDataResponse>(PutDataResponse.class);
}
