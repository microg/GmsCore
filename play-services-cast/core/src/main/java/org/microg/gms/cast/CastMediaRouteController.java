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
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import androidx.mediarouter.media.MediaRouteProvider;
import androidx.mediarouter.media.MediaRouter;

import com.google.android.gms.common.images.WebImage;
import com.google.android.gms.cast.CastDevice;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.io.IOException;
import java.lang.Thread;
import java.lang.Runnable;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.ChromeCasts;
import su.litvak.chromecast.api.v2.ChromeCastConnectionEvent;
import su.litvak.chromecast.api.v2.ChromeCastSpontaneousEvent;
import su.litvak.chromecast.api.v2.ChromeCastRawMessage;
import su.litvak.chromecast.api.v2.ChromeCastSpontaneousEventListener;
import su.litvak.chromecast.api.v2.ChromeCastRawMessageListener;
import su.litvak.chromecast.api.v2.ChromeCastConnectionEventListener;
import su.litvak.chromecast.api.v2.ChromeCastsListener;
import su.litvak.chromecast.api.v2.Status;

public class CastMediaRouteController extends MediaRouteProvider.RouteController implements
    ChromeCastConnectionEventListener,
    ChromeCastSpontaneousEventListener,
    ChromeCastRawMessageListener {

    private static final String TAG = CastMediaRouteController.class.getSimpleName();

    private CastMediaRouteProvider provider;
    private String routeId;
    private ChromeCast chromecast;
    private boolean connected = false;

    public CastMediaRouteController(CastMediaRouteProvider provider, String routeId, String address) {
        super();

        this.provider = provider;
        this.routeId = routeId;
        this.chromecast = new ChromeCast(address);
        this.chromecast.registerConnectionListener(this);
        this.chromecast.registerListener(this);
        this.chromecast.registerRawMessageListener(this);
    }

    @Override
    public void connectionEventReceived(ChromeCastConnectionEvent event) {
        if (event.isConnected()) {
            Log.d(TAG, "Connected to ChromeCast: " + this.routeId);
            this.connected = true;
        } else {
            Log.d(TAG, "Disconnected from ChromeCast: " + this.routeId);
            this.connected = false;
        }
    }

    @Override
    public void spontaneousEventReceived(ChromeCastSpontaneousEvent event) {
        Log.d(TAG, "Spontaneous event received: " + event.getType() + " for " + this.routeId);
    }

    @Override
    public void rawMessageReceived(ChromeCastRawMessage message, Long requestId) {
        Log.d(TAG, "Raw message received: " + message.getPayloadType() + " for " + this.routeId);
    }

    public boolean onControlRequest(Intent intent, MediaRouter.ControlRequestCallback callback) {
        Log.d(TAG, "unimplemented Method: onControlRequest: " + this.routeId);
        return false;
    }

    @Override
    public void onRelease() {
        Log.d(TAG, "Releasing CastMediaRouteController: " + this.routeId);
        if (this.connected) {
            try {
                this.chromecast.disconnect();
            } catch (IOException e) {
                Log.w(TAG, "Error disconnecting chromecast: " + e.getMessage());
            }
        }
        this.connected = false;
    }

    @Override
    public void onSelect() {
        Log.d(TAG, "onSelect called for route: " + this.routeId);
        if (!this.connected) {
            try {
                this.chromecast.connect();
                Log.d(TAG, "Connection initiated to ChromeCast: " + this.routeId);
            } catch (IOException e) {
                Log.e(TAG, "Error connecting to ChromeCast: " + e.getMessage() + " for " + this.routeId);
            }
        } else {
            Log.d(TAG, "Already connected to ChromeCast: " + this.routeId);
        }
    }

    @Override
    public void onSetVolume(int volume) {
        Log.d(TAG, "onSetVolume: " + volume + " for " + this.routeId);
        if (this.connected) {
            try {
                this.chromecast.setVolume(volume / 20.0);  // Volume is 0-20, ChromeCast expects 0.0-1.0
            } catch (IOException e) {
                Log.e(TAG, "Error setting volume: " + e.getMessage() + " for " + this.routeId);
            }
        }
    }

    @Override
    public void onUnselect() {
        Log.d(TAG, "onUnselect for " + this.routeId);
        onUnselect(MediaRouter.UNSELECT_REASON_USER);
    }

    @Override
    public void onUnselect(int reason) {
        Log.d(TAG, "onUnselect: reason=" + reason + " for " + this.routeId);
        if (this.connected) {
            try {
                this.chromecast.disconnect();
            } catch (IOException e) {
                Log.w(TAG, "Error disconnecting chromecast: " + e.getMessage() + " for " + this.routeId);
            }
            this.connected = false;
        }
    }

    @Override
    public void onUpdateVolume(int delta) {
        Log.d(TAG, "unimplemented Method: onUpdateVolume: " + delta + " for " + this.routeId);
    }
}
