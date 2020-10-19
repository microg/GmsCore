/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */
package com.google.android.gms.tasks

import org.microg.gms.common.PublicApi

/**
 * A function that is called to continue execution then a [Task] succeeds.
 * @see Task.onSuccessTask
 */
@PublicApi
interface SuccessContinuation<TResult, TContinuationResult> {
    /**
     * Returns the result of applying this SuccessContinuation to Task.
     *
     *
     * The SuccessContinuation only happens then the Task is successful.
     * If the previous Task fails, the onSuccessTask continuation will be skipped and
     * failure listeners will be invoked.
     *
     * @param result the result of completed Task
     * @throws Exception if the result couldn't be produced
     */
    @Throws(Exception::class)
    fun then(result: TResult): Task<TContinuationResult>?
}