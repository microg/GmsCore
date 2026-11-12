/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastStatusCodes;
import com.google.android.gms.common.api.CommonStatusCodes;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.ChromeCastConnectionEvent;
import su.litvak.chromecast.api.v2.ChromeCastConnectionEventListener;

/**
 * The connection to a Cast device: makes sure we only ever talk to a genuine Cast device and gets the connection back
 * if it is lost.
 * <p>
 * The Cast library accepts whatever answers at the address of the device. So the device first has to prove that it is
 * a Cast device on a connection of our own ({@link CastDeviceAuthenticator}). The connection of the library is then
 * only used if it ends up at the very device that passed, which shows in the TLS certificate. The library is kept from
 * connecting on its own, all connections are made here.
 */
final class CastDeviceConnection implements ChromeCastConnectionEventListener {
    private static final String TAG = "GmsCastDeviceConnection";

    private static final int DEFAULT_PORT = 8009;
    /**
     * Global setting that turns off device authentication, to test against something that is not a certified Cast
     * device. Apps can't change global settings, it takes adb:
     * {@code adb shell settings put global microg_cast_skip_device_authentication 1}
     */
    static final String SETTING_SKIP_DEVICE_AUTHENTICATION = "microg_cast_skip_device_authentication";

    // Delay before each attempt to reconnect to a device we lost the connection to. Gives up after about half a minute.
    private static final long[] RECONNECT_DELAYS_MILLIS = {500, 2000, 5000, 10000, 15000};
    private static final ScheduledExecutorService RECONNECT_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    interface Callback {
        /**
         * The connection got lost and we are trying to get it back.
         */
        void onConnectionSuspended();

        /**
         * The connection is back after it was suspended.
         */
        void onConnectionRestored();

        /**
         * We gave up on getting the connection back.
         *
         * @param statusCode why, as to be reported to the client
         */
        void onConnectionLost(int statusCode);
    }

    private enum State {
        DISCONNECTED,
        /**
         * {@link #connect()} is at work
         */
        CONNECTING,
        CONNECTED,
        /**
         * Lost the connection without being asked to close it, an attempt to reconnect is scheduled or under way
         */
        SUSPENDED
    }

    private final Context context;
    private final String host;
    private final int port;
    private final ChromeCast chromecast;
    private final Callback callback;
    private CastDeviceAuthenticator authenticator;

    // Held while a connection is being made, so that there is only ever one attempt
    private final Object connectLock = new Object();
    // Changed through compareAndSetState() and close() only
    private volatile State state = State.DISCONNECTED;

    /**
     * @param authenticator what decides whether the device is genuine, or null to accept certified Cast devices
     */
    CastDeviceConnection(Context context, CastDevice device, CastDeviceAuthenticator authenticator, Callback callback) {
        this.context = context;
        this.host = device.getAddress();
        this.port = device.getServicePort() > 0 ? device.getServicePort() : DEFAULT_PORT;
        this.authenticator = authenticator;
        this.callback = callback;
        this.chromecast = new ChromeCast(this.host, this.port);
        this.chromecast.setAutoReconnect(false);
        this.chromecast.registerConnectionListener(this);
    }

    /**
     * For requests to the device. They fail unless {@link #isConnected()}.
     */
    ChromeCast getChromeCast() {
        return this.chromecast;
    }

    boolean isConnected() {
        return this.state == State.CONNECTED && this.chromecast.isConnected();
    }

    void requireConnected() throws IOException {
        if (!isConnected()) throw new IOException("Not connected to device");
    }

    private synchronized boolean compareAndSetState(State expected, State update) {
        if (this.state != expected) return false;
        this.state = update;
        return true;
    }

    /**
     * Connects to the device unless we are connected already. Blocks until done.
     *
     * @throws GeneralSecurityException if what answers is not a genuine Cast device
     * @throws IOException              if the device can't be reached
     */
    void connect() throws IOException, GeneralSecurityException {
        synchronized (this.connectLock) {
            if (isConnected()) return;
            synchronized (this) {
                // Takes over from an attempt to reconnect that might be scheduled
                this.state = State.CONNECTING;
            }
            boolean opened = false;
            try {
                open();
                opened = true;
            } finally {
                if (!opened) compareAndSetState(State.CONNECTING, State.DISCONNECTED);
            }
            if (!compareAndSetState(State.CONNECTING, State.CONNECTED)) {
                disconnectQuietly();
                throw new IOException("Connection was closed while connecting");
            }
        }
    }

