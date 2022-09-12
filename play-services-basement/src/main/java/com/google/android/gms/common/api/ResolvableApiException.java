/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.api;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;

import org.microg.gms.common.PublicApi;

/**
 * Exception to be returned by a Task when a call to Google Play services has failed with a
 * possible resolution.
 */
@PublicApi
public class ResolvableApiException extends ApiException {
    @PublicApi
    public ResolvableApiException(Status status) {
        super(status);
    }

    /**
     * A pending intent to resolve the failure. This intent can be started with
     * {@link android.app.Activity#startIntentSenderForResult(IntentSender, int, Intent, int, int, int)}
     * to present UI to solve the issue.
     * @return The pending intent to resolve the failure.
     */
    @PublicApi
    public PendingIntent getResolution() {
        return mStatus.getResolution();
    }

    /**
     * Resolves an error by starting any intents requiring user interaction.
     * See {@link com.google.android.gms.common.api.CommonStatusCodes#SIGN_IN_REQUIRED}, and
     * {@link com.google.android.gms.common.api.CommonStatusCodes#RESOLUTION_REQUIRED}.
     * @param activity An Activity context to use to resolve the issue. The activity's
     *                 onActivityResult method will be invoked after the user is done.
     *                 If the resultCode is {@link android.app.Activity#RESULT_OK},
     *                 the application should try to connect again.
     * @param requestCode The request code to pass to onActivityResult.
     */
    @PublicApi
    public void startResolutionForResult(Activity activity, int requestCode) throws IntentSender.SendIntentException {
        mStatus.startResolutionForResult(activity, requestCode);
    }
}
