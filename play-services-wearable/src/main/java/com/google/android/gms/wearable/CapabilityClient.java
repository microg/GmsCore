/*
 * Copyright (C) 2026 microG Project Team
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

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.tasks.Task;

import org.microg.gms.common.PublicApi;

import java.util.Map;

@PublicApi
public abstract class CapabilityClient extends GoogleApi<Wearable.WearableOptions> {
    public static final String ACTION_CAPABILITY_CHANGED = "com.google.android.gms.wearable.CAPABILITY_CHANGED";
    public static final int FILTER_ALL = 0;
    public static final int FILTER_REACHABLE = 1;
    public static final int FILTER_LITERAL = 0;
    public static final int FILTER_PREFIX = 1;

    public interface OnCapabilityChangedListener extends CapabilityApi.CapabilityListener {
        @Override
        void onCapabilityChanged(@NonNull CapabilityInfo capabilityInfo);
    }

    public CapabilityClient(@NonNull Context context, @NonNull Wearable.WearableOptions options) {
        super(context, Wearable.API, options, Settings.DEFAULT_SETTINGS);
    }

    public CapabilityClient(@NonNull Activity activity, @NonNull Wearable.WearableOptions options) {
        super(activity, Wearable.API, options, Settings.DEFAULT_SETTINGS);
    }

    @NonNull
    public abstract Task<CapabilityInfo> getCapability(@NonNull String capability, int nodeFilter);

    @NonNull
    public abstract Task<Map<String, CapabilityInfo>> getAllCapabilities(int nodeFilter);

    @NonNull
    public abstract Task<Void> addLocalCapability(@NonNull String capability);

    @NonNull
    public abstract Task<Void> removeLocalCapability(@NonNull String capability);

    @NonNull
    public abstract Task<Void> addListener(@NonNull OnCapabilityChangedListener listener, @NonNull String capability);

    @NonNull
    public abstract Task<Void> addListener(@NonNull OnCapabilityChangedListener listener, @NonNull Uri uri, int filterType);

    @NonNull
    public abstract Task<Boolean> removeListener(@NonNull OnCapabilityChangedListener listener);

    @NonNull
    public abstract Task<Boolean> removeListener(@NonNull OnCapabilityChangedListener listener, @NonNull String capability);
}
