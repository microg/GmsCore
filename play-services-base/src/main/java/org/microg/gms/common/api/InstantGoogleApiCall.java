/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.common.api;

import com.google.android.gms.tasks.TaskCompletionSource;

public interface InstantGoogleApiCall<R, A extends ApiClient> extends PendingGoogleApiCall<R, A> {
    R execute(A client) throws Exception;

    @Override
    default void execute(A client, TaskCompletionSource<R> completionSource) {
        try {
            completionSource.setResult(execute(client));
        } catch (Exception e) {
            completionSource.setException(e);
        }
    }
}
