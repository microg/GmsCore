/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.tasks;

import org.microg.gms.common.PublicApi;
import org.microg.gms.tasks.CancellationTokenImpl;

/**
 * Creates a new {@link CancellationToken} or cancels one that has already created. There is a 1:1 {@link CancellationTokenSource} to {@link CancellationToken} relationship.
 * <p>
 * To create a {@link CancellationToken}, create a {@link CancellationTokenSource} first and then call {@link #getToken()} to get the {@link CancellationToken} for this {@link CancellationTokenSource}.
 *
 * @see CancellationToken
 */
@PublicApi
public class CancellationTokenSource {
    private final CancellationTokenImpl token = new CancellationTokenImpl();

    /**
     * Creates a new {@link CancellationTokenSource} instance.
     */
    public CancellationTokenSource() {
    }

    /**
     * Cancels the {@link CancellationToken} if cancellation has not been requested yet.
     */
    public void cancel() {
        token.cancel();
    }

    /**
     * Gets the {@link CancellationToken} for this {@link CancellationTokenSource}.
     *
     * @return the {@link CancellationToken} that can be passed to asynchronous {@link Task} to cancel the Task.
     */
    public CancellationToken getToken() {
        return token;
    }
}
