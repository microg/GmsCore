/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.api.phone;

import com.google.android.gms.auth.api.phone.internal.ISmsRetrieverResultCallback;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.TaskCompletionSource;

class SmsRetrieverResultCallbackImpl extends ISmsRetrieverResultCallback.Stub {
    private final TaskCompletionSource<Void> completionSource;

    public SmsRetrieverResultCallbackImpl(TaskCompletionSource<Void> completionSource) {
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
