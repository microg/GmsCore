/*
 * SPDX-FileCopyrightText: 2026 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.ipcservers.external.enterprise

import android.app.Service
import android.content.Intent
import android.os.IBinder
import io.grpc.Server
import io.grpc.binder.AndroidComponentAddress
import io.grpc.binder.BinderServerBuilder
import io.grpc.binder.IBinderReceiver
import io.grpc.binder.ServerSecurityPolicy
import org.microg.vending.enterprise.AndroidDevicePolicyCaller
import org.microg.vending.enterprise.PolicySyncService

class EnterpriseDeviceManagementGipcEndpointService : Service() {

    private val binderReceiver = IBinderReceiver()
    private lateinit var server: Server

    override fun onCreate() {
        super.onCreate()
        server = BinderServerBuilder.forAddress(AndroidComponentAddress.forContext(this), binderReceiver)
            .securityPolicy(securityPolicy())
            .addService(PolicySyncService(this).definition())
            .build()
            .start()
    }

    override fun onBind(intent: Intent?): IBinder? = binderReceiver.get()

    override fun onDestroy() {
        server.shutdownNow()
        super.onDestroy()
    }

    private fun securityPolicy(): ServerSecurityPolicy =
        ServerSecurityPolicy.newBuilder()
            .servicePolicy(PolicySyncService.SERVICE_NAME, AndroidDevicePolicyCaller(packageManager))
            .build()
}
