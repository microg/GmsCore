/*
 * SPDX-FileCopyrightText: 2013 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.api;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;

import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.internal.ListenerHolder;
import com.google.android.gms.common.api.internal.SignInConnectionListener;
import com.google.android.gms.signin.SignInOptions;
import org.microg.gms.auth.AuthConstants;
import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;
import com.google.android.gms.common.internal.ClientSettings;
import org.microg.gms.common.api.GoogleApiClientImpl;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * The main entry point for Google Play services integration.
 * <p/>
 * GoogleApiClient is used with a variety of static methods. Some of these methods require that GoogleApiClient be connected, some will
 * queue up calls before GoogleApiClient is connected; check the specific API documentation to determine whether you need to be connected.
 * <p/>
 * Before any operation is executed, the GoogleApiClient must be connected. The simplest way to manage the connection is to use
 * enableAutoManage.
 * <p/>
 * GoogleApiClient instances are not thread-safe. To access Google APIs from multiple threads simultaneously, create a GoogleApiClient on
 * each thread. GoogleApiClient service connections are cached internally, so creating multiple instances is fast.
 *
 * @deprecated Use {@link GoogleApi} based APIs instead.
 */
@Deprecated
public abstract class GoogleApiClient {
    private static final Set<GoogleApiClient> CLIENTS = Collections.newSetFromMap(new WeakHashMap<>());

    @NonNull
    @Hide
    public static String DEFAULT_ACCOUNT = "<<default account>>";

    /**
     * If a required authenticated API fails to connect, the entire GoogleApiClient will fail to connect and a failed {@link ConnectionResult} will be
     * delivered to {@code OnConnectionFailedListener#onConnectionFailed}. Once a connection has successfully completed the only way to
     * disconnect the authenticated APIs is to call {@link #disconnect()} on this GoogleApiClient.
     * <p>
     * Using this mode is equivalent to calling connect on a GoogleApiClient that contains authenticated APIs.
     * <p>
     * It is an error to use this mode if no authenticated APIs have been added to this GoogleApiClient.
     */
    public static final int SIGN_IN_MODE_REQUIRED = 1;

    /**
     * If authenticated APIs are present they will attempt to connect, but failure of an authenticated API will not cause the GoogleApiClient
     * connection to fail. After {@code ConnectionCallbacks#onConnected} is received, the status of an authenticated API can be checked with
     * {@link #hasConnectedApi(Api)}.
     * <p>
     * A GoogleApiClient using this mode may be transitioned between authenticated and unauthenticated states by adding
     * {@code GOOGLE_SIGN_IN_API}. To get an Intent that will allow the user to sign-in, call {@code getSignInIntent}. To sign the user out, call
     * {@code signOut}.
     * <p>
     * It is an error to call connect with no arguments on a client in this mode.
     */
    public static final int SIGN_IN_MODE_OPTIONAL = 2;

    /**
     * Connects the client to Google Play services. Blocks until the connection either succeeds or fails.
     * <p>
     * Keep in mind this method will cause ANRs if called from the main thread.
     * <p>
     * If the client is already connected, this methods returns immediately. If the client is already connecting (for example due to a prior call to
     * connect), this method blocks until the existing connection attempt completes. If a prior connection attempt has already failed, then a new
     * connection attempt is started.
     *
     * @return the result of the connection
     */
    @NonNull
    public abstract ConnectionResult blockingConnect();

    /**
     * Connects the client to Google Play services. Blocks until the connection either succeeds or fails, or the timeout is reached.
     * <p>
     * Keep in mind this method will cause ANRs if called from the main thread.
     * <p>
     * If the client is already connected, this methods returns immediately. If the client is already connecting (for example due to a prior call to
     * connect), this method blocks until the existing connection attempt completes or the timeout is reached. If a prior connection attempt has
     * already failed, then a new connection attempt is started.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the {@code timeout} argument
     * @return the result of the connection
     */
    @NonNull
    public abstract ConnectionResult blockingConnect(long timeout, @NonNull TimeUnit unit);

