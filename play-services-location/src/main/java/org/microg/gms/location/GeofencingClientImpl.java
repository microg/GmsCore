/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location;

import android.app.PendingIntent;
import android.content.Context;

import androidx.annotation.NonNull;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

import java.util.List;

public class GeofencingClientImpl extends GoogleApi<Api.ApiOptions.NoOptions> implements GeofencingClient {
    public GeofencingClientImpl(Context context) {
        super(context, LocationServices.API);
    }

    @NonNull
    @Override
    public Task<Void> addGeofences(GeofencingRequest geofencingRequest, PendingIntent pendingIntent) {
        return null;
    }

    @NonNull
    @Override
    public Task<Void> removeGeofences(List<String> geofenceRequestIds) {
        return null;
    }

    @NonNull
    @Override
    public Task<Void> removeGeofences(PendingIntent pendingIntent) {
        return null;
    }
}
