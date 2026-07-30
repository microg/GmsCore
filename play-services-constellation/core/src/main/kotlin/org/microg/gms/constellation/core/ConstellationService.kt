/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * Android Service hosting the Constellation API implementation.
 * Handles RCS phone number verification, PNV capabilities queries,
 * and IID token management. Runs in the unstable GMS process.
 */
class ConstellationService : Service() {

    companion object {
        private const val TAG = "ConstellationService"
    }

    private lateinit var apiService: ConstellationApiService

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ConstellationService created")
        val authManager = AuthManager(this)
        val stateStore = ConstellationStateStore(this)
        val gServices = GServices(this)
        val rpcClient = RpcClient()
        val verificationMappings = VerificationMappings(this)
        apiService = ConstellationApiService(this, authManager, stateStore, gServices, rpcClient, verificationMappings)
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "ConstellationService bound, action=${intent?.action}")
        return apiService
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "ConstellationService unbound")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Log.d(TAG, "ConstellationService destroyed")
        super.onDestroy()
    }
}
