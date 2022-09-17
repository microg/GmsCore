/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.safetynet;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.common.api.Api;

import org.microg.gms.common.PublicApi;
import org.microg.gms.safetynet.SafetyNetApiImpl;
import org.microg.gms.safetynet.SafetyNetGmsClient;

/**
 * The SafetyNet API provides access to Google services that help you assess the health and safety of an Android device.
 * <p>
 * To use SafetyNet, call {@link #getClient(Context)} or {@link #getClient(Activity)}.
 */
@PublicApi
public class SafetyNet {
    /**
     * The API necessary to use SafetyNet.
     *
     * @deprecated use {@link #getClient(Context)} or {@link #getClient(Activity)}.
     */
    @Deprecated
    public static final Api<Api.ApiOptions.NoOptions> API = new Api<>((options, context, looper, clientSettings, callbacks, connectionFailedListener) -> new SafetyNetGmsClient(context, callbacks, connectionFailedListener));;

    /**
     * The entry point for interacting with the SafetyNet APIs which help assess the health and safety of an Android device.
     *
     * @deprecated use {@link #getClient(Context)} or {@link #getClient(Activity)}.
     */
    @Deprecated
    public static final SafetyNetApi SafetyNetApi = new SafetyNetApiImpl();

    /**
     * Returns a {@link SafetyNetClient} that is used to access all APIs that are called when the app has a
     * foreground {@link Activity}.
     * <p>
     * Use this method over {@link #getClient(Context)} if your app has a foreground Activity and you will be making
     * multiple API calls to improve performance.
     */
    public static SafetyNetClient getClient(Activity activity) {
        return new SafetyNetClient(activity);
    }

    /**
     * Returns a {@link SafetyNetClient} that is used to access all APIs that are called without access to a
     * foreground {@link Activity}.
     */
    public static SafetyNetClient getClient(Context context) {
        return new SafetyNetClient(context);
    }
}
