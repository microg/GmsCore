/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.api;

/**
 * Each {@code OptionalPendingResult} is a {@link PendingResult} with additional support for non-blocking accessors. The result of an
 * {@code OptionalPendingResult} may be available immediately. If the result is available {@link #isDone} will return true.
 *
 * @param <R> Result returned by various accessors.
 */
public abstract class OptionalPendingResult<R extends Result> extends PendingResult<R> {
    /**
     * Returns the {@link Result} immediately if it is available. If the result is not available, an exception will be thrown. This method should only be
     * called after checking that {@link #isDone} returns true.
     * <p>
     * After the result has been retrieved using {@link #get}, await, or has been delivered to the result callback, it is an error to attempt to retrieve the result
     * again. It is the responsibility of the caller or callback receiver to release any resources associated with the returned result. Some result types
     * may implement {@link Releasable}, in which case {@link Releasable#release} should be used to free the associated resources.
     *
     * @throws IllegalStateException when the result is not {@link #isDone}.
     */
    public abstract R get();

    /**
     * Returns true if the result is available immediately, false otherwise.
     */
    public abstract boolean isDone();
}
