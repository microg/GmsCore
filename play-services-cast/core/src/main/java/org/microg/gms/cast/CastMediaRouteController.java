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
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import su.litvak.chromecast.api.v2.ChromeCast;

public class CastMediaRouteController extends MediaRouteProvider.RouteController {
    private static final String TAG = CastMediaRouteController.class.getSimpleName();

    // Assumption: the route descriptor uses a volume range of 0..100.
    private static final float VOLUME_MAX = 100f;

    private final CastMediaRouteProvider provider;
    private final String routeId;
    private final ChromeCast chromecast;

    // Network calls must never run on the main thread, so every call
    // to the Chromecast goes through this single background thread.
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private volatile boolean connected = false;

    public CastMediaRouteController(CastMediaRouteProvider provider, String routeId, String address) {
        super();
        this.provider = provider;
        this.routeId = routeId;
        this.chromecast = new ChromeCast(address);
    }

    @Override
    public boolean onControlRequest(Intent intent, MediaRouter.ControlRequestCallback callback) {
        // TODO: map media control intents (play, pause, seek) to the Chromecast.
        Log.d(TAG, "unimplemented Method: onControlRequest: " + this.routeId);
        return false;
    }

    @Override
    public void onSelect() {
        Log.d(TAG, "onSelect: " + this.routeId);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Fix: connect() was never called, so every later call failed.
                    chromecast.connect();
                    connected = true;
                    Log.d(TAG, "Connected to Chromecast: " + routeId);
                } catch (IOException e) {
                    connected = false;
                    Log.w(TAG, "Connection failed (IO): " + routeId, e);
                } catch (GeneralSecurityException e) {
                    // connect() can also throw this one, not only IOException.
                    connected = false;
                    Log.w(TAG, "Connection failed (security): " + routeId, e);
                }
            }
        });
    }

    @Override
    public void onUnselect() {
        Log.d(TAG, "onUnselect: " + this.routeId);
        disconnectAsync();
    }

    @Override
    public void onUnselect(int reason) {
        Log.d(TAG, "onUnselect reason=" + reason + ": " + this.routeId);
        disconnectAsync();
    }

    @Override
    public void onRelease() {
        Log.d(TAG, "onRelease: " + this.routeId);
        disconnectAsync();
        // Lets the queued disconnect finish, then stops the thread.
        executor.shutdown();
    }

    @Override
    public void onSetVolume(final int volume) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (!connected) return;
                try {
                    float level = Math.max(0f, Math.min(1f, volume / VOLUME_MAX));
                    chromecast.setVolume(level);
                } catch (IOException e) {
                    Log.w(TAG, "setVolume failed: " + routeId, e);
                }
            }
        });
    }

    @Override
    public void onUpdateVolume(int delta) {
        // TODO: read the current volume from the Chromecast status, then add delta.
        Log.d(TAG, "unimplemented Method: onUpdateVolume: " + this.routeId);
    }

    private void disconnectAsync() {
        if (executor.isShutdown()) return;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (!connected) return;
                try {
                    chromecast.disconnect();
                } catch (IOException e) {
                    Log.w(TAG, "disconnect failed: " + routeId, e);
                } finally {
                    connected = false;
                }
            }
        });
    }
}
