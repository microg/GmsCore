/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.droidguard.DroidGuardClient;
import com.google.android.gms.droidguard.DroidGuardHandle;
import com.google.android.gms.droidguard.internal.DroidGuardResultsRequest;
import com.google.android.gms.tasks.Task;

import org.microg.gms.common.api.ReturningGoogleApiCall;

import java.util.Map;

public class DroidGuardClientImpl extends GoogleApi<DroidGuardClientImpl.Options> implements DroidGuardClient {
    private static final Api<Options> API = new Api<>((options, context, looper, clientSettings, callbacks, connectionFailedListener) -> {
        DroidGuardApiClient client = new DroidGuardApiClient(context, callbacks, connectionFailedListener);
        if (options != null && options.packageName != null) client.setPackageName(options.packageName);
        return client;
    });

    public DroidGuardClientImpl(Context context) {
        super(context, API);
    }
    public DroidGuardClientImpl(Context context, String packageName) {
        super(context, API, new Options(packageName));
    }

    @Override
    @NonNull
    public Task<DroidGuardHandle> init(@NonNull String flow, @Nullable DroidGuardResultsRequest request) {
        DroidGuardResultsRequest finalRequest = request != null ? request : new DroidGuardResultsRequest();
        return scheduleTask((ReturningGoogleApiCall<DroidGuardHandle, DroidGuardApiClient>) client -> client.openHandle(flow, finalRequest));
    }

    @Override
    @NonNull
    public Task<String> getResults(@NonNull String flow, @Nullable Map<String, String> data, @Nullable DroidGuardResultsRequest request) {
        DroidGuardResultsRequest finalRequest = request != null ? request : new DroidGuardResultsRequest();
        return scheduleTask((ReturningGoogleApiCall<String, DroidGuardApiClient>) client -> {
            DroidGuardHandle handle = client.openHandle(flow, finalRequest);
            String results = handle.snapshot(data);
            handle.close();
            return results;
        });
    }

    public static class Options implements Api.ApiOptions.Optional {
        public final String packageName;

        public Options(String packageName) {
            this.packageName = packageName;
        }
    }
}
