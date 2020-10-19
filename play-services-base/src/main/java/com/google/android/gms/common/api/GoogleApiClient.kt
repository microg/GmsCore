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
package com.google.android.gms.common.api

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.Api.ApiOptions
import com.google.android.gms.common.api.Api.ApiOptions.HasOptions
import com.google.android.gms.common.api.Api.ApiOptions.NotRequiredOptions
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import org.microg.gms.auth.AuthConstants
import org.microg.gms.common.PublicApi
import org.microg.gms.common.api.ApiClientSettings
import org.microg.gms.common.api.GoogleApiClientImpl
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * The main entry point for Google Play services integration.
 *
 *
 * GoogleApiClient is used with a variety of static methods. Some of these methods require that
 * GoogleApiClient be connected, some will queue up calls before GoogleApiClient is connected;
 * check the specific API documentation to determine whether you need to be connected.
 *
 *
 * Before any operation is executed, the GoogleApiClient must be connected using the
 * [.connect] method. The client is not considered connected until the
 * [ConnectionCallbacks.onConnected] callback has been called.
 *
 *
 * When your app is done using this client, call [.disconnect], even if the async result
 * from [.connect] has not yet been delivered.
 *
 *
 * You should instantiate a client object in your Activity's [Activity.onCreate]
 * method and then call [.connect] in [Activity.onStart] and [.disconnect]
 * in [Activity.onStop], regardless of the state.
 */
@PublicApi
@Deprecated("")
interface GoogleApiClient {
    /**
     * Connects the client to Google Play services. Blocks until the connection either succeeds or
     * fails. This is not allowed on the UI thread.
     *
     * @return the result of the connection
     */
    fun blockingConnect(): ConnectionResult?

    /**
     * Connects the client to Google Play services. Blocks until the connection is set or failed or
     * has timed out. This is not allowed on the UI thread.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the `timeout` argument
     * @return the result of the connection
     */
    fun blockingConnect(timeout: Long, unit: TimeUnit?): ConnectionResult?

    /**
     * Clears the account selected by the user and reconnects the client asking the user to pick an
     * account again if [Builder.useDefaultAccount] was set.
     *
     * @return the pending result is fired once the default account has been cleared, but before
     * the client is reconnected - for that [ConnectionCallbacks] can be used.
     */
    fun clearDefaultAccountAndReconnect(): PendingResult<Status?>?

    /**
     * Connects the client to Google Play services. This method returns immediately, and connects
     * to the service in the background. If the connection is successful,
     * [ConnectionCallbacks.onConnected] is called and enqueued items are executed.
     * On a failure, [OnConnectionFailedListener.onConnectionFailed] is
     * called.
     */
    fun connect()

    /**
     * Closes the connection to Google Play services. No calls can be made using this client after
     * calling this method. Any method calls that haven't executed yet will be canceled. That is
     * [ResultCallback.onResult] won't be called, if connection to the service hasn't
     * been established yet all calls already made will be canceled.
     *
     * @see .connect
     */
    fun disconnect()

    /**
     * Checks if the client is currently connected to the service, so that requests to other
     * methods will succeed. Applications should guard client actions caused by the user with a
     * call to this method.
     *
     * @return `true` if the client is connected to the service.
     */
    fun isConnected(): Boolean

    /**
     * Checks if the client is attempting to connect to the service.
     *
     * @return `true` if the client is attempting to connect to the service.
     */
    fun isConnecting(): Boolean

    /**
     * Returns `true` if the specified listener is currently registered to receive connection
     * events.
     *
     * @param listener The listener to check for.
     * @return `true` if the specified listener is currently registered to receive connection
     * events.
     * @see .registerConnectionCallbacks
     * @see .unregisterConnectionCallbacks
     */
    fun isConnectionCallbacksRegistered(listener: ConnectionCallbacks?): Boolean

    /**
     * Returns `true` if the specified listener is currently registered to receive connection
     * failed events.
     *
     * @param listener The listener to check for.
     * @return `true` if the specified listener is currently registered to receive connection
     * failed events.
     * @see .registerConnectionFailedListener
     * @see .unregisterConnectionFailedListener
     */
    fun isConnectionFailedListenerRegistered(listener: OnConnectionFailedListener?): Boolean

    /**
     * Closes the current connection to Google Play services and creates a new connection.
     *
     *
     * This method closes the current connection then returns immediately and reconnects to the
     * service in the background.
     *
     *
     * After calling this method, your application will receive
     * [ConnectionCallbacks.onConnected] if the connection is successful, or
     * [OnConnectionFailedListener.onConnectionFailed] if the connection
     * failed.
     *
     * @see .connect
     * @see .disconnect
     */
    fun reconnect()

