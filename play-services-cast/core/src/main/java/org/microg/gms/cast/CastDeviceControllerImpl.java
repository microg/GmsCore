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

package org.microg.gms.cast;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.ApplicationStatus;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastDeviceStatus;
import com.google.android.gms.cast.CastStatusCodes;
import com.google.android.gms.cast.JoinOptions;
import com.google.android.gms.cast.LaunchOptions;
import com.google.android.gms.cast.internal.ICastDeviceController;
import com.google.android.gms.cast.internal.ICastDeviceControllerListener;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.internal.BinderWrapper;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import su.litvak.chromecast.api.v2.Application;
import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.ChromeCastException;
import su.litvak.chromecast.api.v2.ChromeCastRawMessage;
import su.litvak.chromecast.api.v2.ChromeCastRawMessageListener;
import su.litvak.chromecast.api.v2.ChromeCastSpontaneousEvent;
import su.litvak.chromecast.api.v2.ChromeCastSpontaneousEventListener;
import su.litvak.chromecast.api.v2.Namespace;

/**
 * What a single client sees of a single Cast device: translates between the binder interface of the client library
 * and the Cast library. The connection itself is the business of {@link CastDeviceConnection}.
 * <p>
 * There are two generations of clients:
 * <ul>
 * <li>The GoogleApiClient based one passes its listener in the extras of the service request and considers itself
 * connected to the device as soon as it is bound to this service.</li>
 * <li>The GoogleApi based one (play-services-cast 19+, used by all recent versions of the Cast framework) registers
 * its listener using {@link #registerListener}, explicitly asks to {@link #connect()} and waits for
 * {@link ICastDeviceControllerListener#onConnected(int)}.</li>
 * </ul>
 * Calls of the client are oneway, so they arrive one at a time on binder threads and may block on the network. Events
 * of the device arrive on the reader thread of the Cast library, the outcome of reconnecting on a thread of
 * {@link CastDeviceConnection} and volume requests of the MediaRouter on an executor. What they share is volatile.
 */
