/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.phone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

/**
 * API interface for SmsRetriever.
 */
public interface SmsRetrieverApi {
    /**
     * Starts {@code SmsRetriever}, which waits for a matching SMS message until timeout (5 minutes). The matching SMS message
     * will be sent via a Broadcast Intent with action {@link SmsRetriever#SMS_RETRIEVED_ACTION}. The Intent contains Extras with keys
     * {@link SmsRetriever#EXTRA_SMS_MESSAGE} for the retrieved SMS message as a String, and {@link SmsRetriever#EXTRA_STATUS} for {@link Status} to indicate
     * {@code SUCCESS}, {@code DEVELOPER_ERROR}, {@code ERROR}, or {@code TIMEOUT}.
     * <p>
     * Note: Add {@link SmsRetriever#SEND_PERMISSION} while registering the receiver to detect that the broadcast intent is from the SMS Retriever.
     * <p>
     * The possible causes for errors are:
     * <ul>
     *     <li>DEVELOPER_ERROR: the caller app has incorrect number of certificates. Only one certificate is allowed.</li>
     *     <li>ERROR: the AppCode collides with other installed apps.</li>
     * </ul>
     *
     * @return a Task for the call. Attach an {@link OnCompleteListener} and then check {@link Task#isSuccessful()} to determine if it was successful.
     */
    @NonNull
    Task<Void> startSmsRetriever();

    /**
     * Starts {@code SmsUserConsent}, which waits for an OTP-containing SMS message until timeout (5 minutes). OTP-containing
     * SMS message can be retrieved with two steps.
     * <p>
     * Note: Add {@link SmsRetriever#SEND_PERMISSION} while registering the receiver to detect that the broadcast intent is from the SMS Retriever.
     * <ol>
     *     <li>[Get consent Intent] While OTP-containing SMS message comes, a consent Intent will be sent via a Broadcast
     *     Intent with action {@link SmsRetriever#SMS_RETRIEVED_ACTION}. The Intent contains Extras with keys {@link SmsRetriever#EXTRA_CONSENT_INTENT} for the
     *     consent Intent and {@link SmsRetriever#EXTRA_STATUS} for {@link Status} to indicate {@code SUCCESS} or {@code TIMEOUT}.</li>
     *     <li>[Get OTP-containing SMS message] Calls {@code startActivityForResult} with consent Intent to launch a consent
     *     dialog to get user's approval, then the OTP-containing SMS message can be retrieved from the activity result.</li>
     * </ol>
     *
     * @param senderAddress address of desired SMS sender, or {@code null} to retrieve any sender
     * @return a Task for the call. Attach an {@link OnCompleteListener} and then check {@link Task#isSuccessful()} to determine if it was successful.
     */
    @NonNull
    Task<Void> startSmsUserConsent(@Nullable String senderAddress);
}
