/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.common.api;

import com.google.android.gms.tasks.TaskCompletionSource;

public interface PendingGoogleApiCall<R, A extends ApiClient> {
    void execute(A client, TaskCompletionSource<R> completionSource) throws Exception;
}
