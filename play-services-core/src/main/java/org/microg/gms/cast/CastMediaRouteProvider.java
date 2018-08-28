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
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteDescriptor;
import android.support.v7.media.MediaRouteDiscoveryRequest;
import android.support.v7.media.MediaRouteProvider;
import android.support.v7.media.MediaRouteProviderDescriptor;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import com.google.android.gms.common.images.WebImage;
import com.google.android.gms.cast.CastDevice;

import java.util.ArrayList;
import java.net.InetAddress;
import java.net.Inet4Address;

public class CastMediaRouteProvider extends MediaRouteProvider {
    private static final String TAG = CastMediaRouteProvider.class.getSimpleName();

    public CastMediaRouteProvider(Context context) {
        super(context);
        Log.d(TAG, "unimplemented Method: CastMediaRouteProvider");

        // Uncomment this to create the mock device
        // publishRoutes();
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
        Log.d(TAG, "unimplemented Method: onCreateRouteController");
        return null;
    }

    /**
     * TODO: Currently this method simply publishes a single cast route for
     * testing.
     */
    private void publishRoutes() {
        Log.d(TAG, "unimplemented Method: publishRoutes");
        Bundle extras = new Bundle();
        CastDevice castDevice = new CastDevice("abc123");
        castDevice.putInBundle(extras);
        MediaRouteDescriptor routeDescriptor1 = new MediaRouteDescriptor.Builder(
            "abc123",
            "Rotue Friendly Name")
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
            .build();
        MediaRouteProviderDescriptor providerDescriptor =
            new MediaRouteProviderDescriptor.Builder()
            .addRoute(routeDescriptor1)
            .build();
        this.setDescriptor(providerDescriptor);
    }
}
