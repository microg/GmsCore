/*
 * Copyright (C) 2013-2026 microG Project Team
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
import android.media.AudioManager;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.util.Log;
import androidx.mediarouter.media.MediaRouteDescriptor;
import androidx.mediarouter.media.MediaRouteProvider;
import androidx.mediarouter.media.MediaRouteProviderDescriptor;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CastMediaRouteProvider extends MediaRouteProvider {
    private static final String TAG = "CastMediaRouteProvider";
    private static final String SERVICE_TYPE = "_googlecast._tcp.";
    private final NsdManager nsdManager;
    private final Map<String, CastDevice> discoveredDevices = new ConcurrentHashMap<>();
    private NsdManager.DiscoveryListener discoveryListener;

    public CastMediaRouteProvider(Context context) {
        super(context);
        this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    public synchronized void startDiscovery() {
        if (discoveryListener != null) return;
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery start failed: " + errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery stop failed: " + errorCode);
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.d(TAG, "Discovery started for: " + serviceType);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.d(TAG, "Discovery stopped");
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Found Cast service: " + serviceInfo.getServiceName());
                nsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        Log.w(TAG, "Resolve failed for " + serviceInfo.getServiceName() + ": " + errorCode);
                    }

                    @Override
                    public void onServiceResolved(NsdServiceInfo serviceInfo) {
                        handleResolvedService(serviceInfo);
                    }
                });
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Lost Cast service: " + serviceInfo.getServiceName());
                discoveredDevices.remove(serviceInfo.getServiceName());
                publishRoutes();
            }
        };
        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        } catch (Exception e) {
            Log.e(TAG, "Error initiating discoverServices", e);
        }
    }

    public synchronized void stopDiscovery() {
        if (discoveryListener != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener);
            } catch (Exception e) {
                Log.w(TAG, "Error stopping discovery", e);
            }
            discoveryListener = null;
            discoveredDevices.clear();
            publishRoutes();
        }
    }

    private void handleResolvedService(NsdServiceInfo serviceInfo) {
        InetAddress host = serviceInfo.getHost();
        int port = serviceInfo.getPort();
        String name = serviceInfo.getServiceName();
        CastDevice device = new CastDevice(name, host, name, "Chromecast", null, 0, port);
        discoveredDevices.put(name, device);
        publishRoutes();
    }

    private void publishRoutes() {
        MediaRouteProviderDescriptor.Builder providerBuilder = new MediaRouteProviderDescriptor.Builder();
        for (CastDevice device : discoveredDevices.values()) {
            Bundle extras = new Bundle();
            extras.putParcelable("com.google.android.gms.cast.EXTRA_CAST_DEVICE", device);

            MediaRouteDescriptor descriptor = new MediaRouteDescriptor.Builder(
                    device.getDeviceId(),
                    device.getFriendlyName())
                    .setDescription(device.getModelName())
                    .addControlFilter(CastMediaControlIntent.categoryForCast(device.getDeviceId()))
                    .setPlaybackStream(AudioManager.STREAM_MUSIC)
                    .setPlaybackType(MediaRouteDescriptor.PLAYBACK_TYPE_REMOTE)
                    .setVolumeHandling(MediaRouteDescriptor.VOLUME_HANDLING_REMOTE)
                    .setVolumeMax(20)
                    .setVolume(10)
                    .setExtras(extras)
                    .build();
            providerBuilder.addRoute(descriptor);
        }
        setDescriptor(providerBuilder.build());
    }
}