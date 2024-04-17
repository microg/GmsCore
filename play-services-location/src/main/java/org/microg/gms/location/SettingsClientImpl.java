/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location;

import android.content.Context;

import android.os.RemoteException;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.location.*;
import com.google.android.gms.location.internal.ISettingsCallbacks;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import org.microg.gms.common.api.PendingGoogleApiCall;

public class SettingsClientImpl extends GoogleApi<Api.ApiOptions.NoOptions> implements SettingsClient {
    public SettingsClientImpl(Context context) {
        super(context, LocationServices.API);
    }

    @Override
    public Task<LocationSettingsResponse> checkLocationSettings(LocationSettingsRequest locationSettingsRequest) {
        return scheduleTask((PendingGoogleApiCall<LocationSettingsResponse, LocationClientImpl>) (client, completionSource) -> client.getServiceInterface().requestLocationSettingsDialog(locationSettingsRequest, new ISettingsCallbacks.Stub() {
            @Override
            public void onLocationSettingsResult(LocationSettingsResult result) {
                completionSource.setResult(new LocationSettingsResponse(result));
            }
        }, null));
    }

    @Override
    public Task<Boolean> isGoogleLocationAccuracyEnabled() {
        return Tasks.forResult(true); // TODO
    }
}
