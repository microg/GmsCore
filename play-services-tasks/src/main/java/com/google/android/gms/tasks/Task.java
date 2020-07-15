/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.tasks;

import android.app.Activity;

import org.microg.gms.common.PublicApi;

import java.util.concurrent.Executor;

/**
 * Represents an asynchronous operation.
 */
@PublicApi
public abstract class Task<TResult> {

    public Task() {
    }

    /**
     * Adds a listener that is called when the Task completes.
     * <p/>
     * The listener will be called on main application thread. If the Task is already complete, a
     * call to the listener will be immediately scheduled. If multiple listeners are added, they
     * will be called in the order in which they were added.
     *
     * @return this Task
     */
    public Task<TResult> addOnCompleteListener(OnCompleteListener<TResult> listener) {
        throw new UnsupportedOperationException("addOnCompleteListener is not implemented");
    }

    /**
     * Adds an Activity-scoped listener that is called when the Task completes.
     * <p/>
     * The listener will be called on main application thread. If the Task is already complete, a
     * call to the listener will be immediately scheduled. If multiple listeners are added, they
     * will be called in the order in which they were added.
     * <p/>
     * The listener will be automatically removed during {@link Activity#onStop()}.
     *
     * @return this Task
     */
    public Task<TResult> addOnCompleteListener(Activity activity, OnCompleteListener<TResult> listener) {
        throw new UnsupportedOperationException("addOnCompleteListener is not implemented");
    }

    /**
     * Adds a listener that is called when the Task completes.
     * <p/>
     * If the Task is already complete, a call to the listener will be immediately scheduled. If
     * multiple listeners are added, they will be called in the order in which they were added.
     *
     * @param executor the executor to use to call the listener
     * @return this Task
     */
    public Task<TResult> addOnCompleteListener(Executor executor, OnCompleteListener<TResult> listener) {
        throw new UnsupportedOperationException("addOnCompleteListener is not implemented");
    }

    /**
     * Adds an Activity-scoped listener that is called if the Task fails.
     * <p/>
     * The listener will be called on main application thread. If the Task has already failed, a
     * call to the listener will be immediately scheduled. If multiple listeners are added, they
     * will be called in the order in which they were added.
     * <p/>
     * The listener will be automatically removed during {@link Activity#onStop()}.
     *
     * @return this Task
     */
    public abstract Task<TResult> addOnFailureListener(Activity activity, OnFailureListener listener);

    /**
     * Adds an Activity-scoped listener that is called if the Task fails.
     * <p/>
     * The listener will be called on main application thread. If the Task has already failed, a
     * call to the listener will be immediately scheduled. If multiple listeners are added, they
     * will be called in the order in which they were added.
     *
     * @return this Task
     */
    public abstract Task<TResult> addOnFailureListener(OnFailureListener listener);

    /**
     * Adds a listener that is called if the Task fails.
     * <p/>
     * If the Task has already failed, a call to the listener will be immediately scheduled. If
     * multiple listeners are added, they will be called in the order in which they were added.
     *
     * @param executor the executor to use to call the listener
     * @return this Task
     */
    public abstract Task<TResult> addOnFailureListener(Executor executor, OnFailureListener listener);


    /**
     * Adds a listener that is called if the Task completes successfully.
     * <p/>
     * If multiple listeners are added, they will be called in the order in which they were added. If
     * the Task has already completed successfully, a call to the listener will be immediately scheduled.
     *
     * @param executor the executor to use to call the listener
     * @return this Task
     */
    public abstract Task<TResult> addOnSuccessListener(Executor executor, OnSuccessListener<? super TResult> listener);

    /**
     * Adds a listener that is called if the Task completes successfully.
     * <p/>
     * The listener will be called on the main application thread. If the Task has already
     * completed successfully, a call to the listener will be immediately scheduled. If multiple
     * listeners are added, they will be called in the order in which they were added.
     *
     * @return this Task
     */
    public abstract Task<TResult> addOnSuccessListener(OnSuccessListener<? super TResult> listener);

    /**
     * Adds an Activity-scoped listener that is called if the Task completes successfully.
     * <p/>
     * The listener will be called on the main application thread. If the Task has already
     * completed successfully, a call to the listener will be immediately scheduled. If multiple
     * listeners are added, they will be called in the order in which they were added.
     * <p/>
     * The listener will be automatically removed during {@link Activity#onStop()}.
     *
     * @return this Task
     */
    public abstract Task<TResult> addOnSuccessListener(Activity activity, OnSuccessListener<? super TResult> listener);


    /**
     * Returns a new Task that will be completed with the result of applying the specified
     * Continuation to this Task.
     * <p/>
     * The Continuation will be called on the main application thread.
     *
     * @see Continuation#then(Task)
     */
    public <TContinuationResult> Task<TContinuationResult> continueWith(Continuation<TResult, TContinuationResult> continuation) {
        throw new UnsupportedOperationException("continueWith is not implemented");
    }

    /**
     * Returns a new Task that will be completed with the result of applying the specified Continuation to this Task.
     *
     * @param executor the executor to use to call the Continuation
     * @see Continuation#then(Task)
     */
    public <TContinuationResult> Task<TContinuationResult> continueWith(Executor executor, Continuation<TResult, TContinuationResult> continuation) {
        throw new UnsupportedOperationException("continueWith is not implemented");
    }

    /**
     * Returns a new Task that will be completed with the result of applying the specified
     * Continuation to this Task.
     * <p/>
     * The Continuation will be called on the main application thread.
     *
     * @see Continuation#then(Task)
     */
    public <TContinuationResult> Task<TContinuationResult> continueWithTask(Continuation<TResult, Task<TContinuationResult>> continuation) {
        throw new UnsupportedOperationException("continueWithTask is not implemented");
    }

    /**
     * Returns a new Task that will be completed with the result of applying the specified Continuation to this Task.
     *
     * @param executor the executor to use to call the Continuation
     * @see Continuation#then(Task)
     */
    public <TContinuationResult> Task<TContinuationResult> continueWithTask(Executor executor, Continuation<TResult, Task<TContinuationResult>> var2) {
        throw new UnsupportedOperationException("continueWithTask is not implemented");
    }

    /**
     * Returns the exception that caused the Task to fail. Returns {@code null} if the Task is not
     * yet complete, or completed successfully.
     */
    public abstract Exception getException();

    /**
     * Gets the result of the Task, if it has already completed.
     *
     * @throws IllegalStateException     if the Task is not yet complete
     * @throws RuntimeExecutionException if the Task failed with an exception
     */
    public abstract TResult getResult();

    /**
     * Gets the result of the Task, if it has already completed.
     *
     * @throws IllegalStateException     if the Task is not yet complete
     * @throws X                         if the Task failed with an exception of type X
     * @throws RuntimeExecutionException if the Task failed with an exception that was not of type X
     */
    public abstract <X extends Throwable> TResult getResult(Class<X> exceptionType) throws X;

    /**
     * Returns {@code true} if the Task is complete; {@code false} otherwise.
     */
    public abstract boolean isComplete();

    /**
     * Returns {@code true} if the Task has completed successfully; {@code false} otherwise.
     */
    public abstract boolean isSuccessful();

}
