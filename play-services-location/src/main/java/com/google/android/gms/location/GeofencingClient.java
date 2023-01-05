/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import android.app.Activity;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.HasApiKey;

/**
 * The main entry point for interacting with the geofencing APIs.
 * Get an instance of this client via {@link LocationServices#getGeofencingClient(Activity)}.
 * All methods are thread safe.
 */
public interface GeofencingClient extends HasApiKey<Api.ApiOptions.NoOptions> {
}
