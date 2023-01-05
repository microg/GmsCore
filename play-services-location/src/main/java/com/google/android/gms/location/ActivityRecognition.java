/*
 * SPDX-FileCopyrightText: 2017 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.location;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;

import org.microg.gms.common.PublicApi;
import org.microg.gms.location.ActivityRecognitionApiClientBuilder;
import org.microg.gms.location.ActivityRecognitionApiImpl;

/**
 * The main entry point for activity recognition integration.
 */
@PublicApi
public class ActivityRecognition {
    public static final String CLIENT_NAME = "activity_recognition";

    /**
     * Token to pass to {@link GoogleApiClient.Builder#addApi(Api)} to enable ActivityRecognition.
     *
     * @deprecated Use {@link ActivityRecognitionClient} instead.
     */
    public static final Api<Api.ApiOptions.NoOptions> API = new Api<Api.ApiOptions.NoOptions>(new ActivityRecognitionApiClientBuilder());

    /**
     * Entry point to the activity recognition APIs.
     *
     * @deprecated Use {@link ActivityRecognitionClient} instead.
     */
    public static final ActivityRecognitionApi ActivityRecognitionApi = new ActivityRecognitionApiImpl();

    /**
     * Create a new instance of {@link ActivityRecognitionClient} for use in an {@link Activity}.
     */
    public static ActivityRecognitionClient getClient (Activity activity) {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a new instance of {@link ActivityRecognitionClient} for use in a non-activity {@link Context}.
     */
    public static ActivityRecognitionClient getClient (Context context) {
        throw new UnsupportedOperationException();
    }
}
