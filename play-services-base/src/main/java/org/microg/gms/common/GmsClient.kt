/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.microg.gms.common

import android.accounts.Account
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.common.internal.IGmsServiceBroker
import org.microg.gms.common.api.OnConnectionFailedListener.onConnectionFailed
import org.microg.gms.common.api.ConnectionCallbacks.onConnected
import android.os.IInterface
import org.microg.gms.common.api.ApiClient
import org.microg.gms.common.GmsClient.ConnectionState
import android.content.ServiceConnection
import android.os.Bundle
import kotlin.Throws
import org.microg.gms.common.GmsClient.GmsCallbacks
import com.google.android.gms.common.internal.GetServiceRequest
import android.os.IBinder
import kotlin.jvm.Synchronized
import org.microg.gms.common.GmsClient
import org.microg.gms.common.MultiConnectionKeeper
import org.microg.gms.common.GmsClient.GmsServiceConnection
import com.google.android.gms.common.ConnectionResult
import android.content.ComponentName
import android.content.Context
import android.os.RemoteException
import android.util.Log
import com.google.android.gms.common.internal.ConnectionInfo
import org.microg.gms.common.api.ConnectionCallbacks
import org.microg.gms.common.api.OnConnectionFailedListener

abstract class GmsClient<I : IInterface?>(
        val context: Context,
        protected val callbacks: ConnectionCallbacks,
        protected val connectionFailedListener: OnConnectionFailedListener,
        private val actionString: String
) : ApiClient {
    @JvmField
    protected var state = ConnectionState.NOT_CONNECTED
    private var serviceConnection: ServiceConnection? = null
    private var serviceInterface: I? = null
    @JvmField
    protected var serviceId = -1
    protected var account: Account? = null
    @JvmField
    protected var extras = Bundle()
    @Throws(RemoteException::class)
    protected open fun onConnectedToBroker(broker: IGmsServiceBroker, callbacks: GmsCallbacks?) {
        check(serviceId != -1) { "Service ID not set in constructor and onConnectedToBroker not implemented" }
        val request = GetServiceRequest(serviceId)
        request.extras = Bundle()
        request.packageName = context.packageName
        request.account = account
        request.extras = extras
        broker.getService(callbacks, request)
    }

    protected abstract fun interfaceFromBinder(binder: IBinder?): I
    @Synchronized
    override fun connect() {
        Log.d(TAG, "connect()")
        if (state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING) {
            Log.d(TAG, "Already connected/connecting - nothing to do")
        }
        state = ConnectionState.CONNECTING
        if (serviceConnection != null) {
            MultiConnectionKeeper.getInstance(context).unbind(actionString, serviceConnection)
        }
        serviceConnection = GmsServiceConnection()
        if (!MultiConnectionKeeper.getInstance(context).bind(actionString, serviceConnection)) {
            state = ConnectionState.ERROR
            handleConnectionFailed()
        }
    }

    open fun handleConnectionFailed() {
        connectionFailedListener.onConnectionFailed(ConnectionResult(ConnectionResult.API_UNAVAILABLE, null))
    }

    @Synchronized
    override fun disconnect() {
        Log.d(TAG, "disconnect()")
        if (state == ConnectionState.DISCONNECTING) return
        if (state == ConnectionState.CONNECTING) {
            state = ConnectionState.DISCONNECTING
            return
        }
        serviceInterface = null
        if (serviceConnection != null) {
            MultiConnectionKeeper.getInstance(context).unbind(actionString, serviceConnection)
            serviceConnection = null
        }
        state = ConnectionState.NOT_CONNECTED
    }

    @Synchronized
    override fun isConnected(): Boolean = state == ConnectionState.CONNECTED || state == ConnectionState.PSEUDO_CONNECTED

    @Synchronized
    override fun isConnecting(): Boolean = state == ConnectionState.CONNECTING

    @Synchronized
    fun hasError(): Boolean {
        return state == ConnectionState.ERROR
    }

    @Synchronized
    fun getServiceInterface(): I? {
        check(!isConnecting()) {
            // TODO: wait for connection to be established and return afterwards.
            "Waiting for connection"
        }
        check(isConnected()) { "interface only available once connected!" }
        return serviceInterface
    }

    protected enum class ConnectionState {
        NOT_CONNECTED, CONNECTING, CONNECTED, DISCONNECTING, ERROR, PSEUDO_CONNECTED
    }

    private inner class GmsServiceConnection : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            try {
                Log.d(TAG, "ServiceConnection : onServiceConnected($componentName)")
                onConnectedToBroker(IGmsServiceBroker.Stub.asInterface(iBinder), GmsCallbacks())
            } catch (e: RemoteException) {
                disconnect()
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            synchronized(this@GmsClient) { state = ConnectionState.NOT_CONNECTED }
        }
    }

    inner class GmsCallbacks : IGmsCallbacks.Stub() {
        @Throws(RemoteException::class)
        fun onPostInitComplete(statusCode: Int, binder: IBinder?, params: Bundle?) {
            synchronized(this@GmsClient) {
                if (state == ConnectionState.DISCONNECTING) {
                    state = ConnectionState.CONNECTED
                    disconnect()
                    return
                }
                state = ConnectionState.CONNECTED
                serviceInterface = interfaceFromBinder(binder)
            }
            Log.d(TAG, "GmsCallbacks : onPostInitComplete($serviceInterface)")
            callbacks.onConnected(params)
        }

        @Throws(RemoteException::class)
        fun onAccountValidationComplete(statusCode: Int, params: Bundle?) {
            Log.d(TAG, "GmsCallbacks : onAccountValidationComplete")
        }

        @Throws(RemoteException::class)
        fun onPostInitCompleteWithConnectionInfo(statusCode: Int, binder: IBinder?, info: ConnectionInfo?) {
            onPostInitComplete(statusCode, binder, info?.params)
        }
    }

    companion object {
        private const val TAG = "GmsClient"
    }
}