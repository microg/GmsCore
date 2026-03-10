/*
 * SPDX-FileCopyrightText: 2016 microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.tasks;

import android.os.Handler;
import android.os.Looper;

import org.microg.gms.common.PublicApi;
import org.microg.gms.tasks.CancellationTokenImpl;
import org.microg.gms.tasks.TaskImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link Task} utility methods.
 */
@PublicApi
public final class Tasks {

    /**
     * Blocks until the specified Task is complete.
     *
     * @return the Task's result
     * @throws ExecutionException   if the Task fails
     * @throws InterruptedException if an interrupt occurs while waiting for the Task to complete
     * @throws TimeoutException     if the specified timeout is reached before the Task completes
     */
    public static <TResult> TResult await(Task<TResult> task, long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        if (task == null) throw new IllegalArgumentException("Task must not be null");
        if (timeout <= 0) throw new IllegalArgumentException("Timeout must be positive");
        if (unit == null) throw new IllegalArgumentException("TimeUnit must not be null");
        if (task.isComplete()) return handleCompletedTask(task);
        CountDownLatch latch = new CountDownLatch(1);
        task.addOnCompleteListener(Runnable::run, completedTask -> latch.countDown());
        if (latch.await(timeout, unit)) {
            return handleCompletedTask(task);
        }
        throw new TimeoutException("Timed out waiting for Task");
    }

    /**
     * Blocks until the specified Task is complete.
     *
     * @return the Task's result
     * @throws ExecutionException   if the Task fails
     * @throws InterruptedException if an interrupt occurs while waiting for the Task to complete
     */
    public static <TResult> TResult await(Task<TResult> task) throws ExecutionException, InterruptedException {
        if (Looper.getMainLooper().getThread() == Thread.currentThread())
            throw new IllegalStateException("Must not be invoked on main thread");
        if (task == null) throw new IllegalArgumentException("Task must not be null");
        if (task.isComplete()) return handleCompletedTask(task);
        CountDownLatch latch = new CountDownLatch(1);
        task.addOnCompleteListener(Runnable::run, completedTask -> latch.countDown());
        latch.await();
        return handleCompletedTask(task);
    }

    private static <TResult> TResult handleCompletedTask(Task<TResult> task) throws ExecutionException {
        if (task.isSuccessful()) {
            return task.getResult();
        }
        if (task.isCanceled()) {
            throw new CancellationException("Task is already canceled");
        }
        throw new ExecutionException(task.getException());
    }

    /**
     * Returns a {@link Task} that will be completed with the result of the specified {@code Callable}.
     * <p/>
     * If a non-{@link Exception} throwable is thrown in the callable, the {@link Task} will be failed with a
     * {@link RuntimeException} whose cause is the original throwable.
     * <p/>
     * The {@code Callable} will be called on the main application thread.
     *
     * @deprecated Use {@link TaskCompletionSource} instead, which allows the caller to manage their own Executor.
     */
    @Deprecated
    public static <TResult> Task<TResult> call(Callable<TResult> callable) {
        return call(TaskExecutors.MAIN_THREAD, callable);
    }

