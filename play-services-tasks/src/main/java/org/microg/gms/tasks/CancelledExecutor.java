/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.tasks;

import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.Task;

import java.util.concurrent.Executor;

public class CancelledExecutor<TResult> extends UpdateExecutor<TResult> {
    private OnCanceledListener listener;

    public CancelledExecutor(Executor executor, OnCanceledListener listener) {
        super(executor);
        this.listener = listener;
    }

    @Override
    public void onTaskUpdate(Task<TResult> task) {
        if (task.isCanceled()) {
            execute(() -> listener.onCanceled());
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        listener = null;
    }
}

