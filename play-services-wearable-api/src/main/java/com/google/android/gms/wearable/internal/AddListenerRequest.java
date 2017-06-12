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

import android.content.IntentFilter;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class AddListenerRequest extends AutoSafeParcelable {
    @SafeParceled(1)
    private int versionCode = 1;
    @SafeParceled(2)
    public final IWearableListener listener;
    @SafeParceled(3)
    public final IntentFilter[] intentFilters;
    @SafeParceled(4)
    public final String channelTokenString;

    private AddListenerRequest() {
        listener = null;
        intentFilters = null;
        channelTokenString = null;
    }

    public AddListenerRequest(IWearableListener listener, IntentFilter[] intentFilters, String channelTokenString) {
        this.listener = listener;
        this.intentFilters = intentFilters;
        this.channelTokenString = channelTokenString;
    }

    public static final Creator<AddListenerRequest> CREATOR = new AutoCreator<AddListenerRequest>(AddListenerRequest.class);
}
