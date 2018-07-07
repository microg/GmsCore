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
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
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
import com.google.android.gms.cast.CastMediaControlIntent;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.Thread;
import java.lang.Runnable;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class CastMediaRouteProvider extends MediaRouteProvider {
    private static final String TAG = CastMediaRouteProvider.class.getSimpleName();

    private Map<String, CastDevice> castDevices = new HashMap<String, CastDevice>();

    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener;

    private List<String> customCategories = new ArrayList<String>();

    private enum State {
        NOT_DISCOVERING,
        DISCOVERY_REQUESTED,
        DISCOVERING,
        DISCOVERY_STOP_REQUESTED,
    }
    private State state = State.NOT_DISCOVERING;

    private static final ArrayList<IntentFilter> BASE_CONTROL_FILTERS = new ArrayList<IntentFilter>();
    static {
        IntentFilter filter;

        filter = new IntentFilter();
        filter.addCategory(CastMediaControlIntent.CATEGORY_CAST);
        BASE_CONTROL_FILTERS.add(filter);

        filter = new IntentFilter();
        filter.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        filter.addAction(MediaControlIntent.ACTION_PLAY);
        filter.addDataScheme("http");
        filter.addDataScheme("https");
        String[] types = {
            "image/jpeg",
            "image/pjpeg",
            "image/jpg",
            "image/webp",
            "image/png",
            "image/gif",
            "image/bmp",
            "image/vnd.microsoft.icon",
            "image/x-icon",
            "image/x-xbitmap",
            "audio/wav",
            "audio/x-wav",
            "audio/mp3",
            "audio/x-mp3",
            "audio/x-m4a",
            "audio/mpeg",
            "audio/webm",
            "audio/ogg",
            "audio/x-matroska",
            "video/mp4",
            "video/x-m4v",
            "video/mp2t",
            "video/webm",
            "video/ogg",
            "video/x-matroska",
            "application/x-mpegurl",
            "<item>application/vnd.apple.mpegurl",
            "application/dash+xml",
            "application/vnd.ms-sstr+xml",
        };
        for (String type : types) {
            try {
                filter.addDataType(type);
            } catch (IntentFilter.MalformedMimeTypeException ex) {
                Log.e(TAG, "Error adding filter type " + type);
            }
        }
        BASE_CONTROL_FILTERS.add(filter);

        filter = new IntentFilter();
        filter.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        filter.addAction(MediaControlIntent.ACTION_PAUSE);
        BASE_CONTROL_FILTERS.add(filter);

        filter = new IntentFilter();
        filter.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        filter.addAction(MediaControlIntent.ACTION_RESUME);
        BASE_CONTROL_FILTERS.add(filter);

        filter = new IntentFilter();
        filter.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        filter.addAction(MediaControlIntent.ACTION_STOP);
        BASE_CONTROL_FILTERS.add(filter);

        filter = new IntentFilter();
        filter.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        filter.addAction(MediaControlIntent.ACTION_SEEK);
        BASE_CONTROL_FILTERS.add(filter);

        filter = new IntentFilter();
        filter.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        filter.addAction(MediaControlIntent.ACTION_GET_STATUS);
        BASE_CONTROL_FILTERS.add(filter);

        filter = new IntentFilter();
        filter.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        filter.addAction(MediaControlIntent.ACTION_START_SESSION);
        BASE_CONTROL_FILTERS.add(filter);

        filter = new IntentFilter();
        filter.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        filter.addAction(MediaControlIntent.ACTION_GET_SESSION_STATUS);
        BASE_CONTROL_FILTERS.add(filter);

        filter = new IntentFilter();
        filter.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        filter.addAction(MediaControlIntent.ACTION_END_SESSION);
        BASE_CONTROL_FILTERS.add(filter);

        filter = new IntentFilter();
        filter.addCategory(CastMediaControlIntent.CATEGORY_CAST_REMOTE_PLAYBACK);
        filter.addAction(CastMediaControlIntent.ACTION_SYNC_STATUS);
        BASE_CONTROL_FILTERS.add(filter);

        filter = new IntentFilter();
        filter.addCategory(CastMediaControlIntent.CATEGORY_CAST_REMOTE_PLAYBACK);
        filter.addAction(CastMediaControlIntent.ACTION_SYNC_STATUS);
        BASE_CONTROL_FILTERS.add(filter);
    }

    public CastMediaRouteProvider(Context context) {
        super(context);

        mNsdManager = (NsdManager)context.getSystemService(Context.NSD_SERVICE);

        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "DiscoveryListener unimplemented Method: onDiscoveryStarted");
                CastMediaRouteProvider.this.state = State.DISCOVERING;
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                mNsdManager.resolveService(service, new NsdManager.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        Log.e(TAG, "DiscoveryListener unimplemented Method: Resolve failed" + errorCode);
                    }

                    @Override
                    public void onServiceResolved(NsdServiceInfo serviceInfo) {
                        String name = serviceInfo.getServiceName();
                        InetAddress host = serviceInfo.getHost();
                        int port = serviceInfo.getPort();
                        try {
                            String id = new String(serviceInfo.getAttributes().get("id"), "UTF-8");
                            String deviceVersion = new String(serviceInfo.getAttributes().get("ve"), "UTF-8");
                            String friendlyName = new String(serviceInfo.getAttributes().get("fn"), "UTF-8");
                            String modelName = new String(serviceInfo.getAttributes().get("md"), "UTF-8");
                            String iconPath = new String(serviceInfo.getAttributes().get("ic"), "UTF-8");
                            int status = Integer.parseInt(new String(serviceInfo.getAttributes().get("st"), "UTF-8"));

                            onChromeCastDiscovered(id, name, host, port, deviceVersion, friendlyName, modelName, iconPath, status);
                        } catch (UnsupportedEncodingException ex) {
                            Log.e(TAG, "Error getting cast details from DNS-SD response", ex);
                            return;
                        }
                    }
                });
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.d(TAG, "DiscoveryListener unimplemented Method: onServiceLost" + service);
                // TODO: Remove chromecast route.
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "DiscoveryListener unimplemented Method: onDiscoveryStopped " + serviceType);
                CastMediaRouteProvider.this.state = State.NOT_DISCOVERING;
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "DiscoveryListener unimplemented Method: onStartDiscoveryFailed: Error code:" + errorCode);
                CastMediaRouteProvider.this.state = State.NOT_DISCOVERING;
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "DiscoveryListener unimplemented Method: onStopDiscoveryFailed: Error code:" + errorCode);
                CastMediaRouteProvider.this.state = State.DISCOVERING;
            }
        };
    }

    private void onChromeCastDiscovered(
            String id, String name, InetAddress host, int port, String
            deviceVersion, String friendlyName, String modelName, String
            iconPath, int status) {
        if (!this.castDevices.containsKey(id)) {
            // TODO: Capabilities
            int capabilities = CastDevice.CAPABILITY_VIDEO_OUT | CastDevice.CAPABILITY_AUDIO_OUT;

            CastDevice castDevice = new CastDevice(id, name, host, port, deviceVersion, friendlyName, modelName, iconPath, status, capabilities);
            this.castDevices.put(id, castDevice);
        }

        Handler mainHandler = new Handler(this.getContext().getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                publishRoutes();
            }
        });
    }

    @Override
    public void onDiscoveryRequestChanged(MediaRouteDiscoveryRequest request) {
        if (request != null && request.isValid() && request.isActiveScan()) {
            if (request.getSelector() != null) {
                for (String category : request.getSelector().getControlCategories()) {
                    if (CastMediaControlIntent.isCategoryForCast(category)) {
                        this.customCategories.add(category);
                    }
                }
            }
            if (this.state == State.NOT_DISCOVERING) {
                mNsdManager.discoverServices("_googlecast._tcp.", NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
                this.state = State.DISCOVERY_REQUESTED;
            }
        } else {
            if (this.state == State.DISCOVERING) {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
                this.state = State.DISCOVERY_STOP_REQUESTED;
            }
        }
    }

    @Override
    public RouteController onCreateRouteController(String routeId) {
        CastDevice castDevice = this.castDevices.get(routeId);
        return new CastMediaRouteController(this, routeId, castDevice.getAddress());
    }

    private void publishRoutes() {
        MediaRouteProviderDescriptor.Builder builder = new MediaRouteProviderDescriptor.Builder();
        for (CastDevice castDevice : this.castDevices.values()) {
            ArrayList<IntentFilter> controlFilters = new ArrayList<IntentFilter>(BASE_CONTROL_FILTERS);
            // Include any app-specific control filters that have been requested.
            // TODO: Do we need to check with the device?
            for (String category : this.customCategories) {
                IntentFilter filter = new IntentFilter();
                filter.addCategory(category);
                controlFilters.add(filter);
            }

            Bundle extras = new Bundle();
            castDevice.putInBundle(extras);
            MediaRouteDescriptor route = new MediaRouteDescriptor.Builder(
                castDevice.getDeviceId(),
                castDevice.getFriendlyName())
                .setDescription(castDevice.getModelName())
                .addControlFilters(controlFilters)
                .setDeviceType(MediaRouter.RouteInfo.DEVICE_TYPE_TV)
                .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
                .setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_FIXED)
                .setVolumeMax(20)
                .setVolume(0)
                .setEnabled(true)
                .setExtras(extras)
                .setConnectionState(MediaRouter.RouteInfo.CONNECTION_STATE_DISCONNECTED)
                .build();
            builder.addRoute(route);
        }
        this.setDescriptor(builder.build());
    }
}
