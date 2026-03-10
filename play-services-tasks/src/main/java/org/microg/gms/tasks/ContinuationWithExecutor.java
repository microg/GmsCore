/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.tasks;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.util.concurrent.Executor;

public class ContinuationWithExecutor<TResult, TContinuationResult> extends UpdateExecutor<TResult> {
    private Continuation<TResult, Task<TContinuationResult>> continuation;
    private TaskCompletionSource<TContinuationResult> completionSource = new TaskCompletionSource<>();

    public ContinuationWithExecutor(Executor executor, Continuation<TResult, Task<TContinuationResult>> continuation) {
        super(executor);
        this.continuation = continuation;
    }

    @Override
    public void onTaskUpdate(Task<TResult> task) {
        if (task.isComplete()) {
            execute(() -> {
                try {
                    continuation.then(task).addOnCompleteListener(this, (subTask) -> {
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