    /**
     * Registers a listener to receive connection events from this [GoogleApiClient]. If the
     * service is already connected, the listener's [ConnectionCallbacks.onConnected]
     * method will be called immediately. Applications should balance calls to this method with
     * calls to [.unregisterConnectionCallbacks] to avoid leaking
     * resources.
     *
     *
     * If the specified listener is already registered to receive connection events, this method
     * will not add a duplicate entry for the same listener, but will still call the listener's
     * [ConnectionCallbacks.onConnected] method if currently connected.
     *
     *
     * Note that the order of messages received here may not be stable, so clients should not rely
     * on the order that multiple listeners receive events in.
     *
     * @param listener the listener where the results of the asynchronous [.connect] call
     * are delivered.
     */
    fun registerConnectionCallbacks(listener: ConnectionCallbacks?)

    /**
     * Registers a listener to receive connection failed events from this [GoogleApiClient].
     * Unlike [.registerConnectionCallbacks], if the service is not
     * already connected, the listener's
     * [OnConnectionFailedListener.onConnectionFailed] method will not be
     * called immediately. Applications should balance calls to this method with calls to
     * [.unregisterConnectionFailedListener] to avoid leaking
     * resources.
     *
     *
     * If the specified listener is already registered to receive connection failed events, this
     * method will not add a duplicate entry for the same listener.
     *
     *
     * Note that the order of messages received here may not be stable, so clients should not rely
     * on the order that multiple listeners receive events in.
     *
     * @param listener the listener where the results of the asynchronous [.connect] call
     * are delivered.
     */
    fun registerConnectionFailedListener(listener: OnConnectionFailedListener?)

    /**
     * Disconnects the client and stops automatic lifecycle management. Use this before creating a
     * new client (which might be necessary when switching accounts, changing the set of used APIs
     * etc.).
     *
     *
     * This method must be called from the main thread.
     *
     * @param lifecycleActivity the activity managing the client's lifecycle.
     * @throws IllegalStateException if called from outside of the main thread.
     * @see Builder.enableAutoManage
     */
    @Throws(IllegalStateException::class)
    fun stopAutoManager(lifecycleActivity: FragmentActivity?)

    /**
     * Removes a connection listener from this [GoogleApiClient]. Note that removing a
     * listener does not generate any callbacks.
     *
     *
     * If the specified listener is not currently registered to receive connection events, this
     * method will have no effect.
     *
     * @param listener the listener to unregister.
     */
    fun unregisterConnectionCallbacks(listener: ConnectionCallbacks?)

    /**
     * Removes a connection failed listener from the [GoogleApiClient]. Note that removing a
     * listener does not generate any callbacks.
     *
     *
     * If the specified listener is not currently registered to receive connection failed events,
     * this method will have no effect.
     *
     * @param listener the listener to unregister.
     */
    fun unregisterConnectionFailedListener(listener: OnConnectionFailedListener?)

    /**
     * Builder to configure a [GoogleApiClient].
     */
    @PublicApi
    class Builder(private val context: Context) {
        private val apis: MutableMap<Api<*>, ApiOptions?> = HashMap()
        private val connectionCallbacks: MutableSet<ConnectionCallbacks> = HashSet()
        private val connectionFailedListeners: MutableSet<OnConnectionFailedListener> = HashSet()
        private val scopes: MutableSet<String> = HashSet()
        private var accountName: String? = null
        private var clientId = -1
        private var fragmentActivity: FragmentActivity? = null
        private var looper: Looper
        private var gravityForPopups = 0
        private var unresolvedConnectionFailedListener: OnConnectionFailedListener? = null
        private var viewForPopups: View? = null

        /**
         * Builder to help construct the [GoogleApiClient] object.
         *
         * @param context                  The context to use for the connection.
         * @param connectedListener        The listener where the results of the asynchronous
         * [.connect] call are delivered.
         * @param connectionFailedListener The listener which will be notified if the connection
         * attempt fails.
         */
        constructor(context: Context, connectedListener: ConnectionCallbacks,
                    connectionFailedListener: OnConnectionFailedListener) : this(context) {
            addConnectionCallbacks(connectedListener)
            addOnConnectionFailedListener(connectionFailedListener)
        }

        /**
         * Specify which Apis are requested by your app. See [Api] for more information.
         *
         * @param api     The Api requested by your app.
         * @param options Any additional parameters required for the specific AP
         * @see Api
         */
        fun <O : HasOptions?> addApi(api: Api<O>, options: O): Builder {
            apis[api] = options
            return this
        }

        /**
         * Specify which Apis are requested by your app. See [Api] for more information.
         *
         * @param api The Api requested by your app.
         * @see Api
         */
        fun addApi(api: Api<out NotRequiredOptions?>): Builder {
            apis[api] = null
            return this
        }

