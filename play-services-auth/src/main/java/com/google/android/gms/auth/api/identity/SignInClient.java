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
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.HasApiKey;
import com.google.android.gms.tasks.Task;

public interface SignInClient extends HasApiKey<SignInOptions> {
    /**
     * Initiates the retrieval of a credential that can assist the caller in signing a user in to their application.
     * <p>
     * If the request cannot be honored, an exception will be set on the returned {@link Task}. In all other cases, a
     * {@link BeginSignInResult} will be returned.
     *
     * @param signInRequest configuration for the sign-in operation
     * @return {@link Task} which eventually contains the result of the initialization
     */
    @NonNull
    Task<BeginSignInResult> beginSignIn(@NonNull BeginSignInRequest signInRequest);

    /**
     * Retrieves the Phone Number from the {@link Intent} returned upon a successful Phone Number Hint request, throwing an
     * {@link ApiException} if no phone number is available or the input {@link Intent} is null.
     *
     * @throws ApiException
     */
    @NonNull
    String getPhoneNumberFromIntent(@Nullable Intent data) throws ApiException;

    /**
     * Gets the {@link PendingIntent} that initiates the Phone Number Hint flow.
     * <p>
     * If there is no phone number on the device, an exception will be set on the returned {@link Task}. In all other cases, a
     * {@link PendingIntent} will be returned.
     *
     * @return {@link Task} which can be used to start the Phone Number Hint flow.
     */
    @NonNull
    Task<PendingIntent> getPhoneNumberHintIntent(@NonNull GetPhoneNumberHintIntentRequest getPhoneNumberHintIntentRequest);

    /**
     * Retrieves the {@link SignInCredential} from the {@link Intent} returned upon successful sign-in, throwing an {@link ApiException} if no
     * credential is present.
     *
     * @throws ApiException
     */
    @NonNull
    SignInCredential getSignInCredentialFromIntent(@Nullable Intent data) throws ApiException;

    /**
     * Gets the {@link PendingIntent} that initiates the Google Sign-in flow.
     * <p>
     * If the request cannot be honored, an exception will be set on the returned {@link Task}. In all other cases, a {@link PendingIntent}
     * will be returned.
     *
     * @param getSignInIntentRequest configuration for Google Sign-in flow
     * @return {@link Task} which eventually contains the {@link PendingIntent} to start the Google Sign-in flow.
     */
    @NonNull
    Task<PendingIntent> getSignInIntent(@NonNull GetSignInIntentRequest getSignInIntentRequest);

    /**
     * Resets internal state related to sign-in.
     * <p>
     * This method should be invoked when a user signs out of your app.
     *
     * @return {@link Task} which eventually terminates in success or failure
     */
    @NonNull
    Task<Void> signOut();
}
