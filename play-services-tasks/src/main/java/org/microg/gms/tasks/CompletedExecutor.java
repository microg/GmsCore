/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.tasks;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.concurrent.Executor;

public class CompletedExecutor<TResult> extends UpdateExecutor<TResult> {
    private OnCompleteListener<TResult> listener;

    public CompletedExecutor(Executor executor, OnCompleteListener<TResult> listener) {
        super(executor);
        this.listener = listener;
    }

    @Override
    public void onTaskUpdate(Task<TResult> task) {
        if (task.isComplete()) {
            execute(() -> listener.onComplete(task));
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        listener = null;
    }
}
