/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.common.api;

import android.os.Bundle;

import com.google.android.gms.common.api.GoogleApiClient;

public interface ConnectionCallbacks {

    /**
     * After calling {@link #connect()}, this method will be invoked asynchronously when the
     * connect request has successfully completed. After this callback, the application can
     * make requests on other methods provided by the client and expect that no user
     * intervention is required to call methods that use account and scopes provided to the
     * client constructor.
     * <p/>
     * Note that the contents of the {@code connectionHint} Bundle are defined by the specific
     * services. Please see the documentation of the specific implementation of
     * {@link GoogleApiClient} you are using for more information.
     *
     * @param connectionHint Bundle of data provided to clients by Google Play services. May
     *                       be null if no content is provided by the service.
     */
    void onConnected(Bundle connectionHint);

    /**
     * Called when the client is temporarily in a disconnected state. This can happen if there
     * is a problem with the remote service (e.g. a crash or resource problem causes it to be
     * killed by the system). When called, all requests have been canceled and no outstanding
     * listeners will be executed. GoogleApiClient will automatically attempt to restore the
     * connection. Applications should disable UI components that require the service, and wait
     * for a call to {@link #onConnected(Bundle)} to re-enable them.
     *
     * @param cause The reason for the disconnection. Defined by constants {@code CAUSE_*}.
     */
    void onConnectionSuspended(int cause);
}
