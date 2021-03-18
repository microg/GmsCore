/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard

import android.content.Context
import android.os.Looper
import com.google.android.gms.common.api.Api
import com.google.android.gms.common.api.Api.ApiOptions.NoOptions
import com.google.android.gms.common.api.GoogleApi
import com.google.android.gms.tasks.Task
import org.microg.gms.common.api.ApiClientBuilder
import org.microg.gms.common.api.ApiClientSettings
import org.microg.gms.common.api.ConnectionCallbacks
import org.microg.gms.common.api.OnConnectionFailedListener

class DroidGuardClientImpl(context: Context) : GoogleApi<NoOptions>(context, API), DroidGuardClient {
    companion object {
        private val API = Api(ApiClientBuilder { _: NoOptions?, context: Context, _: Looper?, _: ApiClientSettings?, callbacks: ConnectionCallbacks, connectionFailedListener: OnConnectionFailedListener -> DroidGuardApiClient(context, callbacks, connectionFailedListener) })
    }

    override fun getHandle(): Task<DroidGuardHandle> {
        return scheduleTask { client: DroidGuardApiClient, completionSource ->
            try {
                completionSource.setResult(DroidGuardHandle(client.getHandle()))
            } catch (e: Exception) {
                completionSource.setException(e)
            }
        }
    }
}
