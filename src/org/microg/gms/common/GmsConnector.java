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

import com.google.android.gms.common.api.Api;

import org.microg.gms.common.api.AbstractPendingResult;
import org.microg.gms.common.api.ApiConnection;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;

import org.microg.gms.common.api.GoogleApiClientImpl;

public class GmsConnector {
    public static <C extends ApiConnection, R extends Result, O extends Api.ApiOptions>
    AbstractPendingResult<R> connect(GoogleApiClient apiClient, Api<O> api, Callback<C, R> callback) {
        Looper looper = ((GoogleApiClientImpl) apiClient).getLooper();
        final AbstractPendingResult<R> result = new AbstractPendingResult<>(looper);
        Message msg = new Message();
        msg.obj = new ConnectRequest<C, R, O>((GoogleApiClientImpl) apiClient, api, result, callback);
        new Handler<C, R, O>(looper).sendMessage(msg);
        return result;
    }

    public static interface Callback<C, R> {
        public R onClientAvailable(C client) throws RemoteException;
    }

    private static class Handler<C extends ApiConnection, R extends Result, O extends Api.ApiOptions> extends android.os.Handler {
        private Handler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            ConnectRequest<C, R, O> request = (ConnectRequest<C, R, O>) msg.obj;
            ApiConnection apiConnection = request.apiClient.getApiConnection(request.api);
            apiConnection.connect();
            try {
                request.result.setResult(request.callback.onClientAvailable((C) apiConnection));
            } catch (RemoteException ignored) {

            }
        }
    }

    private static class ConnectRequest<C extends ApiConnection, R extends Result, O extends Api.ApiOptions> {
        GoogleApiClientImpl apiClient;
        Api<O> api;
        AbstractPendingResult<R> result;
        Callback<C, R> callback;

        private ConnectRequest(GoogleApiClientImpl apiClient, Api<O> api, AbstractPendingResult<R> result, Callback<C, R> callback) {
            this.apiClient = apiClient;
            this.api = api;
            this.result = result;
            this.callback = callback;
        }
    }
}
