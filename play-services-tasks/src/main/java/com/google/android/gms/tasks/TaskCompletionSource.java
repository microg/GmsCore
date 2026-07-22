/*
 * SPDX-FileCopyrightText: 2016 microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.tasks;

import org.microg.gms.common.PublicApi;
import org.microg.gms.tasks.TaskImpl;

/**
 * Provides the ability to create an incomplete {@link Task}-based APIs.
 * <p/>
 * Use a {@code TaskCompletionSource} to set a result or exception on a Task returned from an asynchronous API:
 * <pre>
 * public class MarcoPolo {
 *     public static Task<String> marco(int delay) {
 *         TaskCompletionSource<String> taskCompletionSource = new TaskCompletionSource<>();
 *
 *         new Handler().postDelayed(() -> taskCompletionSource.setResult("polo"), delay);
 *
 *         return taskCompletionSource.getTask();
 *     }
 * }
 * </pre>
 * And then your APIs can be used as any other {@link Task}-consuming APIs:
 * <pre>
 * public class MyActivity extends Activity {
 *     &#64;Override
 *     public void onStart() {
 *         super.onStart();
 *
 *         marco(1000).addOnCompleteListener(
 *             task -> Log.d(TAG, "got message after one second: " + task.getResult()));
 *     }
 * }
 * </pre>
 *
 * and later complete it by either
 * calling {@link #setResult(TResult)} or {@link #setException(Exception)}.
 */
@PublicApi
public class TaskCompletionSource<TResult> {
    private TaskImpl<TResult> task = new TaskImpl<>();

    /**
     * Creates an instance of {@link TaskCompletionSource}.
     */
    public TaskCompletionSource() {
    }

    /**
     * Creates an instance of {@link TaskCompletionSource} with a {@link CancellationToken} so that the Task can be set to canceled when {@link CancellationToken} is canceled.
     */
    public TaskCompletionSource(CancellationToken token) {
        token.onCanceledRequested(() -> {
            try {
                task.cancel();
            } catch (DuplicateTaskCompletionException ignored) {
            }
        });
    }

    /**
     * Returns the Task.
     */
    public Task<TResult> getTask() {
        return task;
    }

    /**
     * Completes the Task with the specified exception.
     *
     * @throws IllegalStateException if the Task is already complete
     */
    public void setException(Exception e) {
        task.setException(e);
    }

    /**
     * Completes the Task with the specified exception, unless the Task has already completed.
     * If the Task has already completed, the call does nothing.
     *
     * @return {@code true} if the exception was set successfully, {@code false} otherwise
     */
    public boolean trySetException(Exception e) {
        try {
            setException(e);
            return true;
        } catch (DuplicateTaskCompletionException ignored) {
            return false;
        }
    }

    /**
     * Completes the Task with the specified result.
     *
     * @throws IllegalStateException if the Task is already complete
     */
    public void setResult(TResult result) {
        task.setResult(result);
    }

    /**
     * Completes the Task with the specified result, unless the Task has already completed.
     * If the Task has already completed, the call does nothing.
     *
     * @return {@code true} if the result was set successfully, {@code false} otherwise
     */
    public boolean trySetResult(TResult result) {
        try {
            setResult(result);
            return true;
        } catch (DuplicateTaskCompletionException ignored) {
            return false;
        }
    }
}
