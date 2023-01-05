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

package com.google.android.gms.location.internal;

import com.google.android.gms.common.api.Status;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class FusedLocationProviderResult extends AutoSafeParcelable {
    public static final FusedLocationProviderResult SUCCESS = FusedLocationProviderResult.create(Status.SUCCESS);

    @SafeParceled(1000)
    private int versionCode = 1;

    @SafeParceled(1)
    public Status status;

    public static FusedLocationProviderResult create(Status status) {
        FusedLocationProviderResult result = new FusedLocationProviderResult();
        result.status = status;
        return result;
    }

    public static final Creator<FusedLocationProviderResult> CREATOR = new AutoCreator<FusedLocationProviderResult>(FusedLocationProviderResult.class);
}
