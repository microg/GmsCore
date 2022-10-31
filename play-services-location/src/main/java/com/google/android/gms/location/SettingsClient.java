/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.HasApiKey;

/**
 * The main entry point for interacting with the location settings-enabler APIs.
 * <p>
 * This API makes it easy for an app to ensure that the device's system settings are properly configured for the app's
 * location needs.
 */
public interface SettingsClient extends HasApiKey<Api.ApiOptions.NoOptions> {
}
