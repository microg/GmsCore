/*
 * Copyright (C) 2013-2025 microG Project Team
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

import android.content.Context;
import com.google.android.gms.common.api.Api;

/**
import org.microg.gms.common.PublicApi;
 */
public class Wearable {
    public static final Api<Wearable.WearableOptions> API = new Api<Wearable.WearableOptions>("Wearable.API", null, null);
    public static final Api<Wearable.WearableOptions> WEARABLE_API = API;

    public static class WearableOptions implements Api.ApiOptions.NoOptions {
    }

    public static NodeApi getNodeApi(Context context) {
        return new NodeApiImpl();
    }
}
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

    public static class WearableOptions implements Api.ApiOptions.Optional {
        /**
         * Special option for microG to allow implementation of a FOSS first party Android Wear app
         */
        @PublicApi(exclude = true)
        public boolean firstPartyMode = false;

        public static class Builder {
            public WearableOptions build() {
                return new WearableOptions();
            }
        }
    }
}
