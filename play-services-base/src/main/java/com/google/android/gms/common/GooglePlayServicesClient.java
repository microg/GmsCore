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

package com.google.android.gms.common;

import android.os.Bundle;

@Deprecated
public interface GooglePlayServicesClient {
    void connect();

    void disconnect();

    boolean isConnected();

    boolean isConnecting();

    void registerConnectionCallbacks(ConnectionCallbacks listener);

    boolean isConnectionCallbacksRegistered(ConnectionCallbacks listener);

    void unregisterConnectionCallbacks(ConnectionCallbacks listener);

    void registerConnectionFailedListener(OnConnectionFailedListener listener);

    boolean isConnectionFailedListenerRegistered(OnConnectionFailedListener listener);

    void unregisterConnectionFailedListener(OnConnectionFailedListener listener);

    @Deprecated
    interface OnConnectionFailedListener {

        void onConnectionFailed(ConnectionResult result);
    }

    @Deprecated
    interface ConnectionCallbacks {

        void onConnected(Bundle connectionHint);

        void onDisconnected();
    }
}