public class CastDeviceControllerImpl extends ICastDeviceController.Stub implements
        CastDeviceConnection.Callback,
        ChromeCastSpontaneousEventListener,
        ChromeCastRawMessageListener {
    private static final String TAG = "GmsCastDeviceController";

    private static final String NAMESPACE_PREFIX_TRANSPORT = "urn:x-cast:com.google.cast.tp.";
    private static final String NAMESPACE_RECEIVER = "urn:x-cast:com.google.cast.receiver";
    // Same value as GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST
    private static final int CAUSE_NETWORK_LOST = 2;

    // Volume requests of the MediaRouter arrive on the main thread and have to reach the device from elsewhere
    private static final ExecutorService ROUTE_EXECUTOR = Executors.newSingleThreadExecutor();

    private final CastDeviceRegistry registry = CastDeviceRegistry.getInstance();
    private final String packageName;
    private final CastDevice castDevice;
    private final CastDeviceConnection connection;
    private final ChromeCast chromecast;

    private volatile ICastDeviceControllerListener listener;
    private IBinder.DeathRecipient listenerDeathRecipient;
    // Session of the receiver application this client launched or joined
    private volatile String sessionId = null;
    // Volume the MediaRouter asked for that the device has not confirmed yet, the route shows it meanwhile
    private final AtomicReference<Double> unconfirmedRouteVolume = new AtomicReference<>();

    public CastDeviceControllerImpl(Context context, String packageName, Bundle extras) {
        this(context, packageName, extras, null);
    }

    /**
     * @param authenticator what decides whether the device is genuine, or null to accept certified Cast devices
     * @throws IllegalArgumentException if the request is not for a device that was found by discovery
     */
    CastDeviceControllerImpl(Context context, String packageName, Bundle extras, CastDeviceAuthenticator authenticator) {
        this.packageName = packageName;

        if (extras == null) extras = new Bundle();
        extras.setClassLoader(BinderWrapper.class.getClassLoader());
        this.castDevice = CastDevice.getFromBundle(extras);
        if (this.castDevice == null) {
            throw new IllegalArgumentException("Missing cast device in service request of " + packageName);
        }
        if (!this.registry.isDiscovered(this.castDevice)) {
            // The device is whatever the client says it is, so only accept what we found ourselves
            throw new IllegalArgumentException("Cast device in service request of " + packageName + " was not found by discovery: " + this.castDevice);
        }
        Log.d(TAG, "Controller created for " + packageName + ": " + this.castDevice);

        this.connection = new CastDeviceConnection(context, this.castDevice, authenticator, this);
        this.chromecast = this.connection.getChromeCast();
        this.chromecast.registerListener(this);
        this.chromecast.registerRawMessageListener(this);

        BinderWrapper listenerWrapper = (BinderWrapper) extras.get("listener");
        if (listenerWrapper != null) {
            // Legacy client. It is implicitly connected, see ensureConnected()
            setListener(ICastDeviceControllerListener.Stub.asInterface(listenerWrapper.binder));
        }
    }

    // region Connection

    /**
     * @return status code to report to the client
     */
    private int connectToDevice() {
        try {
            this.connection.connect();
        } catch (GeneralSecurityException e) {
            Log.w(TAG, "Not connecting, device failed to prove that it is a Cast device", e);
            return CastStatusCodes.AUTHENTICATION_FAILED;
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "Error connecting to cast device", e);
            return CommonStatusCodes.NETWORK_ERROR;
        }
        this.registry.setConnected(this.castDevice.getDeviceId(), this);
        return CommonStatusCodes.SUCCESS;
    }

    /**
     * Legacy clients never ask us to connect, their first request implies it.
     */
    private boolean ensureConnected() {
        if (this.connection.isConnected()) return true;
        int statusCode = connectToDevice();
        if (statusCode != CommonStatusCodes.SUCCESS) this.onApplicationConnectionFailure(statusCode);
        return statusCode == CommonStatusCodes.SUCCESS;
    }

    private void closeDeviceConnection() {
        this.sessionId = null;
        this.registry.setDisconnected(this.castDevice.getDeviceId(), this);
        this.connection.close();
    }

    @Override
    public void onConnectionSuspended() {
        this.onConnectionSuspended(CAUSE_NETWORK_LOST);
    }

    @Override
    public void onConnectionRestored() {
        // Makes the client ask to join the application again, see CastSessionImpl.onConnected()
        this.onConnected(CommonStatusCodes.SUCCESS);
    }

    @Override
    public void onConnectionLost(int statusCode) {
        this.sessionId = null;
        this.registry.setDisconnected(this.castDevice.getDeviceId(), this);
        this.onDisconnected(statusCode);
    }

    /**
     * Volume request that came in through the MediaRouter, for example from the volume keys.
     */
    void setVolumeFromRoute(double volume) {
        if (!this.connection.isConnected()) {
            Log.d(TAG, "Not connected to device, ignoring volume change");
            return;
        }
        // Requests are relative to the volume of the route, so they may leave the range
        Double requested = Math.max(0, Math.min(1, volume));
        // The route shows the volume right away and not only once the device confirmed it, which takes two round
        // trips. The next key press builds on the volume of the route, so presses that arrive in the meantime would
        // otherwise all start from the same volume.
        this.unconfirmedRouteVolume.set(requested);
        this.registry.setVolume(this.castDevice.getDeviceId(), this, requested);
        ROUTE_EXECUTOR.execute(this::sendRouteVolume);
    }

    /**
     * Sends the latest volume the MediaRouter asked for. Requests that came in while the device was busy with an
     * earlier one are skipped, so holding a volume key does not queue up.
     */
    private void sendRouteVolume() {
        Double requested = this.unconfirmedRouteVolume.get();
        // Sent by an earlier run already
        if (requested == null) return;
        try {
            this.connection.requireConnected();
            this.chromecast.setVolume(requested.floatValue());
            // See setVolume() for why the device has to be asked
            su.litvak.chromecast.api.v2.Status status = this.chromecast.getStatus();
            // The route shows what the device says again, unless there is a newer request
            this.unconfirmedRouteVolume.compareAndSet(requested, null);
            dispatchStatus(status);
        } catch (IOException e) {
            Log.w(TAG, "Error setting volume: " + e.getMessage());
            // The route keeps the requested volume until the device reports its status again
            this.unconfirmedRouteVolume.compareAndSet(requested, null);
        }
    }

    // endregion

    // region Events from the device

    private ApplicationMetadata createMetadataFromApplication(Application app) {
        if (app == null) {
            return null;
        }
        ApplicationMetadata metadata = new ApplicationMetadata();
        metadata.applicationId = app.id;
        metadata.name = app.name;
        metadata.images = new ArrayList<>();
        metadata.namespaces = new ArrayList<>();
        if (app.namespaces != null) {
            for (Namespace namespace : app.namespaces) {
                metadata.namespaces.add(namespace.name);
            }
        }
        metadata.senderAppIdentifier = this.packageName;
        return metadata;
    }

    private void dispatchStatus(su.litvak.chromecast.api.v2.Status status) {
        if (status == null) return;
        Application app = status.getRunningApp();
        ApplicationMetadata metadata = this.createMetadataFromApplication(app);
        if (app != null) {
            this.onApplicationStatusChanged(new ApplicationStatus(app.statusText));
        }
        double volume = status.volume == null || status.volume.level == null ? 0 : status.volume.level;
        boolean muted = status.volume != null && status.volume.muted;
        int activeInputState = status.activeInput ? 1 : 0;
        int standbyState = status.standBy ? 1 : 0;
        this.onDeviceStatusChanged(new CastDeviceStatus(volume, muted, activeInputState, metadata, standbyState));
        // While a volume request of the MediaRouter is under way, the device still reports the volume from before
        if (status.volume != null && status.volume.level != null && this.unconfirmedRouteVolume.get() == null) {
            this.registry.setVolume(this.castDevice.getDeviceId(), this, volume);
        }

        String sessionId = this.sessionId;
        if (sessionId != null && (app == null || !sessionId.equals(app.sessionId))) {
            // The application we were connected to is gone, e.g. because someone else started casting
            Log.d(TAG, "Application session " + sessionId + " ended on device");
            applicationEnded();
        }
    }

    private void applicationEnded() {
        if (this.sessionId == null) return;
        this.sessionId = null;
        this.onApplicationDisconnected(CommonStatusCodes.SUCCESS);
    }

    @Override
    public void spontaneousEventReceived(ChromeCastSpontaneousEvent event) {
        switch (event.getType()) {
            case STATUS:
                dispatchStatus((su.litvak.chromecast.api.v2.Status) event.getData());
                break;
            case CLOSE:
                applicationEnded();
                break;
            case MEDIA_STATUS:
            case APPEVENT:
            default:
                // Delivered to the client as raw message
                break;
        }
    }

    @Override
    public void rawMessageReceived(ChromeCastRawMessage message, Long requestId) {
        String namespace = message.getNamespace();
        if (namespace == null || namespace.startsWith(NAMESPACE_PREFIX_TRANSPORT) || namespace.equals(NAMESPACE_RECEIVER)) {
            // Handled by us and of no use to the client
            return;
        }
        switch (message.getPayloadType()) {
            case STRING:
                this.onTextMessageReceived(namespace, message.getPayloadUtf8());
                break;
            case BINARY:
                this.onBinaryMessageReceived(namespace, message.getPayloadBinary());
                break;
        }
    }

    // endregion

    // region Calls from the client

    @Override
    public void registerListener(ICastDeviceControllerListener listener) {
        Log.d(TAG, "registerListener");
        setListener(listener);
    }

    private synchronized void setListener(ICastDeviceControllerListener listener) {
        ICastDeviceControllerListener previous = this.listener;
        if (previous != null && this.listenerDeathRecipient != null) {
            try {
                previous.asBinder().unlinkToDeath(this.listenerDeathRecipient, 0);
            } catch (Exception ignored) {
                // Already dead
            }
        }
        this.listener = listener;
        this.listenerDeathRecipient = null;
        if (listener == null) return;

        IBinder binder = listener.asBinder();
        IBinder.DeathRecipient recipient = () -> onListenerDied(binder);
        try {
            binder.linkToDeath(recipient, 0);
            this.listenerDeathRecipient = recipient;
        } catch (RemoteException e) {
            onListenerDied(binder);
        }
    }

    /**
     * The client process is gone without telling us. Nobody is left to use the connection to the device, so close it.
     * The receiver application keeps running, which is also what happens if the app is killed with Play Services.
     */
    private void onListenerDied(IBinder binder) {
        synchronized (this) {
            ICastDeviceControllerListener current = this.listener;
            // Might be about a listener that was replaced in the meantime
            if (current == null || current.asBinder() != binder) return;
            this.listener = null;
            this.listenerDeathRecipient = null;
        }
        Log.d(TAG, "Client " + this.packageName + " died, closing connection to device");
        closeDeviceConnection();
    }

    @Override
    public void unregisterListener() {
        Log.d(TAG, "unregisterListener");
        setListener(null);
        // The client is done with the device, don't keep the socket open
        closeDeviceConnection();
    }

    @Override
    public void connect() {
        Log.d(TAG, "connect: " + this.castDevice.getAddress());
        int statusCode = connectToDevice();
        this.onConnected(statusCode);
        if (statusCode == CommonStatusCodes.SUCCESS) this.requestStatus();
    }

    @Override
    public void disconnect() {
        Log.d(TAG, "disconnect");
        closeDeviceConnection();
    }

    @Override
    public void sendMessage(String namespace, String message, long requestId) {
        try {
            this.connection.requireConnected();
            this.chromecast.sendRawRequest(namespace, message, requestId);
        } catch (IOException e) {
            Log.w(TAG, "Error sending cast message: " + e.getMessage());
            this.onSendMessageFailure(namespace, requestId, CommonStatusCodes.NETWORK_ERROR);
            return;
        }
        // Completes the send request of the client. Replies of the receiver arrive as regular messages.
        this.onSendMessageSuccess(namespace, requestId);
    }

    @Override
    public void leaveApplication() {
        Log.d(TAG, "leaveApplication");
        this.sessionId = null;
        this.onStatusRequestResult(CommonStatusCodes.SUCCESS);
    }

    @Override
    public void stopApplication(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) sessionId = this.sessionId;
        Log.d(TAG, "stopApplication: " + sessionId);
        if (sessionId == null) {
            this.onStatusRequestResult(CastStatusCodes.INVALID_REQUEST);
            return;
        }
        try {
            this.connection.requireConnected();
            this.chromecast.stopSession(sessionId);
        } catch (IOException e) {
            Log.w(TAG, "Error stopping cast application: " + e.getMessage());
            this.onStatusRequestResult(CommonStatusCodes.NETWORK_ERROR);
            return;
        }
        if (sessionId.equals(this.sessionId)) this.sessionId = null;
        this.onStatusRequestResult(CommonStatusCodes.SUCCESS);
    }

    /**
     * Asks the device for its status and passes it on to the client.
     */
    private void refreshStatus() throws IOException {
        this.connection.requireConnected();
        dispatchStatus(this.chromecast.getStatus());
    }

    @Override
    public void requestStatus() {
        try {
            refreshStatus();
        } catch (IOException e) {
            Log.w(TAG, "Error requesting cast device status: " + e.getMessage());
        }
    }

    @Override
    public void setVolume(double volume, double expectedVolume, boolean muted) {
        Log.d(TAG, "setVolume: " + volume);
        try {
            this.connection.requireConnected();
            this.chromecast.setVolume((float) Math.max(0, Math.min(1, volume)));
            // The device answers the sender that asked with a reply, which the library consumes, and only tells the
            // other senders with a spontaneous status. Without asking again the client would never learn about the
            // volume it just set.
            refreshStatus();
        } catch (IOException e) {
            Log.w(TAG, "Error setting volume: " + e.getMessage());
        }
    }

    @Override
    public void setMute(boolean mute, double volume, boolean expectedMute) {
        Log.d(TAG, "setMute: " + mute);
        try {
            this.connection.requireConnected();
            this.chromecast.setMuted(mute);
            // See setVolume
            refreshStatus();
        } catch (IOException e) {
            Log.w(TAG, "Error setting mute: " + e.getMessage());
        }
    }

    @Override
    public void registerNamespace(String namespace) {
        // The Cast library hands us every message of the device, so there is nothing to register
    }

    @Override
    public void unregisterNamespace(String namespace) {
        // See registerNamespace
    }

    @Override
    public void launchApplication(String applicationId, LaunchOptions launchOptions) {
        Log.d(TAG, "launchApplication: " + applicationId);
        if (!ensureConnected()) return;
        Application app;
        boolean launched = true;
        try {
            Application running = launchOptions != null && launchOptions.getRelaunchIfRunning() ? null : this.chromecast.getRunningApp();
            if (running != null && !running.isIdleScreen && running.sessionId != null && applicationId != null && applicationId.equals(running.id)) {
                // Another sender started it already. Launching again would restart it for everyone.
                Log.d(TAG, "Application is running already, joining session " + running.sessionId);
                app = running;
                launched = false;
            } else {
                app = this.chromecast.launchApp(applicationId);
            }
        } catch (ChromeCastException e) {
            Log.w(TAG, "Cast device failed to launch application: " + e.getMessage());
            this.onApplicationConnectionFailure(statusCodeFor(e));
            return;
        } catch (IOException e) {
            Log.w(TAG, "Error launching cast application: " + e.getMessage());
            this.onApplicationConnectionFailure(CommonStatusCodes.NETWORK_ERROR);
            return;
        }
        if (app == null || app.sessionId == null || (applicationId != null && !applicationId.equals(app.id))) {
            Log.w(TAG, "Cast device did not launch application " + applicationId + ", running: " + app);
            this.onApplicationConnectionFailure(CastStatusCodes.APPLICATION_NOT_FOUND);
            return;
        }
        this.sessionId = app.sessionId;

        ApplicationMetadata metadata = this.createMetadataFromApplication(app);
        this.onApplicationConnectionSuccess(metadata, app.statusText, app.sessionId, launched);
    }

    /**
     * The library reports all protocol level failures using the same exception type, only the message tells them apart.
     */
    private static int statusCodeFor(ChromeCastException e) {
        String message = e.getMessage();
        if (message != null && message.startsWith("Application launch error")) {
            // Receiver refused to launch the application
            return CastStatusCodes.APPLICATION_NOT_FOUND;
        }
        if (message != null && message.contains("timed out")) {
            return CommonStatusCodes.TIMEOUT;
        }
        return CommonStatusCodes.NETWORK_ERROR;
    }

    @Override
    public void joinApplication(String applicationId, String sessionId, JoinOptions joinOptions) {
        Log.d(TAG, "joinApplication: " + applicationId + " session " + sessionId);
        if (!ensureConnected()) return;
        Application app;
        try {
            app = this.chromecast.getRunningApp();
        } catch (IOException e) {
            Log.w(TAG, "Error joining cast application: " + e.getMessage());
            this.onApplicationConnectionFailure(CommonStatusCodes.NETWORK_ERROR);
            return;
        }
        boolean applicationMatches = applicationId == null || applicationId.isEmpty() || (app != null && applicationId.equals(app.id));
        boolean sessionMatches = sessionId == null || sessionId.isEmpty() || (app != null && sessionId.equals(app.sessionId));
        if (app == null || app.isIdleScreen || app.sessionId == null || !applicationMatches || !sessionMatches) {
            Log.d(TAG, "Application to join is not running, running: " + app);
            this.onApplicationConnectionFailure(CastStatusCodes.APPLICATION_NOT_RUNNING);
            return;
        }
        this.sessionId = app.sessionId;

        ApplicationMetadata metadata = this.createMetadataFromApplication(app);
        this.onApplicationConnectionSuccess(metadata, app.statusText, app.sessionId, false);
        // Volume might have changed while we were not connected
        this.requestStatus();
    }

    // endregion

    // region Calls to the client

    private interface ListenerCall {
        void call(ICastDeviceControllerListener listener) throws RemoteException;
    }

    private void callListener(String what, ListenerCall call) {
        ICastDeviceControllerListener listener = this.listener;
        if (listener == null) {
            Log.d(TAG, "No listener for " + what);
            return;
        }
        try {
            call.call(listener);
        } catch (Exception e) {
            Log.w(TAG, "Error calling " + what + ": " + e.getMessage());
        }
    }

    private void onConnected(int statusCode) {
        callListener("onConnected", l -> l.onConnected(statusCode));
    }

    private void onDisconnected(int reason) {
        callListener("onDisconnected", l -> l.onDisconnected(reason));
    }

    private void onConnectionSuspended(int reason) {
        callListener("onConnectionSuspended", l -> l.onConnectionSuspended(reason));
    }

    private void onApplicationConnectionSuccess(ApplicationMetadata applicationMetadata, String applicationStatus, String sessionId, boolean wasLaunched) {
        callListener("onApplicationConnectionSuccess", l -> l.onApplicationConnectionSuccess(applicationMetadata, applicationStatus, sessionId, wasLaunched));
    }

    private void onApplicationConnectionFailure(int statusCode) {
        callListener("onApplicationConnectionFailure", l -> l.onApplicationConnectionFailure(statusCode));
    }

    private void onStatusRequestResult(int statusCode) {
        callListener("onStatusRequestResult", l -> l.onStatusRequestResult(statusCode));
    }

    private void onTextMessageReceived(String namespace, String message) {
        callListener("onTextMessageReceived", l -> l.onTextMessageReceived(namespace, message));
    }

    private void onBinaryMessageReceived(String namespace, byte[] data) {
        callListener("onBinaryMessageReceived", l -> l.onBinaryMessageReceived(namespace, data));
    }

    private void onApplicationDisconnected(int statusCode) {
        callListener("onApplicationDisconnected", l -> l.onApplicationDisconnected(statusCode));
    }

    private void onSendMessageFailure(String response, long requestId, int statusCode) {
        callListener("onSendMessageFailure", l -> l.onSendMessageFailure(response, requestId, statusCode));
    }

    private void onSendMessageSuccess(String response, long requestId) {
        callListener("onSendMessageSuccess", l -> l.onSendMessageSuccess(response, requestId));
    }

    private void onApplicationStatusChanged(ApplicationStatus applicationStatus) {
        callListener("onApplicationStatusChanged", l -> l.onApplicationStatusChanged(applicationStatus));
    }

    private void onDeviceStatusChanged(CastDeviceStatus deviceStatus) {
        callListener("onDeviceStatusChanged", l -> l.onDeviceStatusChanged(deviceStatus));
    }

    // endregion
}
