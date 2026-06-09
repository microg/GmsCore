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
import android.util.Log;

import androidx.mediarouter.media.MediaRouteProvider;
import androidx.mediarouter.media.MediaRouter;

import java.io.IOException;

import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.Status;

public class CastMediaRouteController extends MediaRouteProvider.RouteController {
    private static final String TAG = CastMediaRouteController.class.getSimpleName();

    // MediaRouter volumes are integers in [0, VOLUME_MAX]; ChromeCast uses floats in [0.0, 1.0].
    private static final int VOLUME_MAX = 20;

    private final CastMediaRouteProvider provider;
    private final String routeId;
    private final ChromeCast chromecast;

    public CastMediaRouteController(CastMediaRouteProvider provider,
            String routeId, String address) {
        this.provider = provider;
        this.routeId = routeId;
        this.chromecast = new ChromeCast(address);
    }

    /**
     * Called when the user selects this route. Pre-connect so that the subsequent
     * launchApplication or joinApplication call completes faster.
     */
    @Override
    public void onSelect() {
        Log.d(TAG, "onSelect: " + routeId);
        new Thread(() -> {
            try {
                if (!chromecast.isConnected()) {
                    chromecast.connect();
                }
            } catch (IOException e) {
                Log.w(TAG, "Pre-connect on select failed: " + e.getMessage());
            }
        }, "CastRouteSelect-" + routeId).start();
    }

    @Override
    public void onUnselect() {
        onUnselect(MediaRouter.UNSELECT_REASON_UNKNOWN);
    }

    /**
     * Called when the route is deselected. Disconnects the underlying transport cleanly.
     */
    @Override
    public void onUnselect(int reason) {
        Log.d(TAG, "onUnselect reason=" + reason + " route=" + routeId);
        disconnectAsync();
    }

    /**
     * Called when this RouteController is permanently released. Disconnect if still connected.
     */
    @Override
    public void onRelease() {
        Log.d(TAG, "onRelease: " + routeId);
        disconnectAsync();
    }

    /**
     * Sets the absolute volume level (0 – VOLUME_MAX).
     */
    @Override
    public void onSetVolume(int volume) {
        float normalized = Math.max(0f, Math.min(1f, (float) volume / VOLUME_MAX));
        new Thread(() -> {
            try {
                if (chromecast.isConnected()) {
                    chromecast.setVolume(normalized);
                }
            } catch (IOException e) {
                Log.w(TAG, "Error setting volume: " + e.getMessage());
            }
        }, "CastSetVolume-" + routeId).start();
    }

    /**
     * Adjusts the volume by a relative delta (positive = louder, negative = quieter).
     */
    @Override
    public void onUpdateVolume(int delta) {
        new Thread(() -> {
            try {
                if (!chromecast.isConnected()) return;
                Status status = chromecast.getStatus();
                if (status != null && status.volume != null) {
                    float current = (float) status.volume.level;
                    float step = (float) delta / VOLUME_MAX;
                    float next = Math.max(0f, Math.min(1f, current + step));
                    chromecast.setVolume(next);
                }
            } catch (IOException e) {
                Log.w(TAG, "Error updating volume: " + e.getMessage());
            }
        }, "CastUpdateVolume-" + routeId).start();
    }

    /**
     * Media control requests (play/pause/seek/etc.) are handled by the Cast SDK layer via
     * CastDeviceControllerImpl, not directly here. Return false so MediaRouter passes them up.
     */
    @Override
    public boolean onControlRequest(Intent intent, MediaRouter.ControlRequestCallback callback) {
        return false;
    }

    private void disconnectAsync() {
        new Thread(() -> {
            try {
                if (chromecast.isConnected()) {
                    chromecast.disconnect();
                }
            } catch (IOException e) {
                Log.w(TAG, "Error disconnecting on unselect/release: " + e.getMessage());
            }
        }, "CastDisconnect-" + routeId).start();
    }
}
