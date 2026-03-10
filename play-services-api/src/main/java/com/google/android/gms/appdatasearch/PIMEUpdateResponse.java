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

package com.google.android.gms.appdatasearch;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class PIMEUpdateResponse extends AutoSafeParcelable {
    @SafeParceled(1000)
    private int versionCode;

    @SafeParceled(1)
    private String b;

    @SafeParceled(2)
    public final byte[] bytes;

    @SafeParceled(3)
    public final PIMEUpdate[] updates;

    public PIMEUpdateResponse() {
        versionCode = 1;
        this.bytes = null;
        this.updates = new PIMEUpdate[0];
    }

    public static final Creator<PIMEUpdateResponse> CREATOR = new AutoCreator<PIMEUpdateResponse>(PIMEUpdateResponse.class);
}
