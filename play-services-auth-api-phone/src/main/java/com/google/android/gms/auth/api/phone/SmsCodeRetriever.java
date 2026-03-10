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
import org.microg.gms.auth.api.phone.SmsCodeAutofillClientImpl;
import org.microg.gms.auth.api.phone.SmsCodeBrowserClientImpl;

/**
 * {@code SmsCodeRetriever} is a variant of {@link SmsRetriever}, and it provides access to Google services that help you retrieve SMS
 * verification codes sent to the user's device, without having to ask for {@code android.permission.READ_SMS} or {@code android.permission.RECEIVE_SMS}.
 * <p>
 * To use {@code SmsCodeRetriever} in the Android autofill service, obtain an instance of {@link SmsCodeAutofillClient} using
 * {@link #getAutofillClient(Context)} or {@link #getAutofillClient(Activity)}, and start SMS Code Retriever service by calling
 * {@link SmsCodeAutofillClient#startSmsCodeRetriever()}. To use it in the browser app, you obtain an instance of {@link SmsCodeBrowserClient} using
 * {@link #getBrowserClient(Context)} or {@link #getBrowserClient(Activity)} instead.
 * <p>
 * The service first looks for an SMS verification code from messages recently received (up to 1 minute prior). If there is no
 * SMS verification code found from the SMS inbox, it waits for new incoming SMS messages until it finds an SMS
 * verification code or reaches the timeout (about 5 minutes).
 */
public class SmsCodeRetriever {
    /**
     * Intent extra key of the retrieved SMS verification code by the {@link SmsCodeAutofillClient}.
     */
    @NonNull
    public static final String EXTRA_SMS_CODE = "com.google.android.gms.auth.api.phone.EXTRA_SMS_CODE";
    /**
     * Intent extra key of the retrieved SMS verification code line by the {@link SmsCodeBrowserClient}.
     */
    @NonNull
    public static final String EXTRA_SMS_CODE_LINE = "com.google.android.gms.auth.api.phone.EXTRA_SMS_CODE_LINE";
    /**
     * Intent extra key of {@link Status}, which indicates {@code RESULT_SUCCESS}, {@code RESULT_TIMEOUT} or {@link SmsRetrieverStatusCodes}.
     */
    @NonNull
    public static final String EXTRA_STATUS = "com.google.android.gms.auth.api.phone.EXTRA_STATUS";
    /**
     * Intent action when an SMS verification code is retrieved.
     */
    @NonNull
    public static final String SMS_CODE_RETRIEVED_ACTION = "com.google.android.gms.auth.api.phone.SMS_CODE_RETRIEVED";

    /**
     * Creates a new instance of {@link SmsCodeAutofillClient} for use in an {@link Activity}.
     * This {@link SmsCodeAutofillClient} is intended to be used by the current user-designated autofill service only.
     */
    @NonNull
    public static SmsCodeAutofillClient getAutofillClient(Activity activity) {
        return new SmsCodeAutofillClientImpl(activity);
    }

    /**
     * Creates a new instance of {@link SmsCodeAutofillClient} for use in a {@link Context}.
     * This {@link SmsCodeAutofillClient} is intended to be used by the current user-designated autofill service only.
     */
    @NonNull
    public static SmsCodeAutofillClient getAutofillClient(Context context) {
        return new SmsCodeAutofillClientImpl(context);
    }

    /**
     * Creates a new instance of {@link SmsCodeBrowserClient} for use in an {@link Activity}.
     * This {@link SmsCodeBrowserClient} is intended to be used by the default browser app only.
     */
    @NonNull
    public static SmsCodeBrowserClient getBrowserClient(Activity activity) {
        return new SmsCodeBrowserClientImpl(activity);
    }

    /**
     * Creates a new instance of {@link SmsCodeBrowserClient} for use in a {@link Context}.
     * This {@link SmsCodeBrowserClient} is intended to be used by the default browser app only.
     */
    @NonNull
    public static SmsCodeBrowserClient getBrowserClient(Context context) {
        return new SmsCodeBrowserClientImpl(context);
    }

}
