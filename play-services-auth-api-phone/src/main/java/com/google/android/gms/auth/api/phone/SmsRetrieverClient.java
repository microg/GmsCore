/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.phone;

import android.content.Context;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.GoogleApiClient;
import org.microg.gms.auth.api.phone.SmsRetrieverApiClient;

/**
 * The main entry point for interacting with SmsRetriever.
 * <p>
 * This does not require a {@link GoogleApiClient}. See {@link GoogleApi} for more information.
 */
public abstract class SmsRetrieverClient extends GoogleApi<Api.ApiOptions.NoOptions> implements SmsRetrieverApi {

    protected SmsRetrieverClient(Context context) {
        super(context, SmsRetrieverApiClient.API);
    }
}
