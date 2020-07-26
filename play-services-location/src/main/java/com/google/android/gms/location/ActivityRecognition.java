/*
 * Copyright (C) 2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.location;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient.Builder;

import org.microg.gms.location.ActivityRecognitionApiClientBuilder;
import org.microg.gms.location.ActivityRecognitionApiImpl;

/**
 * The main entry point for activity recognition integration.
 */
public class ActivityRecognition {
    public static final String CLIENT_NAME = "activity_recognition";

    /**
     * Token to pass to {@link Builder#addApi(Api)} to enable ContextServices.
     */
    public static final Api<Api.ApiOptions.NoOptions> API = new Api<Api.ApiOptions.NoOptions>(new ActivityRecognitionApiClientBuilder());

    /**
     * Entry point to the activity recognition APIs.
     */
    public static final ActivityRecognitionApi ActivityRecognitionApi = new ActivityRecognitionApiImpl();
}
