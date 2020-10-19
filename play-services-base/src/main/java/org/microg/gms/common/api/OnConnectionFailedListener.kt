/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.common.api

import com.google.android.gms.common.ConnectionResult

interface OnConnectionFailedListener {
    /**
     * Called when there was an error connecting the client to the service.
     *
     * @param result A [ConnectionResult] that can be used for resolving the error, and
     * deciding what sort of error occurred. To resolve the error, the resolution
     * must be started from an activity with a non-negative `requestCode`
     * passed to [ConnectionResult.startResolutionForResult].
     * Applications should implement [Activity.onActivityResult] in their
     * Activity to call [.connect] again if the user has resolved the
     * issue (resultCode is [Activity.RESULT_OK]).
     */
    fun onConnectionFailed(result: ConnectionResult?)
}