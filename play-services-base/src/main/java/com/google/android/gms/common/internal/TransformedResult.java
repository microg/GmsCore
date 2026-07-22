/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.internal;

import androidx.annotation.NonNull;
import com.google.android.gms.common.api.*;

/**
 * Represents a pending result that has been transformed by one or more subsequent API calls.
 * <p>
 * The result can either be consumed by callbacks set using {@link #andFinally} or used as input to another API call using {@link #then}. It is an error to call
 * both of these methods, or one of them multiple times, on a single instance.
 *
 * @see PendingResult#then(ResultTransform)
 */
public abstract class TransformedResult<R extends Result> {
    /**
     * Requests that the supplied callbacks are called when the result is ready.
     */
    public abstract void andFinally(@NonNull ResultCallbacks<? super R> callbacks);

    /**
     * Transforms the result by making another API call.
     * <p>
     * If the result is successful, then {@link ResultTransform#onSuccess(Result)} will be called to make the additional API call that yields the transformed result. If the result is a
     * failure, then {@link ResultTransform#onFailure(Status)} will be called to (optionally) allow modification of failure status.
     * <p>
     * If the result implements {@link Releasable}, then {@link Releasable#release()} will be called once the transform has been applied.
     */
    @NonNull
    public abstract <S extends Result> TransformedResult<S> then(@NonNull ResultTransform<? super R, S> transform);
}
