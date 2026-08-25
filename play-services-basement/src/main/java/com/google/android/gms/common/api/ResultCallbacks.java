/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.api;

import android.os.Handler;
import androidx.annotation.NonNull;

/**
 * Callbacks for receiving a {@link Result} from a as an asynchronous callback. Contains separate callbacks for success and failure.
 * These methods are called on the main thread, unless overridden by {@link GoogleApiClient.Builder#setHandler(Handler)}.
 */
public abstract class ResultCallbacks<R extends Result> implements ResultCallback<R> {
    /**
     * Called when the {@link Result} is ready and a failure occurred.
     *
     * @param result Status resulting from the API call. Guaranteed to be non-null and unsuccessful.
     */
    public abstract void onFailure(@NonNull Status result);

    /**
     * Called when the {@link Result} is ready and was successful.
     * <p>
     * It is the responsibility of the callback to release any resources associated with the result if {@link #onSuccess(Result)} is called. Some result types may
     * implement {@link Releasable}, in which case {@link Releasable#release()} should be used to free the associated resources. If a failure occurs the result will be
     * released automatically.
     *
     * @param result The result from the API call. Never null.
     */
    public abstract void onSuccess(@NonNull R result);
}
