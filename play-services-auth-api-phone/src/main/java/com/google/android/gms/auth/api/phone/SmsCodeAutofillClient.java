/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.phone;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Handler;
import androidx.annotation.IntDef;
import com.google.android.gms.common.api.*;
import com.google.android.gms.tasks.Task;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The interface for interacting with the SMS Code Autofill API. These methods are only supported on devices running Android P and later.
 * For devices that run versions earlier than Android P, all method calls return {@link SmsRetrieverStatusCodes#PLATFORM_NOT_SUPPORTED}.
 * <p>
 * Note: This interface works only for the current user-designated autofill service.
 * Any calls from non user-designated autofill services or other applications will fail with {@link SmsRetrieverStatusCodes#API_NOT_AVAILABLE}.
 */
public interface SmsCodeAutofillClient extends HasApiKey<Api.ApiOptions.NoOptions> {
    /**
     * Returns the {@link SmsCodeAutofillClient.PermissionState} of the current user-designated autofill service.
     * The result could be {@code NONE}, {@code GRANTED}, or {@code DENIED}.
     * <p>
     * The autofill service should check its permission state prior to showing the suggestion prompt for retrieving an SMS
     * verification code, because it will definitely fail on calling {@link #startSmsCodeRetriever()} in permission denied state.
     */
    Task<@PermissionState Integer> checkPermissionState();

    /**
     * Returns {@code true} if there are requests from {@link SmsRetriever} in progress for the given package name.
     * <p>
     * The autofill service can check this method to avoid showing a suggestion prompt for retrieving an SMS verification code,
     * in case that a user app may already be retrieving the SMS verification code through {@link SmsRetriever}.
     * <p>
     * Note: This result does not include those requests from {@code SmsCodeAutofillClient}.
     */
    Task<Boolean> hasOngoingSmsRequest(String packageName);

    /**
     * Starts {@code SmsCodeRetriever}, which looks for an SMS verification code from messages recently received (up to 1 minute
     * prior). If there is no SMS verification code found from the SMS inbox, it waits for new incoming SMS messages until it
     * finds an SMS verification code or reaches the timeout (about 5 minutes).
     * <p>
     * The SMS verification code will be sent via a Broadcast Intent with {@link SmsCodeRetriever#SMS_CODE_RETRIEVED_ACTION}. This Intent contains
     * Extras with keys {@link SmsCodeRetriever#EXTRA_SMS_CODE} for the retrieved verification code as a {@code String}, and {@link SmsCodeRetriever#EXTRA_STATUS} for {@link Status} to
     * indicate {@code RESULT_SUCCESS}, {@code RESULT_TIMEOUT} or {@link SmsRetrieverStatusCodes}.
     * <p>
     * Note: Add {@link SmsRetriever#SEND_PERMISSION} in {@link Context#registerReceiver(BroadcastReceiver, IntentFilter, String, Handler)} while
     * registering the receiver to detect that the broadcast intent is from the SMS Retriever.
     */
    Task<Void> startSmsCodeRetriever();

    /**
     * Permission states for the current user-designated autofill service. The initial state is {@code NONE} upon the first time using the
     * SMS Code Autofill API. This permission can be granted or denied through a consent dialog requested by the current
     * autofill service, or an explicit change by users within the SMS verification codes settings.
     */
    @Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PermissionState.NONE, PermissionState.GRANTED, PermissionState.DENIED})
    @interface PermissionState {
        /**
         * Indicates that the current autofill service has not been granted or denied permission by the user. Calling
         * {@link #startSmsCodeRetriever()} will fail with {@link CommonStatusCodes#RESOLUTION_REQUIRED}. The caller can use
         * {@link ResolvableApiException#startResolutionForResult(Activity, int)} to show a consent dialog for requesting permission from the user.
         */
        int NONE = 0;
        /**
         * Indicates that the current autofill service has been granted permission by the user. The user consent is not required for
         * calling {@link #startSmsCodeRetriever()} in this state.
         */
        int GRANTED = 1;
        /**
         * Indicates that the current autofill service has been denied permission by the user. Calling {@link #startSmsCodeRetriever()}
         * will fail with {@link SmsRetrieverStatusCodes#USER_PERMISSION_REQUIRED}. It can only be resolved by the user explicitly turning on the permission
         * in settings.
         */
        int DENIED = 2;
    }
}
