/*
 * SPDX-FileCopyrightText: 2016, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.tasks;

import org.microg.gms.common.PublicApi;

/**
 * A function that is called to continue execution after completion of a {@link Task}.
 *
 * @see Task#continueWith(Continuation)
 * @see Task#continueWithTask(Continuation)
 */
@PublicApi
public interface Continuation<TResult, TContinuationResult> {
    /**
     * Returns the result of applying this Continuation to {@code task}.
     * <p/>
     * To propagate failure from the completed Task call {@link Task#getResult()} and allow the
     * {@link RuntimeExecutionException} to propagate. The RuntimeExecutionException will be
     * unwrapped such that the Task returned by {@link Task#continueWith(Continuation)} or
     * {@link Task#continueWithTask(Continuation)} fails with the original exception.
     * <p/>
     * To suppress specific failures call {@link Task#getResult(Class)} and catch the exception
     * types of interest:
     * <pre>task.continueWith(new Continuation<String, String>() {
     *     @Override
     *     public String then(Task<String> task) {
     *         try {
     *             return task.getResult(IOException.class);
     *         } catch (FileNotFoundException e) {
     *             return "Not found";
     *         } catch (IOException e) {
     *             return "Read failed";
     *         }
     *     }
     * }</pre>
     * <p/>
     * To suppress all failures guard any calls to {@link Task#getResult()} with {@link Task#isSuccessful()}:
     * <pre>task.continueWith(new Continuation<String, String>() {
     *     @Override
     *     public String then(Task<String> task) {
     *         if (task.isSuccessful()) {
     *             return task.getResult();
     *         } else {
     *             return DEFAULT_VALUE;
     *         }
     *     }
     * }</pre>
     *
     * @param task the completed Task. Never null
     * @throws Exception if the result couldn't be produced
     */
    TContinuationResult then(Task<TResult> task);
}
