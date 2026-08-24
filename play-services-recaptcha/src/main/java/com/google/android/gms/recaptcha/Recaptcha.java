/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.recaptcha;

import android.app.Activity;
import android.content.Context;

import org.microg.gms.recaptcha.RecaptchaClientImpl;

/**
 * The reCAPTCHA API provides access to Google Cloud services that help you protect your app from spam and other
 * abusive actions.
 * <p>
 * To instantiate a reCAPTCHA mobile client, call {@link #getClient(Context)} or {@link #getClient(Activity)}.
 */
public class Recaptcha {

    /**
     * Returns a {@link RecaptchaClient} that is used to access all APIs that are called when the app has a foreground
     * {@link Activity}.
     * <p>
     * Use this method over {@link #getClient(Context)} to improve performance if you plan to make multiple API calls
     * from your application's foreground {@link Activity}.
     */
    public static RecaptchaClient getClient(Activity activity) {
        return new RecaptchaClientImpl(activity);
    }

    /**
     * Returns a {@link RecaptchaClient} that is used to access all APIs that are called without access to a foreground
     * {@link Activity}.
     */
    public static RecaptchaClient getClient(Context context) {
        return new RecaptchaClientImpl(context);
    }
}
