/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.identity;

import android.app.PendingIntent;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.HasApiKey;
import com.google.android.gms.tasks.Task;

/**
 * A client for the authorization API.
 */
public interface AuthorizationClient extends HasApiKey<AuthorizationOptions> {
    /**
     * Requests authorization to access the Google data associated with a signed-in account on the device.
     * <p>
     * If an eligible signed-in account is found for the application, this request will verify that all the requested OAuth 2.0 scopes were previously
     * granted by the user. If they were, the requested tokens will be returned in the result. If, however, no saved account is found or the required
     * grants do not exist, the result will contain a {@link PendingIntent} that can be used to launch the authorization flow. During that flow, the user will
     * be asked to select an account and/or grant the permission for all or a subset of requested scopes. An exception will be set on the returned
     * {@link Task} if authorization is not available on the device (for example, internal error or Play Services not available).
     *
     * @param request configuration for the authorization operation.
     * @return {@link Task} which contains the result of the operation.
     */
    @NonNull
    Task<AuthorizationResult> authorize(@NonNull AuthorizationRequest request);

    /**
     * Clears an access token from the local cache.
     *
     * @param request configuration for the clear token operation.
     * @return A Task that may be used to check for failure, success or completion
     */
    @NonNull
    Task<Void> clearToken(@NonNull ClearTokenRequest request);

    /**
     * Retrieves the {@link AuthorizationResult} from the {@link Intent} returned upon successful authorization, throwing an {@link ApiException} if no result is
     * present or authorization has failed.
     *
     * @throws ApiException
     */
    @NonNull
    AuthorizationResult getAuthorizationResultFromIntent(@Nullable Intent intent) throws ApiException;

    /**
     * Revokes access given to the current application. Future sign-in or authorization attempts will require the user to re-consent to all requested
     * scopes. Applications are required to provide users that are signed in with Google the ability to disconnect their Google account from the
     * app. If the user deletes their account, you must delete the information that your app obtained from the Google APIs.
     *
     * @param revokeAccessRequest configuration for the revoke authorization operation.
     * @return A Task that may be used to check for failure, success or completion
     */
    @NonNull
    Task<Void> revokeAccess(@NonNull RevokeAccessRequest revokeAccessRequest);
}