    /**
     * Clears the account selected by the user and reconnects the client asking the user to pick an
     * account again if {@link Builder#useDefaultAccount()} was set.
     *
     * @return the pending result is fired once the default account has been cleared, but before
     * the client is reconnected - for that {@link ConnectionCallbacks} can be used.
     */
    @NonNull
    public abstract PendingResult<Status> clearDefaultAccountAndReconnect();

    /**
     * Connects the client to Google Play services. This method returns immediately, and connects
     * to the service in the background. If the connection is successful,
     * {@link ConnectionCallbacks#onConnected(Bundle)} is called and enqueued items are executed.
     * On a failure, {@link OnConnectionFailedListener#onConnectionFailed(ConnectionResult)} is
     * called.
     * <p>
     * If the client is already connected or connecting, this method does nothing.
     */
    public abstract void connect();

    /**
     * Connects the client to Google Play services using the given sign in mode.
     * <p>
     * It is an error to make multiple calls to this method passing different modes. Once a mode is selected, all future connect calls must use the
     * same mode.
     *
     * @see #connect()
     * @see #SIGN_IN_MODE_REQUIRED
     * @see #SIGN_IN_MODE_OPTIONAL
     */
    public abstract void connect(int signInMode);

    /**
     * Closes the connection to Google Play services. No calls can be made using this client after calling this method. Any method calls that
     * haven't executed yet will be canceled, and their {@link ResultCallback#onResult(Result)} callbacks won't be called.
     * <p>
     * If the connection to the remote service hasn't been established yet, all enqueued calls will be canceled.
     *
     * @see #connect()
     */
    public abstract void disconnect();

    /**
     * Prints the GoogleApiClient's state into the given stream.
     *
     * @param prefix Desired prefix to prepend at each line of output.
     * @param fd     The raw file descriptor that the dump is being sent to.
     * @param writer The PrintWriter to use for writing the dump.
     * @param args   Additional arguments to the dump request.
     */
    public abstract void dump(@NonNull String prefix, @Nullable FileDescriptor fd, @NonNull PrintWriter writer, @Nullable String[] args);

    /**
     * Prints the state of all GoogleApiClients in the current process into the given stream.
     * <p>
     * This can be used to diagnose lifecycle issues where GoogleApiClients may be unintentionally left in the connected state. Note that the
     * output may include clients that are no longer referenced but have not yet been garbage collected.
     *
     * @param prefix Desired prefix to prepend at each line of output.
     * @param fd     The raw file descriptor that the dump is being sent to.
     * @param writer The PrintWriter to use for writing the dump.
     * @param args   Additional arguments to the dump request.
     */
    public static void dumpAll(@NonNull String prefix, @NonNull FileDescriptor fd, @NonNull PrintWriter writer, @NonNull String[] args) {
        synchronized (CLIENTS) {
            int idx = 0;
            for (GoogleApiClient client : CLIENTS) {
                writer.append(prefix).append("GoogleApiClient#").println(idx++);
                client.dump(prefix + "  ", fd, writer, args);
            }
        }
    }

    /**
     * Returns the {@link ConnectionResult} for the GoogleApiClient's connection to the specified API. This method must only be called after connect
     * has been called and before {@link #disconnect()} is called.
     * <p>
     * This method should be used to check the connection result of an API added via addApiIfAvailable in the event that the overall connection
     * succeeded, but the individual API failed to connect. To check the failure of the overall connection, use {@link Builder#addOnConnectionFailedListener}.
     * <p>
     * This method may return stale results if the GoogleApiClient is reconnecting due to a lost network connection. It is guaranteed to return the
     * most recent ConnectionResult from attempting to connect the given API, but will throw an IllegalStateException if called before calling
     * connect, after calling disconnect, or after receiving {@link OnConnectionFailedListener#onConnectionFailed(ConnectionResult)}. This method can be used to
     * easily determine why an API failed to connect if it was not available. To determine whether a given API is currently connected (without
     * potential stale results) see {@link #hasConnectedApi(Api)}.
     *
     * @param api The {@link Api} to retrieve the ConnectionResult of. Passing an API that was not registered with the
     *            GoogleApiClient results in an IllegalArgumentException.
     */
    @NonNull
    public abstract ConnectionResult getConnectionResult(@NonNull Api<?> api);

