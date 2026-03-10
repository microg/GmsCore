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

package com.google.android.gms.cast;

import android.view.Display;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;

import org.microg.gms.cast.CastRemoteDisplayApiClientBuilder;
import org.microg.gms.cast.CastRemoteDisplayApiImpl;
import org.microg.gms.common.PublicApi;

@PublicApi
public final class CastRemoteDisplay {
    /**
     * Token to pass to {@link GoogleApiClient.Builder#addApi(Api)} to enable the CastRemoteDisplay features.
     */
    public static final Api<CastRemoteDisplayOptions> API = new Api<CastRemoteDisplayOptions>(new CastRemoteDisplayApiClientBuilder());

    /**
     * An implementation of the CastRemoteDisplayAPI interface. The interface is used to interact with a cast device.
     */
    public static final CastRemoteDisplayApi CastApi = new CastRemoteDisplayApiImpl();

    private CastRemoteDisplay() {
    }

    public static final class CastRemoteDisplayOptions implements Api.ApiOptions.HasOptions {
        private CastDevice castDevice;
        private CastRemoteDisplaySessionCallbacks callbacks;

        private CastRemoteDisplayOptions(CastDevice castDevice, CastRemoteDisplaySessionCallbacks callbacks) {
            this.castDevice = castDevice;
            this.callbacks = callbacks;
        }

        public static final class Builder {
            private CastDevice castDevice;
            private CastRemoteDisplaySessionCallbacks callbacks;

            public Builder(CastDevice castDevice, CastRemoteDisplaySessionCallbacks callbacks) {
                this.castDevice = castDevice;
                this.callbacks = callbacks;
            }

            public CastRemoteDisplayOptions build() {
                return new CastRemoteDisplayOptions(castDevice, callbacks);
            }
        }
    }

    public interface CastRemoteDisplaySessionCallbacks {
        void onRemoteDisplayEnded(Status status);
    }

    public interface CastRemoteDisplaySessionResult extends Result {
        Display getPresentationDisplay();
    }
}
