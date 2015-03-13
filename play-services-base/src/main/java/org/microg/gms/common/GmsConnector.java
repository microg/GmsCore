/*
 * Copyright 2014-2015 µg Project Team
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

import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;

import org.microg.gms.common.api.AbstractPendingResult;
import org.microg.gms.common.api.ApiConnection;
import org.microg.gms.common.api.GoogleApiClientImpl;

public class GmsConnector<C extends ApiConnection, R extends Result, O extends Api.ApiOptions> {
    private static final String TAG = "GmsConnector";

    private final GoogleApiClientImpl apiClient;
    private final Api<O> api;
    private final Callback<C, R> callback;

    public GmsConnector(GoogleApiClient apiClient, Api<O> api, Callback<C, R> callback) {
        this.apiClient = (GoogleApiClientImpl) apiClient;
        this.api = api;
        this.callback = callback;
    }


    public AbstractPendingResult<R> connect() {
        Log.d(TAG, "connect()");
        apiClient.getApiConnection(api);
        Looper looper = apiClient.getLooper();
        final AbstractPendingResult<R> result = new AbstractPendingResult<>(looper);
        Message msg = new Message();
        msg.obj = result;
        new Handler(looper).sendMessage(msg);
        return result;
    }

    public static interface Callback<C, R> {
        public R onClientAvailable(C client) throws RemoteException;
    }

    private class Handler extends android.os.Handler {
        private Handler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "Handler : handleMessage");
            AbstractPendingResult<R> result = (AbstractPendingResult<R>) msg.obj;
            try {
                C connection = (C)apiClient.getApiConnection(api);
                result.deliverResult(callback.onClientAvailable(connection));
            } catch (RemoteException ignored) {

            }
        }
    }
}
