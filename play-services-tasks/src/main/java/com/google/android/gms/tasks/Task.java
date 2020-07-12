/*
 * SPDX-FileCopyrightText: 2016, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
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
     * Adds a listener that is called if the Task is canceled.
     * <p>
     * The listener will be called on main application thread. If the Task has already been canceled, a call to the listener will be immediately scheduled. If multiple listeners are added, they will be called in the order in which they were added.
     *
     * @return this Task
     */
    public Task<TResult> addOnCanceledListener(OnCanceledListener listener) {
        throw new UnsupportedOperationException("addOnCanceledListener is not implemented");
    }

    /**
     * Adds a listener that is called if the Task is canceled.
     * <p>
     * If the Task has already been canceled, a call to the listener will be immediately scheduled. If multiple listeners are added, they will be called in the order in which they were added.
     *
     * @param executor the executor to use to call the listener
     * @return this Task
     */
    public Task<TResult> addOnCanceledListener(Executor executor, OnCanceledListener listener) {
        throw new UnsupportedOperationException("addOnCanceledListener is not implemented");
    }

    /**
     * Adds an Activity-scoped listener that is called if the Task is canceled.
     * <p>
     * The listener will be called on main application thread. If the Task has already been canceled, a call to the listener will be immediately scheduled. If multiple listeners are added, they will be called in the order in which they were added.
     * <p>
     * The listener will be automatically removed during {@link Activity#onStop()}.
     *
     * @return this Task
     */
    public Task<TResult> addOnCanceledListener(Activity activity, OnCanceledListener listener) {
        throw new UnsupportedOperationException("addOnCanceledListener is not implemented");
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
    public <TContinuationResult> Task<TContinuationResult> continueWithTask(Executor executor, Continuation<TResult, Task<TContinuationResult>> continuation) {
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
     * Returns {@code true} if the Task is canceled; {@code false} otherwise.
     */
    public abstract boolean isCanceled();

    /**
     * Returns {@code true} if the Task is complete; {@code false} otherwise.
     */
    public abstract boolean isComplete();

    /**
     * Returns {@code true} if the Task has completed successfully; {@code false} otherwise.
     */
    public abstract boolean isSuccessful();

    /**
     * Returns a new Task that will be completed with the result of applying the specified SuccessContinuation to this Task when this Task completes successfully. If the previous Task fails, the onSuccessTask completion will be skipped and failure listeners will be invoked.
     * <p>
     * The SuccessContinuation will be called on the main application thread.
     * <p>
     * If the previous Task is canceled, the returned Task will also be canceled and the SuccessContinuation would not execute.
     *
     * @see SuccessContinuation#then
     */
    public <TContinuationResult> Task<TContinuationResult> onSuccessTask(SuccessContinuation<TResult, TContinuationResult> successContinuation) {
        throw new UnsupportedOperationException("onSuccessTask is not implemented");
    }

    /**
     * Returns a new Task that will be completed with the result of applying the specified SuccessContinuation to this Task when this Task completes successfully. If the previous Task fails, the onSuccessTask completion will be skipped and failure listeners will be invoked.
     * <p>
     * If the previous Task is canceled, the returned Task will also be canceled and the SuccessContinuation would not execute.
     *
     * @param executor the executor to use to call the SuccessContinuation
     * @see SuccessContinuation#then
     */
    public <TContinuationResult> Task<TContinuationResult> onSuccessTask(Executor executor, SuccessContinuation<TResult, TContinuationResult> successContinuation) {
        throw new UnsupportedOperationException("onSuccessTask is not implemented");
    }

}
