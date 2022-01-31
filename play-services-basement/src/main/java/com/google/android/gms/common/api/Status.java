/*
 * Copyright (C) 2013-2017 microG Project Team
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

package com.google.android.gms.common.api;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

/**
 * Represents the results of work.
 */
@PublicApi
public final class Status extends AutoSafeParcelable implements Result {
    @PublicApi(exclude = true)
    public static final Status INTERNAL_ERROR = new Status(CommonStatusCodes.INTERNAL_ERROR, "Internal error");
    @PublicApi(exclude = true)
    public static final Status CANCELED = new Status(CommonStatusCodes.CANCELED, "Cancelled");
    @PublicApi(exclude = true)
    public static final Status SUCCESS = new Status(CommonStatusCodes.SUCCESS, "Success");

    @SafeParceled(1000)
    private final int versionCode = 1;

    @SafeParceled(1)
    private final int statusCode;

    @SafeParceled(2)
    private final String statusMessage;

    @SafeParceled(3)
    private final PendingIntent resolution;

    private Status() {
        statusCode = 0;
        statusMessage = null;
        resolution = null;
    }

    /**
     * Creates a representation of the status resulting from a GoogleApiClient operation.
     *
     * @param statusCode The status code.
     */
    public Status(int statusCode) {
        this(statusCode, null);
    }

    /**
     * Creates a representation of the status resulting from a GoogleApiClient operation.
     *
     * @param statusCode    The status code.
     * @param statusMessage The message associated with this status, or null.
     */
    public Status(int statusCode, String statusMessage) {
        this(statusCode, statusMessage, null);
    }

    /**
     * Creates a representation of the status resulting from a GoogleApiClient operation.
     *
     * @param statusCode    The status code.
     * @param statusMessage The message associated with this status, or null.
     * @param resolution    A pending intent that will resolve the issue when started, or null.
     */
    public Status(int statusCode, String statusMessage, PendingIntent resolution) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.resolution = resolution;
    }

    /**
     * A pending intent to resolve the failure. This intent can be started with
     * {@link Activity#startIntentSenderForResult(IntentSender, int, Intent, int, int, int)} to
     * present UI to solve the issue.
     *
     * @return The pending intent to resolve the failure.
     */
    public PendingIntent getResolution() {
        return resolution;
    }

    /**
     * Returns the status of this result. Use {@link #isSuccess()} to determine whether the call
     * was successful, and {@link #getStatusCode()} to determine what the error cause was.
     * <p>
     * Certain errors are due to failures that can be resolved by launching a particular intent.
     * The resolution intent is available via {@link #getResolution()}.
     */
    @Override
    public Status getStatus() {
        return this;
    }

    /**
     * Indicates the status of the operation.
     *
     * @return Status code resulting from the operation. The value is one of the constants in
     * {@link CommonStatusCodes} or specific to the APIs added to the GoogleApiClient.
     */
    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    /**
     * Returns true if calling {@link #startResolutionForResult(Activity, int)} will start any
     * intents requiring user interaction.
     *
     * @return true if there is a resolution that can be started.
     */
    public boolean hasResolution() {
        return resolution != null;
    }

    /**
     * Returns true if the operation was canceled.
     */
    public boolean isCanceled() {
        return statusCode == CommonStatusCodes.CANCELED;
    }

    /**
     * Returns true if the operation was interrupted.
     */
    public boolean isInterrupted() {
        return statusCode == CommonStatusCodes.INTERRUPTED;
    }

    /**
     * Returns true if the operation was successful.
     *
     * @return true if the operation was successful, false if there was an error.
     */
    public boolean isSuccess() {
        return statusCode <= 0;
    }

    /**
     * Resolves an error by starting any intents requiring user interaction. See
     * {@link CommonStatusCodes#SIGN_IN_REQUIRED}, and {@link CommonStatusCodes#RESOLUTION_REQUIRED}.
     *
     * @param activity    An Activity context to use to resolve the issue. The activity's
     *                    onActivityResult method will be invoked after the user is done. If the
     *                    resultCode is {@link Activity#RESULT_OK}, the application should try to
     *                    connect again.
     * @param requestCode The request code to pass to onActivityResult.
     * @throws SendIntentException If the resolution intent has been canceled or is no longer able
     *                             to execute the request.
     */
    public void startResolutionForResult(Activity activity, int requestCode) throws SendIntentException {
        if (hasResolution()) {
            activity.startIntentSenderForResult(resolution.getIntentSender(), requestCode, null, 0, 0, 0);
        }
    }

    public static final Creator<Status> CREATOR = new AutoCreator<>(Status.class);
}