    @NonNull
    @Hide
    public Context getContext() {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @Hide
    public Looper getLooper() {
        throw new UnsupportedOperationException();
    }

    @Hide
    public boolean hasApi(@NonNull Api<?> api) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns whether or not this GoogleApiClient has the specified API in a connected state.
     *
     * @param api The {@link Api} to test the connection of.
     */
    public abstract boolean hasConnectedApi(@NonNull Api<?> api);

    /**
     * Checks if the client is currently connected to the service, so that requests to other methods will succeed. Applications should guard client
     * actions caused by the user with a call to this method.
     *
     * @return {@code true} if the client is connected to the service.
     */
    public abstract boolean isConnected();

    /**
     * Checks if the client is attempting to connect to the service.
     *
     * @return {@code true} if the client is attempting to connect to the service.
     */
    public abstract boolean isConnecting();

    /**
     * Returns {@code true} if the specified listener is currently registered to receive connection
     * events.
     *
     * @param listener The listener to check for.
     * @return {@code true} if the specified listener is currently registered to receive connection
     * events.
     * @see #registerConnectionCallbacks(ConnectionCallbacks)
     * @see #unregisterConnectionCallbacks(ConnectionCallbacks)
     */
    public abstract boolean isConnectionCallbacksRegistered(@NonNull ConnectionCallbacks listener);

    /**
     * Returns {@code true} if the specified listener is currently registered to receive connection
     * failed events.
     *
     * @param listener The listener to check for.
     * @return {@code true} if the specified listener is currently registered to receive connection
     * failed events.
     * @see #registerConnectionFailedListener(OnConnectionFailedListener)
     * @see #unregisterConnectionFailedListener(OnConnectionFailedListener)
     */
    public abstract boolean isConnectionFailedListenerRegistered(@NonNull OnConnectionFailedListener listener);

    @Hide
    public boolean maybeSignIn(@NonNull SignInConnectionListener listener) {
        throw new UnsupportedOperationException();
    }

    @Hide
    public void maybeSignOut() {
        throw new UnsupportedOperationException();
    }

    /**
     * Closes the current connection to Google Play services and creates a new connection. Equivalent to calling {@link #disconnect()} followed by
     * connect.
     * <p/>
     * This method closes the current connection then returns immediately and reconnects to the
     * service in the background.
     * <p/>
     * After calling this method, your application will receive
     * {@link ConnectionCallbacks#onConnected(Bundle)} if the connection is successful, or
     * {@link OnConnectionFailedListener#onConnectionFailed(ConnectionResult)} if the connection
     * failed.
     *
     * @see #connect()
     * @see #disconnect()
     */
    public abstract void reconnect();

    /**
     * Registers a listener to receive connection events from this {@link GoogleApiClient}. If the
     * service is already connected, the listener's {@link ConnectionCallbacks#onConnected(Bundle)}
     * method will be called immediately. Applications should balance calls to this method with
     * calls to {@link #unregisterConnectionCallbacks(ConnectionCallbacks)} to avoid leaking
     * resources.
     * <p/>
     * If the specified listener is already registered to receive connection events, this method
     * will not add a duplicate entry for the same listener, but will still call the listener's
     * {@link ConnectionCallbacks#onConnected(Bundle)} method if currently connected.
     * <p/>
     * Note that the order of messages received here may not be stable, so clients should not rely
     * on the order that multiple listeners receive events in.
     *
     * @param listener the listener where the results of the asynchronous {@link #connect()} call
     *                 are delivered.
     */
    public abstract void registerConnectionCallbacks(@NonNull ConnectionCallbacks listener);

    /**
     * Registers a listener to receive connection failed events from this {@link GoogleApiClient}.
     * Unlike {@link #registerConnectionCallbacks(ConnectionCallbacks)}, if the service is not
     * already connected, the listener's
     * {@link OnConnectionFailedListener#onConnectionFailed(ConnectionResult)} method will not be
     * called immediately. Applications should balance calls to this method with calls to
     * {@link #unregisterConnectionFailedListener(OnConnectionFailedListener)} to avoid leaking
     * resources.
     * <p/>
     * If the specified listener is already registered to receive connection failed events, this
     * method will not add a duplicate entry for the same listener.
     * <p/>
     * Note that the order of messages received here may not be stable, so clients should not rely
     * on the order that multiple listeners receive events in.
     *
     * @param listener the listener where the results of the asynchronous {@link #connect()} call
     *                 are delivered.
     */
    public abstract void registerConnectionFailedListener(@NonNull OnConnectionFailedListener listener);

    @NonNull
    @Hide
    public <L> ListenerHolder<L> registerListener(@NonNull L listener) {
        throw new UnsupportedOperationException();
    }

    /**
     * Disconnects the client and stops automatic lifecycle management. Use this before creating a
     * new client (which might be necessary when switching accounts, changing the set of used APIs
     * etc.).
     * <p/>
     * This method must be called from the main thread.
     *
     * @param lifecycleActivity the activity managing the client's lifecycle.
     * @throws IllegalStateException if called from outside of the main thread.
     * @see Builder#enableAutoManage(FragmentActivity, int, OnConnectionFailedListener)
     */
    public abstract void stopAutoManage(@NonNull FragmentActivity lifecycleActivity) throws IllegalStateException;

    /**
     * Removes a connection listener from this {@link GoogleApiClient}. Note that removing a
     * listener does not generate any callbacks.
     * <p/>
     * If the specified listener is not currently registered to receive connection events, this
     * method will have no effect.
     *
     * @param listener the listener to unregister.
     */
    public abstract void unregisterConnectionCallbacks(@NonNull ConnectionCallbacks listener);

    /**
     * Removes a connection failed listener from the {@link GoogleApiClient}. Note that removing a
     * listener does not generate any callbacks.
     * <p/>
     * If the specified listener is not currently registered to receive connection failed events,
     * this method will have no effect.
     *
     * @param listener the listener to unregister.
     */
    public abstract void unregisterConnectionFailedListener(@NonNull OnConnectionFailedListener listener);

    /**
     * Builder to configure a {@link GoogleApiClient}.
     *
     * @deprecated Use {@link GoogleApi} based APIs instead.
     */
    @Deprecated
    public static class Builder {
        private final Context context;
        private final Map<Api<?>, Api.ApiOptions> apis = new HashMap<>();
        private final Set<ConnectionCallbacks> connectionCallbacks = new HashSet<>();
        private final Set<OnConnectionFailedListener> connectionFailedListeners = new HashSet<>();
        private final Set<Scope> scopes = new HashSet<>();
        private final Map<Api<?>, Set<Scope>> scopesForOptionalApi = new HashMap<>();
        private Account account;
        private int clientId = -1;
        private FragmentActivity fragmentActivity;
        private Looper looper;
        private int gravityForPopups;
        private OnConnectionFailedListener unresolvedConnectionFailedListener;
        private View viewForPopups;

        /**
         * Builder to help construct the {@link GoogleApiClient} object.
         *
         * @param context The context to use for the connection.
         */
        public Builder(@NonNull Context context) {
            this.context = context;
            this.looper = context.getMainLooper();
        }

        /**
         * Builder to help construct the {@link GoogleApiClient} object.
         *
         * @param context                  The context to use for the connection.
         * @param connectedListener        The listener where the results of the asynchronous
         *                                 {@link #connect()} call are delivered.
         * @param connectionFailedListener The listener which will be notified if the connection
         *                                 attempt fails.
         */
        public Builder(@NonNull Context context, @NonNull ConnectionCallbacks connectedListener, @NonNull OnConnectionFailedListener connectionFailedListener) {
            this(context);
            addConnectionCallbacks(connectedListener);
            addOnConnectionFailedListener(connectionFailedListener);
        }

        /**
         * Specify which Apis are requested by your app. See {@link Api} for more information.
         *
         * @param api The Api requested by your app.
         * @see Api
         */
        @NonNull
        public Builder addApi(@NonNull Api<? extends Api.ApiOptions.NotRequiredOptions> api) {
            apis.put(api, null);
            scopes.addAll(api.getClientBuilder().getImpliedScopes(null));
            return this;
        }

        /**
         * Specify which Apis are requested by your app. See {@link Api} for more information.
         *
         * @param api     The Api requested by your app.
         * @param options Any additional parameters required for the specific AP
         * @see Api
         */
        @NonNull
        public <O extends Api.ApiOptions.HasOptions> Builder addApi(@NonNull Api<O> api, @NonNull O options) {
            apis.put(api, options);
            scopes.addAll(api.getClientBuilder().getImpliedScopes(options));
            return this;
        }

        /**
         * Specify which Apis should attempt to connect, but are not strictly required for your app. The GoogleApiClient will try to connect to these
         * Apis, but will not necessarily fail if there are only errors when connecting to an unavailable Api added with this method. See {@link Api} for more
         * information.
         *
         * @param api    The Api requested by your app.
         * @param scopes Scopes required by this API.
         * @see Api
         */
        @NonNull
        public Builder addApiIfAvailable(@NonNull Api<? extends Api.ApiOptions.NotRequiredOptions> api, @NonNull Scope... scopes) {
            apis.put(api, null);
            Set<Scope> scopeSet = new HashSet<>(api.getClientBuilder().getImpliedScopes(null));
            Collections.addAll(scopeSet, scopes);
            scopesForOptionalApi.put(api, scopeSet);
            return this;
        }

        /**
         * Specify which Apis should attempt to connect, but are not strictly required for your app. The GoogleApiClient will try to connect to these
         * Apis, but will not necessarily fail if there are only errors when connecting to an unavailable Api added with this method. See {@link Api} for more
         * information.
         *
         * @param api    The Api requested by your app.
         * @param scopes Scopes required by this API.
         * @see Api
         */
        @NonNull
        public <O extends Api.ApiOptions.HasOptions> GoogleApiClient.Builder addApiIfAvailable(@NonNull Api<O> api, @NonNull O options, @NonNull Scope... scopes) {
            apis.put(api, options);
            Set<Scope> scopeSet = new HashSet<>(api.getClientBuilder().getImpliedScopes(options));
            Collections.addAll(scopeSet, scopes);
            scopesForOptionalApi.put(api, scopeSet);
            return this;
        }

        /**
         * Registers a listener to receive connection events from this {@code GoogleApiClient}. Applications should balance calls to this method with calls
         * to {@link #unregisterConnectionCallbacks(ConnectionCallbacks)} to avoid leaking resources.
         * <p>
         * If the specified listener is already registered to receive connection events, this method will not add a duplicate entry for the same listener.
         * <p>
         * Note that the order of messages received here may not be stable, so clients should not rely on the order that multiple listeners receive
         * events in.
         *
         * @param listener the listener where the results of the asynchronous connect call are delivered.
         */
        @NonNull
        public Builder addConnectionCallbacks(@NonNull ConnectionCallbacks listener) {
            connectionCallbacks.add(listener);
            return this;
        }

        /**
         * Adds a listener to register to receive connection failed events from this {@code GoogleApiClient}. Applications should balance calls to this
         * method with calls to {@link #unregisterConnectionFailedListener(OnConnectionFailedListener)} to avoid leaking resources.
         * <p>
         * If the specified listener is already registered to receive connection failed events, this method will not add a duplicate entry for the same
         * listener.
         * <p>
         * Note that the order of messages received here may not be stable, so clients should not rely on the order that multiple listeners receive
         * events in.
         *
         * @param listener the listener where the results of the asynchronous connect call are delivered.
         */
        @NonNull
        public Builder addOnConnectionFailedListener(@NonNull OnConnectionFailedListener listener) {
            connectionFailedListeners.add(listener);
            return this;
        }

        /**
         * Specify the OAuth 2.0 scopes requested by your app. See {@link Scopes} for more information.
         * <p>
         * It is an error to call this method when using {@code GOOGLE_SIGN_IN_API}. Use {@code requestScopes} instead.
         *
         * @param scope The OAuth 2.0 scopes requested by your app.
         * @see Scopes
         */
        @NonNull
        public Builder addScope(Scope scope) {
            scopes.add(scope);
            return this;
        }

        /**
         * Builds a new {@link GoogleApiClient} object for communicating with the Google APIs.
         *
         * @return The {@link GoogleApiClient} object.
         */
        @NonNull
        public GoogleApiClient build() {
            return new GoogleApiClientImpl(context, looper, getClientSettings(), apis, connectionCallbacks, connectionFailedListeners, clientId);
        }

        /**
         * Enables automatic lifecycle management in a support library {@link FragmentActivity} that connects the client in {@link FragmentActivity#onStart()} and disconnects it in
         * {@link FragmentActivity#onStop()}.
         * <p>
         * It handles user recoverable errors appropriately and calls {@link OnConnectionFailedListener#onConnectionFailed(ConnectionResult)} on the
         * {@code unresolvedConnectionFailedListener} if the {@link ConnectionResult} has no resolution. This eliminates most of the boiler plate associated with
         * using {@link GoogleApiClient}.
         * <p>
         * This method can only be used if this GoogleApiClient will be the only auto-managed client in the containing activity. The api client will be
         * assigned a default client id.
         * <p>
         * When using this option, {@link #build()} must be called from the main thread.
         *
         * @param fragmentActivity                   The activity that uses the {@link GoogleApiClient}. For lifecycle management to work correctly
         *                                           the activity must call its parent's {@link Activity#onActivityResult(int, int, Intent)}.
         * @param unresolvedConnectionFailedListener Called if the connection failed and there was no resolution or the user chose not to complete
         *                                           the provided resolution. If this listener is called, the client will no longer be auto-managed,
         *                                           and a new instance must be built. In the event that the user chooses not to complete a
         *                                           resolution, the will have a status code of {@link ConnectionResult#CANCELED}.
         */
        @NonNull
        public Builder enableAutoManage(@NonNull FragmentActivity fragmentActivity, @Nullable OnConnectionFailedListener unresolvedConnectionFailedListener) {
            enableAutoManage(fragmentActivity, 0, unresolvedConnectionFailedListener);
            return this;
        }

        /**
         * Enables automatic lifecycle management in a support library {@link FragmentActivity} that connects the client in {@link FragmentActivity#onStart()} and disconnects it in
         * {@link FragmentActivity#onStop()}.
         * <p>
         * It handles user recoverable errors appropriately and calls {@code unresolvedConnectionFailedListener} if the {@link ConnectionResult} has no
         * resolution. This eliminates most of the boiler plate associated with using {@link GoogleApiClient}.
         * <p>
         * When using this option, {@link #build()} must be called from the main thread.
         *
         * @param fragmentActivity                   The activity that uses the {@link GoogleApiClient}. For lifecycle management to work correctly
         *                                           the activity must call its parent's {@link Activity#onActivityResult(int, int, Intent)}.
         * @param clientId                           A non-negative identifier for this client. At any given time, only one auto-managed client is
         *                                           allowed per id. To reuse an id you must first call {@link #stopAutoManage(FragmentActivity)} on the previous client.
         * @param unresolvedConnectionFailedListener Called if the connection failed and there was no resolution or the user chose not to complete
         *                                           the provided resolution. If this listener is called, the client will no longer be auto-managed,
         *                                           and a new instance must be built. In the event that the user chooses not to complete a
         *                                           resolution, the will have a status code of {@link ConnectionResult#CANCELED}.
         * @throws NullPointerException     if fragmentActivity is null
         * @throws IllegalArgumentException if clientId is negative.
         * @throws IllegalStateException    if clientId is already being auto-managed.
         */
        @NonNull
        public Builder enableAutoManage(@NonNull FragmentActivity fragmentActivity, int clientId, @Nullable OnConnectionFailedListener unresolvedConnectionFailedListener) throws NullPointerException, IllegalArgumentException, IllegalStateException {
            this.fragmentActivity = fragmentActivity;
            this.clientId = clientId;
            this.unresolvedConnectionFailedListener = unresolvedConnectionFailedListener;
            return this;
        }

        @Hide
        public ClientSettings getClientSettings() {
            return new ClientSettings(account, scopes, scopesForOptionalApi, gravityForPopups, viewForPopups, context.getPackageName(), context.getClass().getName(), SignInOptions.DEFAULT);
        }

        /**
         * Specify an account name on the device that should be used. If this is never called, the client will use the current default account for Google
         * Play services for this application.
         * <p>
         * It is an error to call this method when using {@code GOOGLE_SIGN_IN_API}. Use {@code #setAccountName(String)} instead.
         *
         * @param accountName The account name on the device that should be used by {@link GoogleApiClient}.
         */
        @NonNull
        public Builder setAccountName(@NonNull String accountName) {
            this.account = new Account(accountName, AuthConstants.DEFAULT_ACCOUNT_TYPE);
            return this;
        }

        /**
         * Specifies the part of the screen at which games service popups (for example, "welcome back" or "achievement unlocked" popups) will be
         * displayed using gravity.
         * <p>
         * Default value is {@link Gravity#TOP}|{@link Gravity#CENTER_HORIZONTAL}.
         *
         * @param gravityForPopups The gravity which controls the placement of games service popups.
         */
        @NonNull
        public Builder setGravityForPopups(int gravityForPopups) {
            this.gravityForPopups = gravityForPopups;
            return this;
        }

        /**
         * Sets a {@link Handler} to indicate which thread to use when invoking callbacks. Will not be used directly to handle callbacks. If this is not called
         * then the application's main thread will be used.
         */
        @NonNull
        public Builder setHandler(@NonNull Handler handler) {
            this.looper = handler.getLooper();
            return this;
        }

        /**
         * Sets the {@link View} to use as a content view for popups.
         *
         * @param viewForPopups The view to use as a content view for popups. View cannot be null.
         */
        @NonNull
        public Builder setViewForPopups(@NonNull View viewForPopups) {
            this.viewForPopups = viewForPopups;
            return this;
        }

        /**
         * Specify that the default account should be used when connecting to services.
         */
        public Builder useDefaultAccount() {
            setAccountName(DEFAULT_ACCOUNT);
            return this;
        }
    }

    /**
     * Provides callbacks that are called when the client is connected or disconnected from the
     * service. Most applications implement {@link #onConnected(Bundle)} to start making requests.
     */
    @PublicApi
    @Deprecated
    public interface ConnectionCallbacks extends com.google.android.gms.common.api.internal.ConnectionCallbacks {
        /**
         * A suspension cause informing that the service has been killed.
         */
        int CAUSE_SERVICE_DISCONNECTED = 1;
        /**
         * A suspension cause informing you that a peer device connection was lost.
         */
        int CAUSE_NETWORK_LOST = 2;

        void onConnected(Bundle connectionHint);
    }

    /**
     * Provides callbacks for scenarios that result in a failed attempt to connect the client to
     * the service. See {@link ConnectionResult} for a list of error codes and suggestions for
     * resolution.
     */
    @PublicApi
    @Deprecated
    public interface OnConnectionFailedListener extends com.google.android.gms.common.api.internal.OnConnectionFailedListener {
    }
}