    /**
     * Closes the connection. Does not wait for an attempt to connect that might be under way, which then fails.
     */
    void close() {
        synchronized (this) {
            this.state = State.DISCONNECTED;
        }
        disconnectQuietly();
    }

    private void disconnectQuietly() {
        try {
            this.chromecast.disconnect();
        } catch (IOException e) {
            Log.w(TAG, "Error disconnecting from device: " + e.getMessage());
        }
    }

    private boolean isDeviceAuthenticationSkipped() {
        try {
            return Settings.Global.getInt(this.context.getContentResolver(), SETTING_SKIP_DEVICE_AUTHENTICATION, 0) == 1;
        } catch (Exception e) {
            return false;
        }
    }

    private void open() throws IOException, GeneralSecurityException {
        byte[] certificate = null;
        if (isDeviceAuthenticationSkipped()) {
            Log.w(TAG, "Device authentication is turned off by " + SETTING_SKIP_DEVICE_AUTHENTICATION + ", not checking that this is a Cast device");
        } else {
            if (this.authenticator == null) this.authenticator = CastDeviceAuthenticator.forCastDevices();
            certificate = this.authenticator.authenticate(this.host, this.port);
        }
        this.chromecast.connect();
        try {
            ChromeCastAccess.limitMessageSize(this.chromecast, CastDeviceAuthenticator.MAX_MESSAGE_SIZE);
            if (certificate != null && !Arrays.equals(certificate, ChromeCastAccess.getPeerCertificate(this.chromecast))) {
                throw new GeneralSecurityException("Device presented another certificate than the one it authenticated with");
            }
        } catch (GeneralSecurityException e) {
            // Never use a connection that we could not vouch for
            disconnectQuietly();
            throw e;
        }
    }

    @Override
    public void connectionEventReceived(ChromeCastConnectionEvent event) {
        // Anything but the loss of an established connection is the doing of connect(), reconnect() or close()
        if (event.isConnected() || !compareAndSetState(State.CONNECTED, State.SUSPENDED)) return;
        // Connections get lost for a moment, for example when the device changes its access point
        Log.d(TAG, "Connection to device lost, trying to reconnect");
        this.callback.onConnectionSuspended();
        scheduleReconnect(0);
    }

    private void scheduleReconnect(int attempt) {
        RECONNECT_EXECUTOR.schedule(() -> reconnect(attempt), RECONNECT_DELAYS_MILLIS[attempt], TimeUnit.MILLISECONDS);
    }

    private void reconnect(int attempt) {
        int failure;
        synchronized (this.connectLock) {
            // Closed or connected by the client in the meantime
            if (this.state != State.SUSPENDED) return;
            try {
                open();
                if (!compareAndSetState(State.SUSPENDED, State.CONNECTED)) {
                    disconnectQuietly();
                    return;
                }
                failure = CommonStatusCodes.SUCCESS;
            } catch (GeneralSecurityException e) {
                // Whatever answers at the address of the device now is not the device, no point in trying again
                Log.w(TAG, "Not reconnecting, device failed to prove that it is a Cast device", e);
                failure = CastStatusCodes.AUTHENTICATION_FAILED;
            } catch (IOException | RuntimeException e) {
                Log.d(TAG, "Reconnecting failed (attempt " + (attempt + 1) + "): " + e.getMessage());
                if (attempt + 1 < RECONNECT_DELAYS_MILLIS.length) {
                    scheduleReconnect(attempt + 1);
                    return;
                }
                Log.w(TAG, "Giving up on reconnecting to device");
                failure = CommonStatusCodes.NETWORK_ERROR;
            }
            if (failure != CommonStatusCodes.SUCCESS && !compareAndSetState(State.SUSPENDED, State.DISCONNECTED)) return;
        }
        if (failure == CommonStatusCodes.SUCCESS) {
            Log.d(TAG, "Reconnected to device");
            this.callback.onConnectionRestored();
        } else {
            this.callback.onConnectionLost(failure);
        }
    }
}
