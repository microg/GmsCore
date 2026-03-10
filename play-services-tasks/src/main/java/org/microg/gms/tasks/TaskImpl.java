/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.tasks;

import android.app.Activity;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.DuplicateTaskCompletionException;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.RuntimeExecutionException;
import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.Task;

import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

import static com.google.android.gms.tasks.TaskExecutors.MAIN_THREAD;

public class TaskImpl<TResult> extends Task<TResult> {
    private final Object lock = new Object();
    private boolean completed;
    private boolean cancelled;
    private TResult result;
    private Exception exception;
    private Queue<UpdateListener<TResult>> completionQueue = new LinkedBlockingQueue<>();

    @Override
    public Task<TResult> addOnCanceledListener(OnCanceledListener listener) {
        return addOnCanceledListener(MAIN_THREAD, listener);
    }

    @Override
    public Task<TResult> addOnCanceledListener(Executor executor, OnCanceledListener listener) {
        return enqueueOrInvoke(new CancelledExecutor<>(executor, listener));
    }

    @Override
    public Task<TResult> addOnCanceledListener(Activity activity, OnCanceledListener listener) {
        return enqueueOrInvoke(activity, new CancelledExecutor<>(MAIN_THREAD, listener));
    }

    @Override
    public Task<TResult> addOnCompleteListener(OnCompleteListener<TResult> listener) {
        return addOnCompleteListener(MAIN_THREAD, listener);
    }

    @Override
    public Task<TResult> addOnCompleteListener(Executor executor, OnCompleteListener<TResult> listener) {
        return enqueueOrInvoke(new CompletedExecutor<>(executor, listener));
    }

    @Override
    public Task<TResult> addOnCompleteListener(Activity activity, OnCompleteListener<TResult> listener) {
        return enqueueOrInvoke(activity, new CompletedExecutor<>(MAIN_THREAD, listener));
    }

    @Override
    public Task<TResult> addOnFailureListener(OnFailureListener listener) {
        return addOnFailureListener(MAIN_THREAD, listener);
    }

    @Override
    public Task<TResult> addOnFailureListener(Executor executor, OnFailureListener listener) {
        return enqueueOrInvoke(new FailureExecutor<>(executor, listener));
    }

    @Override
    public Task<TResult> addOnFailureListener(Activity activity, OnFailureListener listener) {
        return enqueueOrInvoke(activity, new FailureExecutor<>(MAIN_THREAD, listener));
    }

    @Override
    public Task<TResult> addOnSuccessListener(OnSuccessListener<? super TResult> listener) {
        return addOnSuccessListener(MAIN_THREAD, listener);
    }

    @Override
    public Task<TResult> addOnSuccessListener(Executor executor, OnSuccessListener<? super TResult> listener) {
        return enqueueOrInvoke(new SuccessExecutor<>(executor, listener));
    }

    @Override
    public Task<TResult> addOnSuccessListener(Activity activity, OnSuccessListener<? super TResult> listener) {
        return enqueueOrInvoke(activity, new SuccessExecutor<>(MAIN_THREAD, listener));
    }

    @Override
    public <TContinuationResult> Task<TContinuationResult> continueWith(Continuation<TResult, TContinuationResult> continuation) {
        return continueWith(MAIN_THREAD, continuation);
    }

    @Override
    public <TContinuationResult> Task<TContinuationResult> continueWith(Executor executor, Continuation<TResult, TContinuationResult> continuation) {
        ContinuationExecutor<TResult, TContinuationResult> c = new ContinuationExecutor<>(executor, continuation);
        enqueueOrInvoke(c);
        return c.getTask();
    }

    @Override
    public <TContinuationResult> Task<TContinuationResult> continueWithTask(Continuation<TResult, Task<TContinuationResult>> continuation) {
        return continueWithTask(MAIN_THREAD, continuation);
    }

    @Override
    public <TContinuationResult> Task<TContinuationResult> continueWithTask(Executor executor, Continuation<TResult, Task<TContinuationResult>> continuation) {
        ContinuationWithExecutor<TResult, TContinuationResult> c = new ContinuationWithExecutor<>(executor, continuation);
        enqueueOrInvoke(c);
        return c.getTask();
    }

    @Override
    public Exception getException() {
        synchronized (lock) {
            return exception;
        }
    }

    @Override
    public TResult getResult() {
        synchronized (lock) {
            if (!completed) throw new IllegalStateException("Task is not yet complete");
            if (cancelled) throw new CancellationException("Task is canceled");
            if (exception != null) throw new RuntimeExecutionException(exception);
            return result;
        }
    }

    @Override
    public <X extends Throwable> TResult getResult(Class<X> exceptionType) throws X {
        synchronized (lock) {
            if (!completed) throw new IllegalStateException("Task is not yet complete");
            if (cancelled) throw new CancellationException("Task is canceled");
            if (exceptionType.isInstance(exception)) throw exceptionType.cast(exception);
            if (exception != null) throw new RuntimeExecutionException(exception);
            return result;
        }
    }

    @Override
    public boolean isCanceled() {
        synchronized (lock) {
            return cancelled;
        }
    }

    @Override
    public boolean isComplete() {
        synchronized (lock) {
            return completed;
        }
    }

    @Override
    public boolean isSuccessful() {
        synchronized (lock) {
            return completed && !cancelled && exception == null;
        }
    }

    private void registerActivityStop(Activity activity, UpdateListener<TResult> listener) {
        UpdateListenerLifecycleObserver.getObserverForActivity(activity).registerActivityStopListener(listener);
    }

    private Task<TResult> enqueueOrInvoke(Activity activity, UpdateListener<TResult> listener) {
        synchronized (lock) {
            if (completed) {
                listener.onTaskUpdate(this);
            } else {
                completionQueue.offer(listener);
                registerActivityStop(activity, listener);
            }
        }
        return this;
    }

    private Task<TResult> enqueueOrInvoke(UpdateListener<TResult> listener) {
        synchronized (lock) {
            if (completed) {
                listener.onTaskUpdate(this);
            } else {
                completionQueue.offer(listener);
            }
        }
        return this;
    }

    private void notifyQueue() {
        UpdateListener<TResult> listener;
        while ((listener = completionQueue.poll()) != null) {
            listener.onTaskUpdate(this);
        }
    }

    public void cancel() {
        synchronized (lock) {
            if (completed) throw DuplicateTaskCompletionException.of(this);
            this.completed = true;
            this.cancelled = true;
            notifyQueue();
        }
    }

    public void setResult(TResult result) {
        synchronized (lock) {
            if (completed) throw DuplicateTaskCompletionException.of(this);
            this.completed = true;
            this.result = result;
            notifyQueue();
        }
    }

    public void setException(Exception exception) {
        synchronized (lock) {
            if (completed) throw DuplicateTaskCompletionException.of(this);
            this.completed = true;
            this.exception = exception;
            notifyQueue();
        }
    }

    @Override
    public <TContinuationResult> Task<TContinuationResult> onSuccessTask(SuccessContinuation<TResult, TContinuationResult> successContinuation) {
        return onSuccessTask(MAIN_THREAD, successContinuation);
    }

    @Override
    public <TContinuationResult> Task<TContinuationResult> onSuccessTask(Executor executor, SuccessContinuation<TResult, TContinuationResult> successContinuation) {
        SuccessContinuationExecutor<TResult, TContinuationResult> c = new SuccessContinuationExecutor<>(executor, successContinuation);
        enqueueOrInvoke(c);
        return c.getTask();
    }
}
