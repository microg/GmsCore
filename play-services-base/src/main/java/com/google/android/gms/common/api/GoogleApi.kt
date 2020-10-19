/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.common.api

import android.content.Context
import com.google.android.gms.common.api.Api.ApiOptions
import com.google.android.gms.common.api.internal.ApiKey
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import org.microg.gms.common.PublicApi
import org.microg.gms.common.api.ApiClient
import org.microg.gms.common.api.GoogleApiManager
import org.microg.gms.common.api.PendingGoogleApiCall

@PublicApi
abstract class GoogleApi<O : ApiOptions?> @PublicApi(exclude = true) protected constructor(
        context: Context?,
        @field:PublicApi(exclude = true)
        var api: Api<O>
) : HasApiKey<O> {
    private val manager: GoogleApiManager = GoogleApiManager.getInstance(context)

    fun getAPI(): Api<O> = api

    @PublicApi(exclude = true)
    protected fun <R, A : ApiClient?> scheduleTask(apiCall: PendingGoogleApiCall<R, A>?): Task<R> {
        val completionSource = TaskCompletionSource<R>()
        manager.scheduleTask(this, apiCall, completionSource)
        return completionSource.task
    }

    @PublicApi(exclude = true)
    override fun getApiKey(): ApiKey<O>? = null

    @PublicApi(exclude = true)
    fun getOptions(): O? = null

}