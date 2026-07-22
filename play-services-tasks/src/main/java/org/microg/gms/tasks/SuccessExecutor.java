/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.tasks;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.concurrent.Executor;

public class SuccessExecutor<TResult> extends UpdateExecutor<TResult> {
    private OnSuccessListener<? super TResult> listener;

    public SuccessExecutor(Executor executor, OnSuccessListener<? super TResult> listener) {
        super(executor);
        this.listener = listener;
    }

    @Override
    public void onTaskUpdate(Task<TResult> task) {
        if (task.isSuccessful()) {
            execute(() -> listener.onSuccess(task.getResult()));
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        listener = null;
    }
}
