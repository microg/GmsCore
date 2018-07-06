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
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class CastMediaRouteProvider extends MediaRouteProvider {
    private static final String TAG = CastMediaRouteProvider.class.getSimpleName();

    private Map<String, MediaRouteDescriptor> routes = new HashMap<String, MediaRouteDescriptor>();

    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener;

    /**
     * TODO: Mock control filters for chromecast; Will likely need to be
     * adjusted.
     */
    private static final ArrayList<IntentFilter> CONTROL_FILTERS;
    static {
        IntentFilter f2 = new IntentFilter();
        f2.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        f2.addCategory(CastMediaControlIntent.CATEGORY_CAST);
        f2.addAction(MediaControlIntent.ACTION_PLAY);

        CONTROL_FILTERS = new ArrayList<IntentFilter>();
        CONTROL_FILTERS.add(f2);
    }

    public CastMediaRouteProvider(Context context) {
        super(context);

        mNsdManager = (NsdManager)context.getSystemService(Context.NSD_SERVICE);

        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "DiscoveryListener unimplemented Method: onDiscoveryStarted");
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
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "DiscoveryListener unimplemented Method: onDiscoveryStopped " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "DiscoveryListener unimplemented Method: onStartDiscoveryFailed: Error code:" + errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "DiscoveryListener unimplemented Method: onStopDiscoveryFailed: Error code:" + errorCode);
            }
        };
    }

    private void onChromeCastDiscovered(
            String id, String name, InetAddress host, int port, String
            deviceVersion, String friendlyName, String modelName, String
            iconPath, int status) {
        if (!this.routes.containsKey(id)) {
            // TODO: Capabilities
            int capabilities = CastDevice.CAPABILITY_VIDEO_OUT | CastDevice.CAPABILITY_AUDIO_OUT;

            Bundle extras = new Bundle();
            CastDevice castDevice = new CastDevice(id, name, host, port, deviceVersion, friendlyName, modelName, iconPath, status, capabilities);
            castDevice.putInBundle(extras);
            this.routes.put(id, new MediaRouteDescriptor.Builder(
                id,
                friendlyName)
                .setDescription(modelName)
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
        if (request.isValid() && request.isActiveScan()) {
            mNsdManager.discoverServices("_googlecast._tcp.", NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
        } else {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }
    }

    @Override
    public RouteController onCreateRouteController(String routeId) {
        MediaRouteDescriptor descriptor = this.routes.get(routeId);
        CastDevice castDevice = CastDevice.getFromBundle(descriptor.getExtras());
        return new CastMediaRouteController(this, routeId, castDevice.getAddress());
    }

    private void publishRoutes() {
        MediaRouteProviderDescriptor.Builder builder = new MediaRouteProviderDescriptor.Builder();
        for(MediaRouteDescriptor route : this.routes.values()) {
            builder.addRoute(route);
        }
        this.setDescriptor(builder.build());
    }
}
