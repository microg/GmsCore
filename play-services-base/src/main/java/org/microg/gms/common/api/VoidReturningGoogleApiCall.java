/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.common.api;

import com.google.android.gms.common.api.Api;
import com.google.android.gms.tasks.TaskCompletionSource;

public interface VoidReturningGoogleApiCall<A extends Api.Client> extends PendingGoogleApiCall<Void, A>{
    void execute(A client) throws Exception;

    @Override
    default void execute(A client, TaskCompletionSource<Void> completionSource) {
        try {
            execute(client);
            completionSource.setResult(null);
        } catch (Exception e) {
            completionSource.setException(e);
        }
    }
}
