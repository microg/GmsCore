/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.common.api

import com.google.android.gms.tasks.TaskCompletionSource

interface PendingGoogleApiCall<R, A : ApiClient?> {
    fun execute(client: A, completionSource: TaskCompletionSource<R>?)
}