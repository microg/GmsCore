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

import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.mediarouter.media.MediaRouteProvider;
import androidx.mediarouter.media.MediaRouter;

import org.microg.gms.cast.channel.CastChannel;
import org.microg.gms.cast.channel.CastDeviceSession;
import org.microg.gms.cast.channel.ReceiverApplication;
import org.microg.gms.cast.channel.ReceiverStatus;

public class CastMediaRouteController extends MediaRouteProvider.RouteController {
    private static final String TAG = CastMediaRouteController.class.getSimpleName();

    private final CastMediaRouteProvider provider;
    private final String routeId;
    private final String host;
    private final int port;

    // Status replies to an older SET_VOLUME arrive after newer local changes; ignore them for this long.
    private static final long LOCAL_VOLUME_HOLD_MS = 1000;

    private volatile int volume;
    private volatile boolean volumeKnown;
    private volatile int requestedVolume = -1;
    private volatile long requestedVolumeAt;
    private volatile boolean deviceMuted;
    // The control connection of the selected route, used to follow and change the device volume.
    // Guarded by this; callbacks of a replaced session are ignored. Route state changes are posted
    // to the provider while holding the lock, so they arrive in the order the session changed.
    private CastDeviceSession session;
    private boolean sessionConnected;
    private boolean selected;
    private boolean released;

    public CastMediaRouteController(CastMediaRouteProvider provider, String routeId, String address, int port, int volume) {
        super();

        this.provider = provider;
        this.routeId = routeId;
        this.host = address;
        this.port = port > 0 ? port : CastChannel.DEFAULT_PORT;
        this.volume = volume;
    }

    @Override
    public boolean onControlRequest(Intent intent, MediaRouter.ControlRequestCallback callback) {
        Log.d(TAG, "unimplemented Method: onControlRequest: " + this.routeId);
        return false;
    }

    @Override
    public void onSelect() {
        CastDeviceSession newSession;
        synchronized (this) {
            if (released) return;
            selected = true;
            if (session != null) return;
            newSession = startSessionLocked();
        }
        newSession.connect();
    }

    private CastDeviceSession startSessionLocked() {
        SessionCallbacks callbacks = new SessionCallbacks();
        CastDeviceSession newSession = new CastDeviceSession(host, port, callbacks);
        callbacks.session = newSession;
        session = newSession;
        sessionConnected = false;
        provider.onRouteStateChanged(CastMediaRouteController.this, routeId, MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTING, -1);
        return newSession;
    }

    @Override
    public void onUnselect() {
        onUnselect(MediaRouter.UNSELECT_REASON_UNKNOWN);
    }

    @Override
    public void onUnselect(int reason) {
        closeSession();
    }

    @Override
    public void onRelease() {
        synchronized (this) {
            if (released) return;
            released = true;
        }
        closeSession();
        provider.onRouteControllerReleased(CastMediaRouteController.this, routeId);
    }

    private void closeSession() {
        CastDeviceSession oldSession;
        synchronized (this) {
            selected = false;
            oldSession = session;
            session = null;
            sessionConnected = false;
            if (oldSession != null) {
                provider.onRouteStateChanged(CastMediaRouteController.this, routeId, MediaRouter.RouteInfo.CONNECTION_STATE_DISCONNECTED, -1);
            }
        }
        if (oldSession != null) oldSession.disconnect();
    }

