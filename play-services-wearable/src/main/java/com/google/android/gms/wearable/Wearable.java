/*
 * Copyright (C) 2013-2026 microG Project Team
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;

import org.microg.gms.common.PublicApi;
import org.microg.gms.wearable.CapabilityClientImpl;
import org.microg.gms.wearable.ChannelClientImpl;
import org.microg.gms.wearable.DataApiImpl;
import org.microg.gms.wearable.DataClientImpl;
import org.microg.gms.wearable.MessageApiImpl;
import org.microg.gms.wearable.MessageClientImpl;
import org.microg.gms.wearable.NodeApiImpl;
import org.microg.gms.wearable.NodeClientImpl;
import org.microg.gms.wearable.WearableApiClientBuilder;

/**
 * An API for the Android Wear platform.
 */
@PublicApi
public class Wearable {
    /**
     * Token to pass to {@link GoogleApiClient.Builder#addApi(Api)} to enable the Wearable features.
     */
    public static final Api<WearableOptions> API = new Api<WearableOptions>(new WearableApiClientBuilder());

    public static final DataApi DataApi = new DataApiImpl();
    public static final MessageApi MessageApi = new MessageApiImpl();
    public static final NodeApi NodeApi = new NodeApiImpl();

    private static WearableOptions orDefault(@Nullable WearableOptions options) {
        return options != null ? options : new WearableOptions();
    }

    /*
     * DataClient
     */
    @NonNull
    public static DataClient getDataClient(@NonNull Context context) {
        return getDataClient(context, null);
    }

    @NonNull
    public static DataClient getDataClient(@NonNull Context context, @Nullable WearableOptions options) {
        return new DataClientImpl(context, orDefault(options));
    }

    @NonNull
    public static DataClient getDataClient(@NonNull Activity activity) {
        return getDataClient(activity, null);
    }

    @NonNull
    public static DataClient getDataClient(@NonNull Activity activity, @Nullable WearableOptions options) {
        return new DataClientImpl(activity, orDefault(options));
    }

    /*
     * MessageClient
     */
    @NonNull
    public static MessageClient getMessageClient(@NonNull Context context) {
        return getMessageClient(context, null);
    }

    @NonNull
    public static MessageClient getMessageClient(@NonNull Context context, @Nullable WearableOptions options) {
        return new MessageClientImpl(context, orDefault(options));
    }

    @NonNull
    public static MessageClient getMessageClient(@NonNull Activity activity) {
        return getMessageClient(activity, null);
    }

    @NonNull
    public static MessageClient getMessageClient(@NonNull Activity activity, @Nullable WearableOptions options) {
        return new MessageClientImpl(activity, orDefault(options));
    }

    /*
     * NodeClient
     */
    @NonNull
    public static NodeClient getNodeClient(@NonNull Context context) {
        return getNodeClient(context, null);
    }

    @NonNull
    public static NodeClient getNodeClient(@NonNull Context context, @Nullable WearableOptions options) {
        return new NodeClientImpl(context, orDefault(options));
    }

    @NonNull
    public static NodeClient getNodeClient(@NonNull Activity activity) {
        return getNodeClient(activity, null);
    }

    @NonNull
    public static NodeClient getNodeClient(@NonNull Activity activity, @Nullable WearableOptions options) {
        return new NodeClientImpl(activity, orDefault(options));
    }

    /*
     * CapabilityClient
     */
    @NonNull
    public static CapabilityClient getCapabilityClient(@NonNull Context context) {
        return getCapabilityClient(context, null);
    }

    @NonNull
    public static CapabilityClient getCapabilityClient(@NonNull Context context, @Nullable WearableOptions options) {
        return new CapabilityClientImpl(context, orDefault(options));
    }

    @NonNull
    public static CapabilityClient getCapabilityClient(@NonNull Activity activity) {
        return getCapabilityClient(activity, null);
    }

    @NonNull
    public static CapabilityClient getCapabilityClient(@NonNull Activity activity, @Nullable WearableOptions options) {
        return new CapabilityClientImpl(activity, orDefault(options));
    }

    /*
     * ChannelClient
     */
    @NonNull
    public static ChannelClient getChannelClient(@NonNull Context context) {
        return getChannelClient(context, null);
    }

    @NonNull
    public static ChannelClient getChannelClient(@NonNull Context context, @Nullable WearableOptions options) {
        return new ChannelClientImpl(context, orDefault(options));
    }

    @NonNull
    public static ChannelClient getChannelClient(@NonNull Activity activity) {
        return getChannelClient(activity, null);
    }

    @NonNull
    public static ChannelClient getChannelClient(@NonNull Activity activity, @Nullable WearableOptions options) {
        return new ChannelClientImpl(activity, orDefault(options));
    }

    public static class WearableOptions implements Api.ApiOptions.Optional {
        /**
         * Special option for microG to allow implementation of a FOSS first party Android Wear app
         */
        @PublicApi(exclude = true)
        public boolean firstPartyMode = false;

        public static class Builder {
            private boolean firstPartyMode = false;

            public Builder setFirstPartyMode(boolean firstPartyMode) {
                this.firstPartyMode = firstPartyMode;
                return this;
            }

            public WearableOptions build() {
                WearableOptions options = new WearableOptions();
                options.firstPartyMode = this.firstPartyMode;
                return options;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WearableOptions that = (WearableOptions) o;
            return firstPartyMode == that.firstPartyMode;
        }

        @Override
        public int hashCode() {
            return firstPartyMode ? 1 : 0;
        }
    }
}
