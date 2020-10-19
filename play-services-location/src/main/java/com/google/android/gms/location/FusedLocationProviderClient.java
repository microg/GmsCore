/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.location;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import org.jetbrains.annotations.Nullable;
import org.microg.gms.common.PublicApi;
import org.microg.gms.common.api.InstantGoogleApiCall;
import org.microg.gms.common.api.PendingGoogleApiCall;
import org.microg.gms.location.LocationClientImpl;

@PublicApi
public class FusedLocationProviderClient extends GoogleApi<Api.ApiOptions.NoOptions> {
    @PublicApi(exclude = true)
    public FusedLocationProviderClient(Context context) {
        super(context, LocationServices.API);
    }

    public Task<Void> flushLocations() {
        return scheduleTask((PendingGoogleApiCall<Void, LocationClientImpl>) (client, completionSource) -> completionSource.setResult(null));
    }

    public Task<Location> getLastLocation() {

        return scheduleTask(new InstantGoogleApiCall<Location, LocationClientImpl>() {

            @Override
            public void execute(LocationClientImpl client, @Nullable TaskCompletionSource<Location> completionSource) {
                try {
                    completionSource.setResult(execute(client));
                } catch (Exception e) {
                    completionSource.setException(e);
                }
            }

            @Override
            public Location execute(LocationClientImpl client) throws Exception {
                return client.getLastLocation();
            }
        });
    }


}
