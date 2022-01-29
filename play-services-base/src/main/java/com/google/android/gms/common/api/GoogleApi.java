/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.api;

import android.content.Context;

import com.google.android.gms.common.api.internal.ApiKey;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import org.microg.gms.common.PublicApi;
import org.microg.gms.common.api.ApiClient;
import org.microg.gms.common.api.GoogleApiManager;
import org.microg.gms.common.api.PendingGoogleApiCall;

@PublicApi
public abstract class GoogleApi<O extends Api.ApiOptions> implements HasApiKey<O> {
    private final GoogleApiManager manager;
    @PublicApi(exclude = true)
    public Api<O> api;

    @PublicApi(exclude = true)
    protected GoogleApi(Context context, Api<O> api) {
        this.api = api;
        this.manager = GoogleApiManager.getInstance(context);
    }

    @PublicApi(exclude = true)
    protected <R, A extends ApiClient> Task<R> scheduleTask(PendingGoogleApiCall<R, A> apiCall) {
        TaskCompletionSource<R> completionSource = new TaskCompletionSource<>();
        manager.scheduleTask(this, apiCall, completionSource);
        return completionSource.getTask();
    }

    @Override
    @PublicApi(exclude = true)
    public ApiKey<O> getApiKey() {
        return null;
    }

    @PublicApi(exclude = true)
    public O getOptions() {
        return null;
    }
}
