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
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteDescriptor;
import android.support.v7.media.MediaRouteDiscoveryRequest;
import android.support.v7.media.MediaRouteProvider;
import android.support.v7.media.MediaRouteProviderDescriptor;
import android.support.v7.media.MediaRouter;
import android.util.Log;

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
import su.litvak.chromecast.api.v2.Status;
import su.litvak.chromecast.api.v2.ChromeCastsListener;

public class CastMediaRouteProvider extends MediaRouteProvider {
    private static final String TAG = CastMediaRouteProvider.class.getSimpleName();

    private Map<String, ChromeCast> chromecasts = new HashMap<String, ChromeCast>();

    public CastMediaRouteProvider(Context context) {
        super(context);

        // TODO: Uncomment this to manually discover a chromecast on the local
        // network. Discovery not yet implemented.
        /*
        InetAddress addr = null;
        try {
            addr = InetAddress.getByName("192.168.1.11");
        } catch (UnknownHostException e) {
            Log.d(TAG, "Chromecast status exception getting host: " + e.getMessage());
            return;
        }
        onChromeCastDiscovered(addr);
        */
    }

    private void onChromeCastDiscovered(InetAddress address) {
        ChromeCast chromecast = new ChromeCast(address.getHostAddress());

        new Thread(new Runnable() {
            public void run() {
                Status status = null;
                try {
                    status = chromecast.getStatus();
                } catch (IOException e) {
                    Log.e(TAG, "Exception getting chromecast status: " + e.getMessage());
                    return;
                }

                Handler mainHandler = new Handler(CastMediaRouteProvider.this.getContext().getMainLooper());
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        String routeId = address.getHostAddress();
                        CastMediaRouteProvider.this.chromecasts.put(routeId, chromecast);
                        publishRoutes();
                    }
                });
            }
        }).start();
    }

    /**
     * TODO: Mock control filters for chromecast; Will likely need to be
     * adjusted.
     */
    private static final ArrayList<IntentFilter> CONTROL_FILTERS;
    static {
        IntentFilter f2 = new IntentFilter();
        f2.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        f2.addAction(MediaControlIntent.ACTION_PLAY);

        CONTROL_FILTERS = new ArrayList<IntentFilter>();
        CONTROL_FILTERS.add(f2);
    }

    @Override
    public void onDiscoveryRequestChanged(MediaRouteDiscoveryRequest request) {
        Log.d(TAG, "unimplemented Method: onDiscoveryRequestChanged");
    }

    @Override
    public RouteController onCreateRouteController(String routeId) {
        ChromeCast chromecast = this.chromecasts.get(routeId);
        return new CastMediaRouteController(this, routeId, chromecast);
    }

    private void publishRoutes() {
        MediaRouteProviderDescriptor.Builder builder = new MediaRouteProviderDescriptor.Builder();
        for(Map.Entry<String, ChromeCast> entry : this.chromecasts.entrySet()) {
            String routeId = entry.getKey();
            ChromeCast chromecast = entry.getValue();
            Bundle extras = new Bundle();
            CastDevice castDevice = new CastDevice(
                routeId,
                chromecast.getAddress()
            );
            castDevice.putInBundle(extras);
            builder.addRoute(new MediaRouteDescriptor.Builder(
                routeId,
                routeId)
                .setDescription("Chromecast")
                .addControlFilters(CONTROL_FILTERS)
                .setDeviceType(MediaRouter.RouteInfo.DEVICE_TYPE_TV)
                .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
                .setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_FIXED)
                .setVolumeMax(20)
                .setVolume(0)
                .setEnabled(true)
                .setExtras(extras)
                .setConnectionState(MediaRouter.RouteInfo.CONNECTION_STATE_DISCONNECTED)
                .build());
        }

        this.setDescriptor(builder.build());
    }
}
