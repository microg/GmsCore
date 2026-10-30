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
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.tasks.Task;

import org.microg.gms.common.PublicApi;

import java.io.InputStream;
import java.io.OutputStream;

@PublicApi
public abstract class ChannelClient extends GoogleApi<Wearable.WearableOptions> {
    public static final String ACTION_CHANNEL_EVENT = "com.google.android.gms.wearable.CHANNEL_EVENT";

    public static final int CLOSE_REASON_NORMAL = 0;
    public static final int CLOSE_REASON_DISCONNECTED = 1;
    public static final int CLOSE_REASON_REMOTE_CLOSE = 2;
    public static final int CLOSE_REASON_LOCAL_CLOSE = 3;

    public interface Channel extends Parcelable {
        String getNodeId();
        String getPath();
    }

    public static abstract class ChannelCallback {
        public void onChannelOpened(@NonNull Channel channel) {}
        public void onChannelClosed(@NonNull Channel channel, int closeReason, int appSpecificErrorCode) {}
        public void onInputClosed(@NonNull Channel channel, int closeReason, int appSpecificErrorCode) {}
        public void onOutputClosed(@NonNull Channel channel, int closeReason, int appSpecificErrorCode) {}
    }

    public ChannelClient(@NonNull Context context, @NonNull Wearable.WearableOptions options) {
        super(context, Wearable.API, options, Settings.DEFAULT_SETTINGS);
    }

    public ChannelClient(@NonNull Activity activity, @NonNull Wearable.WearableOptions options) {
        super(activity, Wearable.API, options, Settings.DEFAULT_SETTINGS);
    }

    @NonNull
    public abstract Task<Channel> openChannel(@NonNull String nodeId, @NonNull String path);

    @NonNull
    public abstract Task<Void> close(@NonNull Channel channel);

    @NonNull
    public abstract Task<Void> close(@NonNull Channel channel, int errorCode);

    @NonNull
    public abstract Task<InputStream> getInputStream(@NonNull Channel channel);

    @NonNull
    public abstract Task<OutputStream> getOutputStream(@NonNull Channel channel);

    @NonNull
    public abstract Task<Void> registerChannelCallback(@NonNull ChannelCallback callback);

    @NonNull
    public abstract Task<Boolean> unregisterChannelCallback(@NonNull ChannelCallback callback);
}
