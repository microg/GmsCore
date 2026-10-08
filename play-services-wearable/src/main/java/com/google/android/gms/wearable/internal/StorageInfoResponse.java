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

public class StorageInfoResponse extends AutoSafeParcelable {
    @SafeParceled(1)
    private final int version = 1;
    @SafeParceled(2)
    public int statusCode;
    @SafeParceled(3)
    public long totalSize;
    @SafeParceled(4)
    public PackageStorageInfo[] packageStorageInfo;

    private StorageInfoResponse() {}

    public StorageInfoResponse(int statusCode, long totalSize,
                               PackageStorageInfo[] packageStorageInfo) {
        this.statusCode = statusCode;
        this.totalSize = totalSize;
        this.packageStorageInfo = packageStorageInfo;
    }

    public static final Creator<StorageInfoResponse> CREATOR =
            new AutoCreator<>(StorageInfoResponse.class);
}

