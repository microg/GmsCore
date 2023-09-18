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

package org.microg.gms.common.api;

import android.content.Context;
import android.os.Looper;

import com.google.android.gms.common.api.Api;

public interface ApiClientBuilder<O extends Api.ApiOptions> {
    Api.Client build(O options, Context context, Looper looper, ApiClientSettings clientSettings, ConnectionCallbacks callbacks, OnConnectionFailedListener connectionFailedListener);
}
