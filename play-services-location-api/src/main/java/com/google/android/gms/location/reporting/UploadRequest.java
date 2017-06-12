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

package com.google.android.gms.location.reporting;

import android.accounts.Account;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class UploadRequest extends AutoSafeParcelable {
    @SafeParceled(1)
    public int versionCode = 1;
    @SafeParceled(2)
    public Account account;
    @SafeParceled(3)
    public String reason;
    @SafeParceled(4)
    public long durationMillis;
    @SafeParceled(5)
    public long movingLatencyMillis;
    @SafeParceled(6)
    public long stationaryLatencyMillis;
    @SafeParceled(7)
    public String appSpecificKey;

    public static final Creator<UploadRequest> CREATOR = new AutoCreator<UploadRequest>(UploadRequest.class);
}
