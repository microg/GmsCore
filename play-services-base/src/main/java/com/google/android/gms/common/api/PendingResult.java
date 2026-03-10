/*
 * SPDX-FileCopyrightText: 2015 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.api;

import android.os.Handler;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.ResultTransform;
import com.google.android.gms.common.internal.TransformedResult;
import org.microg.gms.common.Hide;

import java.util.concurrent.TimeUnit;

/**
 * Represents a pending result from calling an API method in Google Play services. The final result object from a PendingResult is of type R,
 * which can be retrieved in one of two ways.
 * <ul>
 * <li>via blocking calls to {@link #await()}, or {@link #await(long, TimeUnit)}, or</li>
 * <li>via a callback by passing in an object implementing interface {@link ResultCallback} to
 * {@link #setResultCallback(ResultCallback)}.</li>
 * </ul>
 * After the result has been retrieved using {@link #await()} or delivered to the result callback, it is an error to attempt to retrieve the result again. It is
 * the responsibility of the caller or callback receiver to release any resources associated with the returned result. Some result types may
 * implement {@link Releasable}, in which case {@link Releasable#release()} should be used to free the associated resources.
 */
public abstract class PendingResult<R extends Result> {

    /**
     * Blocks until the task is completed. This is not allowed on the UI thread. The returned result object can have an additional failure mode of
     * {@link CommonStatusCodes#INTERRUPTED}.
     */
    public abstract R await();

    /**
     * Blocks until the task is completed or has timed out waiting for the result. This is not allowed on the UI thread. The returned result object can
     * have an additional failure mode of either {@link CommonStatusCodes#INTERRUPTED} or {@link CommonStatusCodes#TIMEOUT}.
     */
    public abstract R await(long time, TimeUnit unit);

    /**
     * Requests that the PendingResult be canceled. If the result is available, but not consumed it will be released. If the result is set after
     * cancelation was requested it is immediately released.
     * <p>
     * {@link ResultCallback#onResult(Result)} will never be called, {@link #await()}  will return a failed result with {@link CommonStatusCodes#CANCELED}.
     */
    public abstract void cancel();

    /**
     * Indicates whether the pending result has been canceled either due to calling {@link GoogleApiClient#disconnect()}  or calling {@link #cancel()} directly on the pending result
     * or an enclosing {@link Batch}.
     */
    public abstract boolean isCanceled();

    /**
     * Set the callback here if you want the result to be delivered via a callback when the result is ready.
     */
    public abstract void setResultCallback(@NonNull ResultCallback<R> callback);

    /**
     * Set the callback here if you want the result to be delivered via a callback when the result is ready or has timed out waiting for the result. The
     * returned result object can have an additional failure mode of {@link CommonStatusCodes#TIMEOUT}.
     */
    public abstract void setResultCallback(@NonNull ResultCallback<R> callback, long time, TimeUnit unit);

    /**
     * Transforms the result by making another API call.
     * <p>
     * If the result is successful, then {@link ResultTransform#onSuccess} will be called to make the additional API call that yields the transformed result. If the result is a
     * failure, then {@link ResultTransform#onFailure} will be called to (optionally) allow modification of failure status.
     * <p>
     * If the result implements {@link Releasable}, then {@link Releasable#release} will be called once the transform has been applied.
     * <p>
     * Multiple API calls can be chained together by making subsequent calls to {@link TransformedResult#then(ResultTransform)} and the final result can be received by an instance of
     * specified via {@link TransformedResult#andFinally(ResultCallbacks)}.
     * <p>
     * All {@link ResultTransform}s will be run on a worker thread. These transforms therefore must not interact with UI elements, but they may perform
     * brief background work (not requiring more than a few seconds). If {@link ResultCallbacks} are specified, these will be called on the thread
     * specified by {@link GoogleApiClient.Builder#setHandler(Handler)} or on the main thread by default.
     * <p>
     * If {@link GoogleApiClient#disconnect()} is called before a series of transforms completes the transforms will continue to run in the background until the last one
     * completes. In this case, {@link ResultCallbacks} will not be called. Note that this may cause memory leaks if background transformations are
     * long-running.
     * <p>
     * Note: it is an error to use multiple {@link GoogleApiClient}s for various API calls within subsequent {@link ResultTransform}s. Behavior is undefined if
     * calls don't use the same GoogleApiClient.
     */
    @NonNull
    public <S extends Result> TransformedResult<S> then(@NonNull ResultTransform<? super R, ? extends S> resultTransform) {
        throw new UnsupportedOperationException();
    }

    @Hide
    public interface StatusListener {
        void onComplete(@NonNull Status status);
    }

    @Hide
    public void addStatusListener(@NonNull StatusListener statusListener) {
        throw new UnsupportedOperationException();
    }
}
