/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.common.api;

import android.content.Context;
import android.os.Bundle;
import android.os.DeadObjectException;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleApiManager {
    private static GoogleApiManager instance;
    private Context context;
    private Map<ApiInstance, ApiClient> clientMap = new HashMap<>();
    private Map<ApiInstance, List<WaitingApiCall<?>>> waitingApiCallMap = new HashMap<>();

    private GoogleApiManager(Context context) {
        this.context = context;
    }

    public synchronized static GoogleApiManager getInstance(Context context) {
        if (instance == null) instance = new GoogleApiManager(context);
        return instance;
    }

    private synchronized <O extends Api.ApiOptions, A extends ApiClient> A clientForApi(GoogleApi<O> api) {
        ApiInstance apiInstance = new ApiInstance(api);
        if (clientMap.containsKey(apiInstance)) {
            return (A) clientMap.get(apiInstance);
        } else {
            ApiClient client = api.api.getBuilder().build(api.getOptions(), context, context.getMainLooper(), null, new ConnectionCallback(apiInstance), new ConnectionFailedListener(apiInstance));
            clientMap.put(apiInstance, client);
            waitingApiCallMap.put(apiInstance, new ArrayList<>());
            return (A) client;
        }
    }

    public synchronized <O extends Api.ApiOptions, R, A extends ApiClient> void scheduleTask(GoogleApi<O> api, PendingGoogleApiCall<R, A> apiCall, TaskCompletionSource<R> completionSource) {
        A client = clientForApi(api);
        boolean connecting = client.isConnecting();
        boolean connected = client.isConnected();
        if (connected) {
            apiCall.execute(client, completionSource);
        } else {
            waitingApiCallMap.get(new ApiInstance(api)).add(new WaitingApiCall<R>((PendingGoogleApiCall<R, ApiClient>) apiCall, completionSource));
            if (!connecting) {
                client.connect();
            }
        }
    }

    private synchronized void onInstanceConnected(ApiInstance apiInstance, Bundle connectionHint) {
        List<WaitingApiCall<?>> waitingApiCalls = waitingApiCallMap.get(apiInstance);
        for (WaitingApiCall<?> waitingApiCall : waitingApiCalls) {
            waitingApiCall.execute(clientMap.get(apiInstance));
        }
        waitingApiCalls.clear();
    }

    private synchronized void onInstanceSuspended(ApiInstance apiInstance, int cause) {

    }

    private synchronized void onInstanceFailed(ApiInstance apiInstance, ConnectionResult result) {
        List<WaitingApiCall<?>> waitingApiCalls = waitingApiCallMap.get(apiInstance);
        for (WaitingApiCall<?> waitingApiCall : waitingApiCalls) {
            waitingApiCall.failed(new RuntimeException(result.getErrorMessage()));
        }
        waitingApiCalls.clear();
    }

    private class ConnectionCallback implements ConnectionCallbacks {
        private ApiInstance apiInstance;

        public ConnectionCallback(ApiInstance apiInstance) {
            this.apiInstance = apiInstance;
        }

        @Override
        public void onConnected(Bundle connectionHint) {
            onInstanceConnected(apiInstance, connectionHint);
        }

        @Override
        public void onConnectionSuspended(int cause) {
            onInstanceSuspended(apiInstance, cause);
        }
    }

    private class ConnectionFailedListener implements OnConnectionFailedListener {
        private ApiInstance apiInstance;

        public ConnectionFailedListener(ApiInstance apiInstance) {
            this.apiInstance = apiInstance;
        }

        @Override
        public void onConnectionFailed(ConnectionResult result) {
            onInstanceFailed(apiInstance, result);
        }
    }

    private static class WaitingApiCall<R> {
        private PendingGoogleApiCall<R, ApiClient> apiCall;
        private TaskCompletionSource<R> completionSource;

        public WaitingApiCall(PendingGoogleApiCall<R, ApiClient> apiCall, TaskCompletionSource<R> completionSource) {
            this.apiCall = apiCall;
            this.completionSource = completionSource;
        }

        public void execute(ApiClient client) {
            apiCall.execute(client, completionSource);
        }

        public void failed(Exception e) {
            completionSource.setException(e);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            WaitingApiCall<?> that = (WaitingApiCall<?>) o;

            if (apiCall != null ? !apiCall.equals(that.apiCall) : that.apiCall != null) return false;
            return completionSource != null ? completionSource.equals(that.completionSource) : that.completionSource == null;
        }

        @Override
        public int hashCode() {
            int result = apiCall != null ? apiCall.hashCode() : 0;
            result = 31 * result + (completionSource != null ? completionSource.hashCode() : 0);
            return result;
        }
    }

    private static class ApiInstance {
        private Class<?> apiClass;
        private Api.ApiOptions apiOptions;

        public ApiInstance(Class<?> apiClass, Api.ApiOptions apiOptions) {
            this.apiClass = apiClass;
            this.apiOptions = apiOptions;
        }

        public ApiInstance(GoogleApi<?> api) {
            this(api.getClass(), api.getOptions());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ApiInstance that = (ApiInstance) o;

            if (apiClass != null ? !apiClass.equals(that.apiClass) : that.apiClass != null) return false;
            return apiOptions != null ? apiOptions.equals(that.apiOptions) : that.apiOptions == null;
        }

        @Override
        public int hashCode() {
            int result = apiClass != null ? apiClass.hashCode() : 0;
            result = 31 * result + (apiOptions != null ? apiOptions.hashCode() : 0);
            return result;
        }
    }
}