    /**
     * Returns a {@link Task} that will be completed with the result of the specified {@code Callable}.
     * <p/>
     * If a non-{@link Exception} throwable is thrown in the callable, the {@link Task} will be failed with a
     * {@link RuntimeException} whose cause is the original throwable.
     *
     * @param executor the Executor to use to call the {@code Callable}
     * @deprecated Use {@link TaskCompletionSource} instead, which allows the caller to manage their own Executor.
     */
    @Deprecated
    public static <TResult> Task<TResult> call(Executor executor, Callable<TResult> callable) {
        if (executor == null) throw new IllegalArgumentException("Executor must not be null");
        if (callable == null) throw new IllegalArgumentException("Callable must not be null");
        TaskCompletionSource<TResult> taskCompletionSource = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                taskCompletionSource.setResult(callable.call());
            } catch (Exception e) {
                taskCompletionSource.trySetException(e);
            } catch (Throwable t) {
                taskCompletionSource.trySetException(new RuntimeException(t));
            }
        });
        return taskCompletionSource.getTask();
    }

    /**
     * Returns a canceled Task.
     */
    public static <TResult> Task<TResult> forCancelled() {
        TaskImpl<TResult> task = new TaskImpl<>();
        task.cancel();
        return task;
    }

    /**
     * Returns a completed Task with the specified exception.
     */
    public static <TResult> Task<TResult> forException(Exception e) {
        TaskImpl<TResult> task = new TaskImpl<>();
        task.setException(e);
        return task;
    }

    /**
     * Returns a completed Task with the specified result.
     */
    public static <TResult> Task<TResult> forResult(TResult result) {
        TaskImpl<TResult> task = new TaskImpl<>();
        task.setResult(result);
        return task;
    }

    /**
     * Returns a Task that completes successfully when all of the specified Tasks complete
     * successfully. Does not accept nulls.
     * <p/>
     * The returned Task would fail if any of the provided Tasks fail. The returned Task would be set to canceled if
     * any of the provided Tasks is canceled and no failure is detected.
     *
     * @throws NullPointerException if any of the provided Tasks are null
     */
    public static Task<Void> whenAll(Collection<? extends Task<?>> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return forResult(null);
        }
        for (Task<?> task : tasks) {
            if (task == null) throw new NullPointerException("null tasks are not accepted");
        }
        TaskImpl<Void> allTask = new TaskImpl<>();
        AtomicInteger finishedTasks = new AtomicInteger(0);
        AtomicInteger failedTasks = new AtomicInteger(0);
        AtomicReference<Exception> exceptionReference = new AtomicReference<>(null);
        AtomicBoolean isCancelled = new AtomicBoolean(false);
        for (Task<?> task : tasks) {
            task.addOnCompleteListener(Runnable::run, completedTask -> {
                if (!completedTask.isSuccessful()) {
                    if (completedTask.isCanceled()) {
                        isCancelled.set(true);
                    } else {
                        exceptionReference.set(completedTask.getException());
                        failedTasks.incrementAndGet();
                    }
                }
                if (finishedTasks.incrementAndGet() != tasks.size()) return;
                Exception exception = exceptionReference.get();
                if (exception != null) {
                    allTask.setException(new ExecutionException(failedTasks.get() + " out of " + tasks.size() + " underlying tasks failed", exception));
                } else if (isCancelled.get()) {
                    allTask.cancel();
                } else {
                    allTask.setResult(null);
                }
            });
        }
        return allTask;
    }

    /**
     * Returns a Task that completes successfully when all of the specified Tasks complete
     * successfully. Does not accept nulls.
     * <p/>
     * The returned Task would fail if any of the provided Tasks fail. The returned Task would be set to canceled if
     * any of the provided Tasks is canceled and no failure is detected.
     *
     * @throws NullPointerException if any of the provided Tasks are null
     */
    public static Task<Void> whenAll(Task<?>... tasks) {
        if (tasks == null || tasks.length == 0) {
            return forResult(null);
        }
        return whenAll(Arrays.asList(tasks));
    }

    /**
     * Returns a Task with a list of Tasks that completes successfully when all of the specified Tasks complete. This
     * Task would always succeed even if any of the provided Tasks fail or canceled. Does not accept nulls.
     *
     * @throws NullPointerException if any of the provided Tasks are null
     */
    public static Task<List<Task<?>>> whenAllComplete(Task<?>... tasks) {
        if (tasks == null || tasks.length == 0) {
            return forResult(Collections.emptyList());
        }
        return whenAllComplete(Arrays.asList(tasks));
    }

    /**
     * Returns a Task with a list of Tasks that completes successfully when all of the specified Tasks complete. This
     * Task would always succeed even if any of the provided Tasks fail or canceled. Does not accept nulls.
     *
     * @throws NullPointerException if any of the provided Tasks are null
     */
    public static Task<List<Task<?>>> whenAllComplete(Collection<? extends Task<?>> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return forResult(Collections.emptyList());
        }
        return whenAll(tasks).continueWithTask(TaskExecutors.MAIN_THREAD, allTask -> forResult(new ArrayList<>(tasks)));
    }

    /**
     * Returns a Task with a list of Task results that completes successfully when all of the specified Tasks complete
     * successfully. This Task would fail if any of the provided Tasks fail. Does not accept nulls.
     * <p/>
     * This Task would be set to canceled if any of the provided Tasks is canceled and no failure is detected.
     *
     * @throws NullPointerException if any of the provided Tasks are null
     */
    public static <TResult> Task<List<TResult>> whenAllSuccess(Task<? extends TResult>... tasks) {
        if (tasks == null || tasks.length == 0) {
            return forResult(Collections.emptyList());
        }
        return whenAllSuccess(Arrays.asList(tasks));
    }

    /**
     * Returns a Task with a list of Task results that completes successfully when all of the specified Tasks complete
     * successfully. This Task would fail if any of the provided Tasks fail. Does not accept nulls.
     * <p/>
     * This Task would be set to canceled if any of the provided Tasks is canceled and no failure is detected.
     *
     * @throws NullPointerException if any of the provided Tasks are null
     */
    public static <TResult> Task<List<TResult>> whenAllSuccess(Collection<? extends Task<? extends TResult>> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return forResult(Collections.emptyList());
        }
        return whenAll(tasks).continueWithTask(TaskExecutors.MAIN_THREAD, allTask -> {
            List<TResult> results = new ArrayList<>(tasks.size());
            for (Task<? extends TResult> task : tasks) {
                results.add(task.getResult());
            }
            return forResult(results);
        });
    }

    /**
     * Returns a new Task which will return a TimeoutException if a result is not returned within the specified time
     * period.
     *
     * @return A new Task.
     */
    public static <T> Task<T> withTimeout(Task<T> task, long timeout, TimeUnit unit) {
        if (task == null) throw new IllegalArgumentException("Task must not be null");
        if (timeout <= 0) throw new IllegalArgumentException("Timeout must be positive");
        if (unit == null) throw new IllegalArgumentException("TimeUnit must not be null");
        CancellationTokenImpl cancellationToken = new CancellationTokenImpl();
        TaskCompletionSource<T> taskCompletionSource = new TaskCompletionSource<>(cancellationToken);
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> taskCompletionSource.trySetException(new TimeoutException()), unit.toMillis(timeout));
        task.addOnCompleteListener(completedTask -> {
            handler.removeCallbacksAndMessages(null);
            if (completedTask.isSuccessful()) {
                taskCompletionSource.trySetResult(completedTask.getResult());
            } else if (completedTask.isCanceled()) {
                cancellationToken.cancel();
            } else {
                taskCompletionSource.trySetException(completedTask.getException());
            }
        });
        return taskCompletionSource.getTask();
    }
}
