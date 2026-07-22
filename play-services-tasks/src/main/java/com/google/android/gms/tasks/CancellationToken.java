/*
 * SPDX-FileCopyrightText: 2020 microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.tasks;

import org.microg.gms.common.PublicApi;

/**
 * Propagates notification that operations should be canceled.
 * <p/>
 * Developers writing methods that return a Task should take a {@code CancellationToken} as a parameter if they wish to
 * make the Task cancelable (see below code snippet). A {@code CancellationToken} can only be created by creating a new
 * instance of {@link CancellationTokenSource}. {@code CancellationToken} is immutable and must be canceled by calling
 * {@link CancellationTokenSource#cancel()} on the {@link CancellationTokenSource} that creates it. It can only be
 * canceled once. If canceled, it should not be passed to future operations.
 * <p/>
 * When {@link CancellationTokenSource#cancel()} is called, all the Tasks with the {@code CancellationToken} from that
 * {@link CancellationTokenSource} will be canceled. This operation only flags those Tasks as canceled, and the API
 * author is responsible for stopping whatever the Task is actually doing to free up the resources.
 * <p/>
 * Cancellable {@link Task} example:
 * <pre>
 * public Task<Integer> doSomething(CancellationToken token) {
 *
 *     // Attach a listener that will be called once cancellation is requested.
 *     token.onCanceledRequested(new OnTokenCanceledListener() {
 *         &#64;Override
 *         public void onCanceled() {
 *             // Some other operations to cancel this Task, such as free resources...
 *         }
 *     });
 *
 *     final TaskCompletionSource<Integer> tcs = new TaskCompletionSource<>(token);
 *
 *     // do something...
 *
 * }
 *
 * CancellationTokenSource cts = new CancellationTokenSource();
 * Task<Integer> task = doSomething(cts.getToken());
 * cts.cancel();
 * </pre>
 * Cancellable {@link Task} example in {@link android.app.Activity} context:
 * <pre>
 * public class MyActivity extends Activity {
 *     // ...
 *
 *     &#64;Override
 *     public void onStart() {
 *         super.onStart();
 *
 *         // Typically use one cancellation source per lifecycle.
 *         cancellationSource = new TaskCancellationSource();
 *
 *         // That source's token can be passed to multiple calls.
 *         doSomethingCancellable(cancellationSource.getToken())
 *             .onSuccessTask(result -> doSomethingElse(result, cancellationSource.getToken()));
 *     }
 *
 *     &#64;Override
 *     public void onStop() {
 *         super.onStop();
 *         cancellationSource.cancel();
 *     }
 * }
 * </pre>
 */
@PublicApi
public abstract class CancellationToken {
    /**
     * Checks if cancellation has been requested from the {@link CancellationTokenSource}.
     *
     * @return {@code true} if cancellation is requested, {@code false} otherwise
     */
    public abstract boolean isCancellationRequested();

    /**
     * Adds an {@link OnTokenCanceledListener} to this {@link CancellationToken}.
     *
     * @param listener the listener that will fire once the cancellation request succeeds.
     */
    public abstract CancellationToken onCanceledRequested(OnTokenCanceledListener listener);
}
