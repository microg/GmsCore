/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location;

import android.content.Context;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.SettingsClient;

public class SettingsClientImpl extends GoogleApi<Api.ApiOptions.NoOptions> implements SettingsClient {
    public SettingsClientImpl(Context context) {
        super(context, LocationServices.API);
    }
}
