/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.api.phone;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.internal.IStatusCallback;
import com.google.android.gms.tasks.TaskCompletionSource;

class StatusCallbackImpl extends IStatusCallback.Stub {
    private final TaskCompletionSource<Void> completionSource;

    public StatusCallbackImpl(TaskCompletionSource<Void> completionSource) {
        this.completionSource = completionSource;
    }

    @Override
    public void onResult(Status status) {
        if (status.isSuccess()) {
            completionSource.trySetResult(null);
        } else {
            completionSource.trySetException(new ApiException(status));
        }
    }
}