    @Override
    public void onSetVolume(int volume) {
        int newVolume = Math.max(0, Math.min(CastMediaRouteProvider.VOLUME_MAX, volume));
        this.volume = newVolume;
        requestedVolume = newVolume;
        requestedVolumeAt = SystemClock.elapsedRealtime();
        provider.onRouteStateChanged(CastMediaRouteController.this, routeId, -1, newVolume);

        // Volume is a device setting, so any open connection to the receiver can change it. Prefer
        // the route's own connection, then one an app opened through the device controller.
        CastDeviceSession target;
        CastDeviceSession started = null;
        synchronized (this) {
            target = sessionConnected ? session : null;
        }
        if (target == null) target = CastChannelRegistry.get(routeId);
        if (target == null) {
            synchronized (this) {
                // Still connecting: the request is sent once the connection is up. If the connection
                // dropped while the route stays selected, open it again.
                target = session;
                if (target == null && selected && !released) target = started = startSessionLocked();
            }
        }
        if (started != null) started.connect();
        if (target != null) {
            target.setVolume((double) newVolume / CastMediaRouteProvider.VOLUME_MAX);
            // A level change on a muted receiver would otherwise stay inaudible.
            if (deviceMuted && newVolume > 0) target.setMute(false);
        } else {
            Log.d(TAG, "No connection to " + routeId + " to set volume");
        }
    }

    @Override
    public void onUpdateVolume(int delta) {
        if (!volumeKnown) {
            // A step relative to the placeholder volume would jump the device to an arbitrary level.
            Log.d(TAG, "Volume of " + routeId + " not known yet, ignoring step");
            return;
        }
        onSetVolume(volume + delta);
    }

    private class SessionCallbacks implements CastDeviceSession.Callbacks {
        CastDeviceSession session;

        private boolean isCurrent() {
            synchronized (CastMediaRouteController.this) {
                return CastMediaRouteController.this.session == session;
            }
        }

        private void onSessionEnded(int statusCode) {
            synchronized (CastMediaRouteController.this) {
                if (CastMediaRouteController.this.session != session) return;
                CastMediaRouteController.this.session = null;
                sessionConnected = false;
                provider.onRouteStateChanged(CastMediaRouteController.this, routeId, MediaRouter.RouteInfo.CONNECTION_STATE_DISCONNECTED, -1);
            }
            Log.d(TAG, "Connection to " + routeId + " ended: " + statusCode);
            session.disconnect();
        }

        @Override
        public void onConnected() {
            synchronized (CastMediaRouteController.this) {
                if (CastMediaRouteController.this.session != session) return;
                sessionConnected = true;
                provider.onRouteStateChanged(CastMediaRouteController.this, routeId, MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTED, -1);
            }
        }

        @Override
        public void onConnectionFailed(int statusCode) {
            onSessionEnded(statusCode);
        }

        @Override
        public void onDisconnected(int statusCode) {
            onSessionEnded(statusCode);
        }

        @Override
        public void onDeviceStatusChanged(@NonNull ReceiverStatus status) {
            if (!isCurrent()) return;
            deviceMuted = status.getMuted();
            if (!status.getHasVolumeLevel()) return;
            // Follow volume changes made on the device or by other senders.
            int deviceVolume = (int) Math.round(status.getVolumeLevel() * CastMediaRouteProvider.VOLUME_MAX);
            volumeKnown = true;
            if (deviceVolume != requestedVolume && SystemClock.elapsedRealtime() - requestedVolumeAt < LOCAL_VOLUME_HOLD_MS) return;
            volume = deviceVolume;
            provider.onRouteStateChanged(CastMediaRouteController.this, routeId, -1, deviceVolume);
        }

        @Override
        public void onApplicationConnected(@NonNull ReceiverApplication application, boolean wasLaunched) {
        }

        @Override
        public void onApplicationConnectionFailed(int statusCode) {
        }

        @Override
        public void onApplicationStatusChanged(String statusText) {
        }

        @Override
        public void onApplicationDisconnected(int statusCode) {
        }

        @Override
        public void onStopApplicationResult(int statusCode) {
        }

        @Override
        public void onLeaveApplicationResult(int statusCode) {
        }

        @Override
        public void onTextMessage(@NonNull String namespace, @NonNull String message) {
        }

        @Override
        public void onBinaryMessage(@NonNull String namespace, @NonNull byte[] data) {
        }

        @Override
        public void onSendMessageSuccess(@NonNull String namespace, long requestId) {
        }

        @Override
        public void onSendMessageFailure(@NonNull String namespace, long requestId, int statusCode) {
        }
    }
}
