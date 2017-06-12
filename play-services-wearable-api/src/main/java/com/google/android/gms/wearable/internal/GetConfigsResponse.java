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

import com.google.android.gms.wearable.ConnectionConfiguration;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class GetConfigsResponse extends AutoSafeParcelable {
    @SafeParceled(1)
    private int versionCode = 1;
    @SafeParceled(2)
    public final int statusCode;
    @SafeParceled(3)
    public final ConnectionConfiguration[] configurations;

    private GetConfigsResponse() {
        statusCode = 0;
        configurations = null;
    }

    public GetConfigsResponse(int statusCode, ConnectionConfiguration[] configurations) {
        this.statusCode = statusCode;
        this.configurations = configurations;
    }

    public static final Creator<GetConfigsResponse> CREATOR = new AutoCreator<GetConfigsResponse>(GetConfigsResponse.class);
}
