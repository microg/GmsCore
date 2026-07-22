/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
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
import com.google.android.gms.common.api.HasApiKey;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;

/**
 * A client for the Credential Saving API.
 */
public interface CredentialSavingClient extends HasApiKey<CredentialSavingOptions> {
    /**
     * Extracts the {@link Status} from the {@link Intent} object in activity results.
     */
    @NonNull
    Status getStatusFromIntent(@Nullable Intent intent);

    /**
     * Attempts to save a token for account linking.
     * <p>
     * Calling this method will provide a {@link PendingIntent} in the response that can be used to launch the flow to complete the
     * saving of the account linking token. As part of the request, you need to provide a {@link PendingIntent} for your consent page
     * that Google Play services will launch in the middle of the flow. The result must then be sent back to the caller following a
     * certain contract described in {@link SaveAccountLinkingTokenRequest.Builder#setConsentPendingIntent(PendingIntent)}.
     *
     * @param saveAccountLinkingTokenRequest the request that contains the parameters to successfully return a response that can be used to
     *                                       launch the appropriate flow.
     * @return {@link Task} which may contain the {@link PendingIntent} required to launch the flow. To find out if the response can be used
     * to start the flow, first call {@link SaveAccountLinkingTokenResult#hasResolution()}.
     */
    @NonNull
    Task<SaveAccountLinkingTokenResult> saveAccountLinkingToken(@NonNull SaveAccountLinkingTokenRequest saveAccountLinkingTokenRequest);

    /**
     * Initiates the storage of a password-backed credential that can later be used to sign a user in.
     * <p>
     * If the request cannot be honored, an exception will be set on the returned {@link Task}. In all other cases, a
     * {@link SavePasswordResult} will be returned.
     *
     * @param savePasswordRequest container for the {@link SignInPassword} for the password-saving flow
     * @return {@link Task} which eventually contains the result of the initialization
     */
    @NonNull
    Task<SavePasswordResult> savePassword(@NonNull SavePasswordRequest savePasswordRequest);
}
