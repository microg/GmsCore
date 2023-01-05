/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard;

import android.content.Context;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.droidguard.DroidGuardClient;
import com.google.android.gms.droidguard.DroidGuardHandle;
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest;
import com.google.android.gms.tasks.Task;

import org.microg.gms.common.api.ReturningGoogleApiCall;

import java.util.Map;

public class DroidGuardClientImpl extends GoogleApi<Api.ApiOptions.NoOptions> implements DroidGuardClient {
    private static final Api<Api.ApiOptions.NoOptions> API = new Api<>((options, context, looper, clientSettings, callbacks, connectionFailedListener) -> new DroidGuardApiClient(context, callbacks, connectionFailedListener));

    public DroidGuardClientImpl(Context context) {
        super(context, API);
    }

    @Override
    public Task<DroidGuardHandle> init(String flow, DroidGuardResultsRequest request) {
        DroidGuardResultsRequest finalRequest = request != null ? request : new DroidGuardResultsRequest();
        return scheduleTask((ReturningGoogleApiCall<DroidGuardHandle, DroidGuardApiClient>) client -> client.openHandle(flow, finalRequest));
    }

    @Override
    public Task<String> getResults(String flow, Map<String, String> data, DroidGuardResultsRequest request) {
        DroidGuardResultsRequest finalRequest = request != null ? request : new DroidGuardResultsRequest();
        return scheduleTask((ReturningGoogleApiCall<String, DroidGuardApiClient>) client -> {
            DroidGuardHandle handle = client.openHandle(flow, finalRequest);
            String results = handle.snapshot(data);
            handle.close();
            return results;
        });
    }
}
