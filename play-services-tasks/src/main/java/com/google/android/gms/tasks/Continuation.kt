/*
 * SPDX-FileCopyrightText: 2016, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */
package com.google.android.gms.tasks

import org.microg.gms.common.PublicApi

/**
 * A function that is called to continue execution after completion of a [Task].
 *
 * @see Task.continueWith
 * @see Task.continueWithTask
 */
@PublicApi
interface Continuation<TResult, TContinuationResult> {
    /**
     * Returns the result of applying this Continuation to `task`.
     *
     *
     * To propagate failure from the completed Task call [Task.getResult] and allow the
     * [RuntimeExecutionException] to propagate. The RuntimeExecutionException will be
     * unwrapped such that the Task returned by [Task.continueWith] or
     * [Task.continueWithTask] fails with the original exception.
     *
     *
     * To suppress specific failures call [Task.getResult] and catch the exception
     * types of interest:
     * <pre>task.continueWith(new Continuation<String></String>, String>() {
     * @Override
     * public String then(Task<String> task) {
     * try {
     * return task.getResult(IOException.class);
     * } catch (FileNotFoundException e) {
     * return "Not found";
     * } catch (IOException e) {
     * return "Read failed";
     * }
     * }
     * }</String></pre>
     *
     *
     * To suppress all failures guard any calls to [Task.getResult] with [Task.isSuccessful]:
     * <pre>task.continueWith(new Continuation<String></String>, String>() {
     * @Override
     * public String then(Task<String> task) {
     * if (task.isSuccessful()) {
     * return task.getResult();
     * } else {
     * return DEFAULT_VALUE;
     * }
     * }
     * }</String></pre>
     *
     * @param task the completed Task. Never null
     * @throws Exception if the result couldn't be produced
     */
    fun then(task: Task<TResult>?): TContinuationResult
}