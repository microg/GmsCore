/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.tasks;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;

import java.util.concurrent.Executor;

public class FailureExecutor<TResult> extends UpdateExecutor<TResult> {
    private OnFailureListener listener;

    public FailureExecutor(Executor executor, OnFailureListener listener) {
        super(executor);
        this.listener = listener;
    }

    @Override
    public void onTaskUpdate(Task<TResult> task) {
        if (task.isComplete() && !task.isSuccessful() && !task.isCanceled()) {
            execute(() -> listener.onFailure(task.getException()));
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        listener = null;
    }
}
