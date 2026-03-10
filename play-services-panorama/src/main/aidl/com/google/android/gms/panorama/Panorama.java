/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.panorama;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;

/**
 * The main entry point for panorama integration.
 */
@Deprecated
public class Panorama {
    /**
     * Token to pass to {@link GoogleApiClient.Builder#addApi(Api)} to enable the Panorama features.
     */
    @Deprecated
    public static final Api<Api.ApiOptions.NoOptions> API = null;

    /**
     * The entry point for interacting with the Panorama API.
     */
    @Deprecated
    public static final PanoramaApi PanoramaApi = null;
}
