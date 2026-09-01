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
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.HasApiKey;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.tasks.Task;

/**
 * The interface for interacting with the SMS Code Browser API. By using {@link #startSmsCodeRetriever()}, you can retrieve the
 * origin-bound one-time code from SMS messages.
 * <p>
 * The SMS message format should follow the origin-bound one-time code specification:
 * <ul>
 *     <li>Can optionally begin with human-readable explanatory text. This consists of all but the last line of the message.</li>
 *     <li>The last line of the message contains both a host and a code, each prefixed with a sigil: U+0040 (@) before the host, and U+0023 (#) before the code.</li>
 * </ul>
 * <p>
 * Note: This interface works only for the default browser app set by the current user. Any other calls will fail with {@link SmsRetrieverStatusCodes#API_NOT_AVAILABLE}.
 */
public interface SmsCodeBrowserClient extends HasApiKey<Api.ApiOptions.NoOptions> {
    /**
     * Starts {@code SmsCodeRetriever}, which looks for an origin-bound one-time code from SMS messages recently received (up to
     * 1 minute prior). If there is no matching message found from the SMS inbox, it waits for new incoming SMS messages
     * until it finds a matching message or reaches the timeout (about 5 minutes). Calling this method multiple times only
     * returns one result, but it can extend the timeout period to the last call. Once the result is returned or it reaches
     * the timeout, SmsCodeRetriever will stop automatically.
     * <p>
     * The SMS verification code will be sent via a Broadcast Intent with {@link SmsCodeRetriever#SMS_CODE_RETRIEVED_ACTION}.
     * This Intent contains Extras with keys:
     * <ul>
     *     <li>{@link SmsCodeRetriever#EXTRA_SMS_CODE_LINE} for the retrieved line that contains the origin-bound one-time code and the metadata, or
     * {@code null} in failed cases.</li>
     *     <li>{@link SmsCodeRetriever#EXTRA_STATUS} for the Status to indicate {@code RESULT_SUCCESS}, {@code RESULT_TIMEOUT} or other {@link SmsRetrieverStatusCodes}.</li>
     * </ul>
     * If the caller has not been granted or denied permission by the user, it will fail with a {@link ResolvableApiException}. The
     * caller can use {@link ResolvableApiException#startResolutionForResult(Activity, int)} to show a consent dialog for requesting permission from
     * the user. The dialog result is returned via {@link Activity#onActivityResult(int, int, Intent)}. If the user grants the permission,
     * the activity result returns with {@code RESULT_OK}. Then you can start the retriever again to retrieve the verification code.
     * <p>
     * Note: Add {@link SmsRetriever#SEND_PERMISSION} in {@link Context#registerReceiver(BroadcastReceiver, IntentFilter, String, Handler)} while
     * registering the receiver to detect that the broadcast intent is from the SMS Retriever.
     */
    Task<Void> startSmsCodeRetriever();
}