        /**
         * Registers a listener to receive connection events from this [GoogleApiClient].
         * Applications should balance calls to this method with calls to
         * [.unregisterConnectionCallbacks] to avoid
         * leaking resources.
         *
         *
         * If the specified listener is already registered to receive connection events, this
         * method will not add a duplicate entry for the same listener.
         *
         *
         * Note that the order of messages received here may not be stable, so clients should not
         * rely on the order that multiple listeners receive events in.
         *
         * @param listener the listener where the results of the asynchronous [.connect]
         * call are delivered.
         */
        fun addConnectionCallbacks(listener: ConnectionCallbacks): Builder {
            connectionCallbacks.add(listener)
            return this
        }

        /**
         * Adds a listener to register to receive connection failed events from this
         * [GoogleApiClient]. Applications should balance calls to this method with calls to
         * [.unregisterConnectionFailedListener] to avoid
         * leaking resources.
         *
         *
         * If the specified listener is already registered to receive connection failed events,
         * this method will not add a duplicate entry for the same listener.
         *
         *
         * Note that the order of messages received here may not be stable, so clients should not
         * rely on the order that multiple listeners receive events in.
         *
         * @param listener the listener where the results of the asynchronous [.connect]
         * call are delivered.
         */
        fun addOnConnectionFailedListener(listener: OnConnectionFailedListener): Builder {
            connectionFailedListeners.add(listener)
            return this
        }

        /**
         * Specify the OAuth 2.0 scopes requested by your app. See
         * [com.google.android.gms.common.Scopes] for more information.
         *
         * @param scope The OAuth 2.0 scopes requested by your app.
         * @see com.google.android.gms.common.Scopes
         */
        fun addScope(scope: Scope): Builder {
            scopes.add(scope.scopeUri)
            return this
        }

        /**
         * Builds a new [GoogleApiClient] object for communicating with the Google APIs.
         *
         * @return The [GoogleApiClient] object.
         */
        fun build(): GoogleApiClient {
            return GoogleApiClientImpl(context, looper, getClientSettings(), apis, connectionCallbacks, connectionFailedListeners, clientId)
        }

        private fun getClientSettings(): ApiClientSettings? = null

        @Throws(NullPointerException::class, IllegalArgumentException::class, IllegalStateException::class)
        fun enableAutoManage(fragmentActivity: FragmentActivity?, cliendId: Int,
                             unresolvedConnectionFailedListener: OnConnectionFailedListener?): Builder {
            this.fragmentActivity = fragmentActivity
            clientId = cliendId
            this.unresolvedConnectionFailedListener = unresolvedConnectionFailedListener
            return this
        }

        /**
         * Specify an account name on the device that should be used. If this is never called, the
         * client will use the current default account for Google Play services for this
         * application.
         *
         * @param accountName The account name on the device that should be used by
         * [GoogleApiClient].
         */
        fun setAccountName(accountName: String?): Builder {
            this.accountName = accountName
            return this
        }

        /**
         * Specifies the part of the screen at which games service popups (for example,
         * "welcome back" or "achievement unlocked" popups) will be displayed using gravity.
         *
         * @param gravityForPopups The gravity which controls the placement of games service popups.
         */
        fun setGravityForPopups(gravityForPopups: Int): Builder {
            this.gravityForPopups = gravityForPopups
            return this
        }

        /**
         * Sets a [Handler] to indicate which thread to use when invoking callbacks. Will not
         * be used directly to handle callbacks. If this is not called then the application's main
         * thread will be used.
         */
        fun setHandler(handler: Handler): Builder {
            looper = handler.looper
            return this
        }

        /**
         * Sets the [View] to use as a content view for popups.
         *
         * @param viewForPopups The view to use as a content view for popups. View cannot be null.
         */
        fun setViewForPopups(viewForPopups: View?): Builder {
            this.viewForPopups = viewForPopups
            return this
        }

        /**
         * Specify that the default account should be used when connecting to services.
         */
        fun useDefaultAccount(): Builder {
            accountName = AuthConstants.DEFAULT_ACCOUNT
            return this
        }

        /**
         * Builder to help construct the [GoogleApiClient] object.
         *
         * @param context The context to use for the connection.
         */
        init {
            looper = context.mainLooper
        }
    }

    /**
     * Provides callbacks that are called when the client is connected or disconnected from the
     * service. Most applications implement [.onConnected] to start making requests.
     */
    @PublicApi
    @Deprecated("")
    interface ConnectionCallbacks : org.microg.gms.common.api.ConnectionCallbacks {
        companion object {
            /**
             * A suspension cause informing that the service has been killed.
             */
            const val CAUSE_SERVICE_DISCONNECTED = 1

            /**
             * A suspension cause informing you that a peer device connection was lost.
             */
            const val CAUSE_NETWORK_LOST = 2
        }
    }

    /**
     * Provides callbacks for scenarios that result in a failed attempt to connect the client to
     * the service. See [ConnectionResult] for a list of error codes and suggestions for
     * resolution.
     */
    @PublicApi
    @Deprecated("")
    interface OnConnectionFailedListener : org.microg.gms.common.api.OnConnectionFailedListener
}