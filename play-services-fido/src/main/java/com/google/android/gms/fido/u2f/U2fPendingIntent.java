/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.u2f;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.IntentSender;

import com.google.android.gms.fido.Fido;

/**
 * Interface for apps to launch a {@link PendingIntent}.
 *
 * @deprecated Please use {@link Fido} APIs instead.
 */
public interface U2fPendingIntent {
    /**
     * Returns true if an {@link Activity} has a {@link PendingIntent}.
     */
    boolean hasPendingIntent();

    /**
     * Launches the PendingIntent.
     *
     * @param activity    An Activity context to use to launch the intent. The activity's onActivityResult method will
     *                    be invoked after the user is done.
     * @param requestCode The request code to pass to onActivityResult.
     * @throws IllegalStateException            if hasPendingIntent is false
     * @throws IntentSender.SendIntentException If the resolution intent has been canceled or is no longer able to
     *                                          execute the request.
     */
    void launchPendingIntent(Activity activity, int requestCode);
}
