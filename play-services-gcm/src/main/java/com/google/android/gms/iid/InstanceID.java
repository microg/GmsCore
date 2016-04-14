/*
 * Copyright 2013-2016 microG Project Team
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

package com.google.android.gms.iid;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;

import org.microg.gms.common.PublicApi;
import org.microg.gms.gcm.GcmConstants;

import java.io.IOException;

/**
 * Instance ID provides a unique identifier for each app instance and a mechanism
 * to authenticate and authorize actions (for example, sending a GCM message).
 * <p/>
 * Instance ID is stable but may become invalid, if:
 * [...]
 * If Instance ID has become invalid, the app can call {@link com.google.android.gms.iid.InstanceID#getId()}
 * to request a new Instance ID.
 * To prove ownership of Instance ID and to allow servers to access data or
 * services associated with the app, call {@link com.google.android.gms.iid.InstanceID#getToken(java.lang.String, java.lang.String)}.
 */
public class InstanceID {
    /**
     * Error returned when failed requests are retried too often.  Use
     * exponential backoff when retrying requests
     */
    public static final String ERROR_BACKOFF = "RETRY_LATER";

    /**
     * Blocking methods must not be called on the main thread.
     */
    public static final String ERROR_MAIN_THREAD = "MAIN_THREAD";

    /**
     * Tokens can't be generated. Only devices with Google Play are supported.
     */
    public static final String ERROR_MISSING_INSTANCEID_SERVICE = "MISSING_INSTANCEID_SERVICE";

    /**
     * The device cannot read the response, or there was a server error.
     * Application should retry the request later using exponential backoff
     * and retry (on each subsequent failure increase delay before retrying).
     */
    public static final String ERROR_SERVICE_NOT_AVAILABLE = GcmConstants.ERROR_SERVICE_NOT_AVAILABLE;

    /**
     * Timeout waiting for a response.
     */
    public static final String ERROR_TIMEOUT = "TIMEOUT";

    /**
     * Resets Instance ID and revokes all tokens.
     *
     * @throws IOException
     */
    public void deleteInstanceID() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Revokes access to a scope (action) for an entity previously
     * authorized by {@link com.google.android.gms.iid.InstanceID#getToken(java.lang.String, java.lang.String)}.
     * <p/>
     * Do not call this function on the main thread.
     *
     * @param authorizedEntity Entity that must no longer have access.
     * @param scope            Action that entity is no longer authorized to perform.
     * @throws IOException if the request fails.
     */
    public void deleteToken(String authorizedEntity, String scope) throws IOException {
        deleteToken(authorizedEntity, scope, new Bundle());
    }

    @PublicApi(exclude = true)
    public void deleteToken(String authorizedEntity, String scope, Bundle extras) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns time when instance ID was created.
     *
     * @return Time when instance ID was created (milliseconds since Epoch).
     */
    public long getCreationTime() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a stable identifier that uniquely identifies the app instance.
     *
     * @return The identifier for the application instance.
     */
    public String getId() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns an instance of this class.
     *
     * @return InstanceID instance.
     */
    public static InstanceID getInstance(Context context) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a token that authorizes an Entity (example: cloud service) to perform
     * an action on behalf of the application identified by Instance ID.
     * <p/>
     * This is similar to an OAuth2 token except, it applies to the
     * application instance instead of a user.
     * <p/>
     * Do not call this function on the main thread.
     *
     * @param authorizedEntity Entity authorized by the token.
     * @param scope            Action authorized for authorizedEntity.
     * @param extras           additional parameters specific to each token scope.
     *                         Bundle keys starting with 'GCM.' and 'GOOGLE.' are
     *                         reserved.
     * @return a token that can identify and authorize the instance of the
     * application on the device.
     * @throws IOException if the request fails.
     */
    public String getToken(String authorizedEntity, String scope, Bundle extras) throws IOException {
        if (Looper.getMainLooper() == Looper.myLooper()) throw new IOException(ERROR_MAIN_THREAD);

        throw new UnsupportedOperationException();
    }

    /**
     * Returns a token that authorizes an Entity (example: cloud service) to perform
     * an action on behalf of the application identified by Instance ID.
     * <p/>
     * This is similar to an OAuth2 token except, it applies to the
     * application instance instead of a user.
     * <p/>
     * Do not call this function on the main thread.
     *
     * @param authorizedEntity Entity authorized by the token.
     * @param scope            Action authorized for authorizedEntity.
     * @return a token that can identify and authorize the instance of the
     * application on the device.
     * @throws IOException if the request fails.
     */
    public String getToken(String authorizedEntity, String scope) throws IOException {
        return getToken(authorizedEntity, scope, null);
    }

}