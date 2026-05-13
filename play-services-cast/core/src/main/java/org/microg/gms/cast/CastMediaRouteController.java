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
import java.util.concurrent.RejectedExecutionException;

import su.litvak.chromecast.api.v2.ChromeCast;

public class CastMediaRouteController extends MediaRouteProvider.RouteController {
    private static final String TAG = CastMediaRouteController.class.getSimpleName();

    private CastMediaRouteProvider provider;
    private String routeId;
    private ChromeCast chromecast;
    private final ExecutorService castExecutor = Executors.newSingleThreadExecutor();

    public CastMediaRouteController(CastMediaRouteProvider provider, String routeId, String address) {
        super();
        this.provider = provider;
        this.routeId = routeId;
        this.chromecast = new ChromeCast(address);
    }

    @Override
    public boolean onControlRequest(Intent intent, MediaRouter.ControlRequestCallback callback) {
        Log.d(TAG, "unimplemented Method: onControlRequest: " + this.routeId);
        return false;
    }

    @Override
    public void onRelease() {
        runCastOperation("releasing cast route controller", () -> {
            if (this.chromecast.isConnected()) {
                this.chromecast.disconnect();
            }
        });
        this.castExecutor.shutdown();
    }

    /**
     * Called when the user selects this route. Opens the TCP/TLS connection
     * to the Cast device so subsequent operations succeed immediately.
     */
    @Override
    public void onSelect() {
        runCastOperation("connecting to cast device on route select", () -> {
            if (!this.chromecast.isConnected()) {
                this.chromecast.connect();
            }
        });
    }

    @Override
    public void onSetVolume(int volume) {
        Log.d(TAG, "unimplemented Method: onSetVolume: " + this.routeId);
    }

    /**
     * Called when the user deselects or disconnects from this route.
     * Closes the TCP/TLS connection to the Cast device.
     */
    @Override
    public void onUnselect() {
        runCastOperation("disconnecting from cast device on route unselect", () -> {
            if (this.chromecast.isConnected()) {
                this.chromecast.disconnect();
            }
        });
    }

    @Override
    public void onUnselect(int reason) {
        onUnselect();
    }

    @Override
    public void onUpdateVolume(int delta) {
        Log.d(TAG, "unimplemented Method: onUpdateVolume: " + this.routeId);
    }

    private void runCastOperation(String operation, CastOperation castOperation) {
        try {
            this.castExecutor.execute(() -> {
                try {
                    castOperation.run();
                } catch (IOException | GeneralSecurityException e) {
                    Log.e(TAG, "Error " + operation + ": " + e.getMessage());
                }
            });
        } catch (RejectedExecutionException e) {
            Log.w(TAG, "Ignoring cast operation after release: " + operation);
        }
    }

    private interface CastOperation {
        void run() throws IOException, GeneralSecurityException;
    }
}
