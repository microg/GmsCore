/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ProvisioningRunner(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )
) {
    private val coordinator = ProvisioningCoordinator(context)
    private var currentJob: Job? = null

    @Synchronized
    fun start(
        request: ProvisioningRequest,
        callback: (ProvisioningResult) -> Unit
    ) {
        currentJob?.cancel()
        currentJob = scope.launch {
            val result = coordinator.provision(request)
            callback(result)
        }
    }

    @Synchronized
    fun cancel() {
        currentJob?.cancel()
        currentJob = null
    }

    fun state(): ProvisioningState = coordinator.state
}