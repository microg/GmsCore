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

package com.google.android.gms.search.global;

import android.os.Parcelable;

import com.google.android.gms.common.api.Status;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.Arrays;

public class GetGlobalSearchSourcesResponse extends AutoSafeParcelable {

    @SafeParceled(1000)
    private int versionCode = 1;

    @SafeParceled(1)
    public final Status status;

    @SafeParceled(2)
    public final Parcelable[] sources;

    private GetGlobalSearchSourcesResponse() {
        status = null;
        sources = null;
    }

    public GetGlobalSearchSourcesResponse(Status status, Parcelable[] sources) {
        this.status = status;
        this.sources = sources;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GetGlobalSearchSourcesResponse{");
        sb.append("status=").append(status);
        sb.append(", sources=").append(Arrays.toString(sources));
        sb.append('}');
        return sb.toString();
    }

    public static final Creator<GetGlobalSearchSourcesResponse> CREATOR = new AutoCreator<GetGlobalSearchSourcesResponse>(GetGlobalSearchSourcesResponse.class);
}
