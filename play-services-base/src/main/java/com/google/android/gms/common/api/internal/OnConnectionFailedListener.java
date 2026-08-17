/*
 * SPDX-FileCopyrightText: 2020 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.api.internal;

import android.app.Activity;

import com.google.android.gms.common.ConnectionResult;
import org.microg.gms.common.Hide;

@Hide
public interface OnConnectionFailedListener {
    /**
     * Called when there was an error connecting the client to the service.
     *
     * @param result A {@link ConnectionResult} that can be used for resolving the error, and
     *               deciding what sort of error occurred. To resolve the error, the resolution
     *               must be started from an activity with a non-negative {@code requestCode}
     *               passed to {@link ConnectionResult#startResolutionForResult(Activity, int)}.
     *               Applications should implement {@link Activity#onActivityResult} in their
     *               Activity to call {@link #connect()} again if the user has resolved the
     *               issue (resultCode is {@link Activity#RESULT_OK}).
     */
    void onConnectionFailed(ConnectionResult result);
}
