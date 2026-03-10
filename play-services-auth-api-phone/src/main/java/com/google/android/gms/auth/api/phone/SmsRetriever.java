/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.phone;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.gms.common.api.Status;
import org.microg.gms.auth.api.phone.SmsRetrieverClientImpl;

/**
 * {@code SmsRetriever} provides access to Google services that help you retrieve SMS messages sent to your app without
 * having to ask for {@code android.permission.READ_SMS} or {@code android.permission.RECEIVE_SMS}.
 * <p>
 * To use {@code SmsRetriever}, obtain an instance of {@link SmsRetrieverClient} using {@link #getClient(Context)} or
 * {@link #getClient(Activity)}, then start the SMS retriever service by calling {@link SmsRetrieverClient#startSmsRetriever()} or
 * {@link SmsRetrieverClient#startSmsUserConsent(String)}. The service waits for a matching SMS message until timeout (5 minutes).
 */
public class SmsRetriever {
    /**
     * Intent extra key of the consent intent to be launched from client app.
     */
    @NonNull
    public static final String EXTRA_CONSENT_INTENT = "com.google.android.gms.auth.api.phone.EXTRA_CONSENT_INTENT";
    /**
     * [Optional] Intent extra key of the retrieved Sim card subscription Id if any, as an {@code int}.
     */
    @NonNull
    public static final String EXTRA_SIM_SUBSCRIPTION_ID = "com.google.android.gms.auth.api.phone.EXTRA_SIM_SUBSCRIPTION_ID";
    /**
     * Intent extra key of the retrieved SMS message as a {@code String}.
     */
    @NonNull
    public static final String EXTRA_SMS_MESSAGE = "com.google.android.gms.auth.api.phone.EXTRA_SMS_MESSAGE";
    /**
     * Intent extra key of {@link Status}, which indicates SUCCESS or TIMEOUT.
     */
    @NonNull
    public static final String EXTRA_STATUS = "com.google.android.gms.auth.api.phone.EXTRA_STATUS";
    /**
     * Permission that's used to register the receiver to detect that the broadcaster is the SMS Retriever.
     */
    @NonNull
    public static final String SEND_PERMISSION = "com.google.android.gms.auth.api.phone.permission.SEND";
    /**
     * Intent action when SMS message is retrieved.
     */
    @NonNull
    public static final String SMS_RETRIEVED_ACTION = "com.google.android.gms.auth.api.phone.SMS_RETRIEVED";

    /**
     * Create a new instance of {@link SmsRetrieverClient} for use in an {@link Activity}.
     */
    @NonNull
    public static SmsRetrieverClient getClient(Activity activity) {
        return new SmsRetrieverClientImpl(activity);
    }

    /**
     * Create a new instance of {@link SmsRetrieverClient} for use in a {@link Context}.
     */
    @NonNull
    public static SmsRetrieverClient getClient(Context context) {
        return new SmsRetrieverClientImpl(context);
    }
}
