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

package org.microg.gms.common;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.api.GoogleApiClient;

public final class ForwardConnectionFailedListener
        implements GoogleApiClient.OnConnectionFailedListener {
    private final GooglePlayServicesClient.OnConnectionFailedListener listener;

    public ForwardConnectionFailedListener(
            GooglePlayServicesClient.OnConnectionFailedListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ForwardConnectionFailedListener &&
                listener.equals(((ForwardConnectionFailedListener) o).listener);
    }

    @Override
    public int hashCode() {
        return listener.hashCode();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        listener.onConnectionFailed(result);
    }
}
