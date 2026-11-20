/*
 * SPDX-FileCopyrightText: 2026 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.enterprise

import android.accounts.AccountManager
import android.content.Context
import android.util.Log
import io.grpc.MethodDescriptor
import io.grpc.ServerServiceDefinition
import io.grpc.stub.ServerCalls
import io.grpc.stub.StreamObserver
import org.microg.gms.auth.AuthConstants
import java.io.ByteArrayInputStream
import java.io.InputStream

class PolicySyncService(private val context: Context) {

    fun definition(): ServerServiceDefinition =
        ServerServiceDefinition.builder(SERVICE_NAME)
            .addMethod(SYNC_POLICY, ServerCalls.asyncUnaryCall(::syncPolicy))
            .build()

    private fun syncPolicy(request: ByteArray, response: StreamObserver<ByteArray>) {
        Log.i(TAG, "Policy sync requested by Android Device Policy (${request.size} bytes)")

        if (hasWorkAccount()) {
            MandatoryAppsJob.scheduleImmediate(context)
        } else {
            Log.i(TAG, "No work account, nothing to sync")
        }

        response.onNext(EMPTY_RESPONSE)
        response.onCompleted()
    }

    private fun hasWorkAccount(): Boolean =
        AccountManager.get(context).getAccountsByType(AuthConstants.WORK_ACCOUNT_TYPE).isNotEmpty()

    private object RawMessage : MethodDescriptor.Marshaller<ByteArray> {

        override fun stream(value: ByteArray): InputStream = ByteArrayInputStream(value)

        override fun parse(stream: InputStream): ByteArray = stream.readBytes()
    }

    companion object {
        const val SERVICE_NAME = "com.google.android.finsky.ipc.enterprise.EnterpriseDeviceManagementService"

        private const val TAG = "GmsVendingPolicySync"

        private val EMPTY_RESPONSE = ByteArray(0)

        private val SYNC_POLICY = MethodDescriptor.newBuilder(RawMessage, RawMessage)
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, "SyncPolicy"))
            .build()
    }
}
