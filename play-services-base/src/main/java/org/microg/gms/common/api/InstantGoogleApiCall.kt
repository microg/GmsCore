/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.common.api

import com.google.android.gms.tasks.TaskCompletionSource

interface InstantGoogleApiCall<R, A : ApiClient?> : PendingGoogleApiCall<R, A> {
    @Throws(Exception::class)
    fun execute(client: A): R
    override fun execute(client: A, completionSource: TaskCompletionSource<R>?) {
        try {
            completionSource?.setResult(execute(client))
        } catch (e: Exception) {
            completionSource?.setException(e)
        }
    }
}