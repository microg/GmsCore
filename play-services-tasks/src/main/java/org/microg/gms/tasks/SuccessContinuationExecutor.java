/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.tasks;

import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.util.concurrent.Executor;

public class SuccessContinuationExecutor<TResult, TContinuationResult> extends UpdateExecutor<TResult> {
    private SuccessContinuation<TResult, TContinuationResult> continuation;
    private TaskCompletionSource<TContinuationResult> completionSource = new TaskCompletionSource<>();

    public SuccessContinuationExecutor(Executor executor, SuccessContinuation<TResult, TContinuationResult> continuation) {
        super(executor);
        this.continuation = continuation;
    }

    @Override
    public void onTaskUpdate(Task<TResult> task) {
        if (task.isSuccessful()) {
            execute(() -> {
                try {
                    continuation.then(task.getResult()).addOnCompleteListener(this, (subTask) -> {
                        if (subTask.isSuccessful()) {
                            completionSource.setResult(subTask.getResult());
                        } else {
                            completionSource.setException(subTask.getException());
                        }
                    });
                } catch (Exception e) {
                    completionSource.setException(e);
                }
            });
        } else {
            completionSource.setException(task.getException());
        }
    }

    public Task<TContinuationResult> getTask() {
        return completionSource.getTask();
    }

    @Override
    public void cancel() {
        super.cancel();
        continuation = null;
        completionSource = null;
    }
}
