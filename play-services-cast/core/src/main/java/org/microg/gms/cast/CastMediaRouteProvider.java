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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaRouteDescriptor;
import androidx.mediarouter.media.MediaRouteDiscoveryRequest;
import androidx.mediarouter.media.MediaRouteProvider;
import androidx.mediarouter.media.MediaRouteProviderDescriptor;
import androidx.mediarouter.media.MediaRouter;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;

import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Publishes Cast devices found on the local network as routes to the MediaRouter of client apps.
 * <p>
 * Runs in a {@link androidx.mediarouter.media.MediaRouteProviderService}. Everything here happens on the main thread:
 * callbacks of NsdManager and of {@link CastDeviceRegistry} arrive elsewhere and are posted to it.
 */
public class CastMediaRouteProvider extends MediaRouteProvider implements CastDeviceRegistry.Listener {
    private static final String TAG = "GmsCastRouteProvider";
    private static final String SERVICE_TYPE = "_googlecast._tcp.";
    private static final int MAX_RESOLVE_ATTEMPTS = 3;
    private static final long RESOLVE_RETRY_DELAY_MILLIS = 500;
    static final int ROUTE_VOLUME_MAX = 20;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Holds the devices we found, shared with the device controllers that connect to them
    private final CastDeviceRegistry registry = CastDeviceRegistry.getInstance();
    // Device id by name of the mDNS service that announced it
    private final Map<String, String> serviceCastIds = new HashMap<>();
    // Devices that stopped announcing themselves while a client was connected to them
    private final Set<String> lostWhileInUse = new HashSet<>();

    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener;

    // NsdManager only resolves one service at a time, so services found are queued
    private final ArrayDeque<NsdServiceInfo> resolveQueue = new ArrayDeque<>();
    private boolean resolving = false;
    private int resolveAttempts = 0;

    private final Set<String> customCategories = new LinkedHashSet<>();

    private enum State {
        NOT_DISCOVERING,
        DISCOVERY_REQUESTED,
        DISCOVERING,
        DISCOVERY_STOP_REQUESTED,
    }
    private State state = State.NOT_DISCOVERING;
    // Whether any client currently wants us to discover devices
    private boolean discoveryWanted = false;

    private static final ArrayList<IntentFilter> BASE_CONTROL_FILTERS = new ArrayList<>();
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
            "application/vnd.apple.mpegurl",
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

    @SuppressLint("NewApi")
    public CastMediaRouteProvider(Context context) {
        super(context);
        this.registry.setListener(this);

        if (android.os.Build.VERSION.SDK_INT < 16) {
            Log.i(TAG, "Cast discovery disabled. Android SDK version 16 or higher required.");
            return;
        }

        mNsdManager = (NsdManager)context.getApplicationContext().getSystemService(Context.NSD_SERVICE);

        // Callbacks of NsdManager arrive on a thread of its own
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                mainHandler.post(() -> {
                    state = State.DISCOVERING;
                    updateDiscovery();
                });
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                mainHandler.post(() -> {
                    resolveQueue.add(service);
                    resolveNext();
                });
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                String name = serviceInfo.getServiceName();
                mainHandler.post(() -> onChromeCastLost(name));
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                mainHandler.post(() -> {
                    state = State.NOT_DISCOVERING;
                    updateDiscovery();
                });
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.w(TAG, "Failed to start discovery. Error code " + errorCode);
                mainHandler.post(() -> state = State.NOT_DISCOVERING);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.w(TAG, "Failed to stop discovery. Error code " + errorCode);
                mainHandler.post(() -> state = State.DISCOVERING);
            }
        };
    }

    @SuppressLint("NewApi")
    private void resolveNext() {
        if (resolving) return;
        NsdServiceInfo service = resolveQueue.peek();
        if (service == null) return;
        resolving = true;
        try {
            mNsdManager.resolveService(service, new NsdManager.ResolveListener() {
                @Override
                public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                    mainHandler.post(() -> {
                        resolving = false;
                        if (errorCode == NsdManager.FAILURE_ALREADY_ACTIVE && ++resolveAttempts < MAX_RESOLVE_ATTEMPTS) {
                            // Someone else is resolving right now, try again in a moment
                            mainHandler.postDelayed(CastMediaRouteProvider.this::resolveNext, RESOLVE_RETRY_DELAY_MILLIS);
                            return;
                        }
                        Log.e(TAG, "Failed to resolve " + serviceInfo.getServiceName() + ". Error code " + errorCode);
                        resolveAttempts = 0;
                        resolveQueue.poll();
                        resolveNext();
                    });
                }

                @Override
                public void onServiceResolved(NsdServiceInfo serviceInfo) {
                    mainHandler.post(() -> {
                        resolving = false;
                        resolveAttempts = 0;
                        resolveQueue.poll();
                        onServiceResolvedInternal(serviceInfo);
                        resolveNext();
                    });
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "Failed to resolve " + service.getServiceName(), e);
            resolving = false;
            resolveQueue.poll();
            // Don't leave the services behind this one waiting for the next one to be found
            mainHandler.post(this::resolveNext);
        }
    }

    /**
     * A cast device announces all of its addresses. Prefer IPv4: it is reachable in every network
     * the device can be discovered in, while the IPv6 address may have no route from here.
     */
    private static InetAddress pickHost(NsdServiceInfo serviceInfo) {
        if (Build.VERSION.SDK_INT >= 34) {
            for (InetAddress address : serviceInfo.getHostAddresses()) {
                if (address instanceof Inet4Address) return address;
            }
        }
        return serviceInfo.getHost();
    }

    @SuppressLint("NewApi")
    private void onServiceResolvedInternal(NsdServiceInfo serviceInfo) {
        String name = serviceInfo.getServiceName();
        InetAddress host = pickHost(serviceInfo);
        int port = serviceInfo.getPort();
        Map<String, byte[]> attributes = serviceInfo.getAttributes();
        if (attributes == null || host == null) {
            Log.e(TAG, "Error getting service attributes from DNS-SD response");
            return;
        }
        try {
            String id = new String(attributes.get("id"), "UTF-8");
            String deviceVersion = getAttribute(attributes, "ve", "");
            String friendlyName = getAttribute(attributes, "fn", name);
            String modelName = getAttribute(attributes, "md", "");
            String iconPath = getAttribute(attributes, "ic", "");
            int status = 0;
            try {
                status = Integer.parseInt(getAttribute(attributes, "st", "0"));
            } catch (NumberFormatException ignored) {
            }

            // Bit field of CastDevice.CAPABILITY_*. Devices that don't tell are assumed to do audio and video.
            int capabilities = CastDevice.CAPABILITY_VIDEO_OUT | CastDevice.CAPABILITY_AUDIO_OUT;
            try {
                capabilities = Integer.parseInt(getAttribute(attributes, "ca", String.valueOf(capabilities)));
            } catch (NumberFormatException ignored) {
            }

            onChromeCastDiscovered(id, name, host, port, deviceVersion, friendlyName, modelName, iconPath, status, capabilities);
        } catch (UnsupportedEncodingException | NullPointerException ex) {
            Log.e(TAG, "Error getting cast details from DNS-SD response", ex);
        }
    }

    private static String getAttribute(Map<String, byte[]> attributes, String key, String defaultValue) throws UnsupportedEncodingException {
        byte[] value = attributes.get(key);
        return value == null ? defaultValue : new String(value, "UTF-8");
    }

    private void onChromeCastDiscovered(
            String id, String name, InetAddress host, int port, String
            deviceVersion, String friendlyName, String modelName, String
            iconPath, int status, int capabilities) {
        // Always replace, the device might have changed its address or name
        CastDevice castDevice = new CastDevice(id, name, host, port, deviceVersion, friendlyName, modelName, iconPath, status, capabilities);
        this.registry.addDiscovered(castDevice);
        this.lostWhileInUse.remove(id);
        this.serviceCastIds.put(name, id);
        Log.d(TAG, "Cast device found: " + friendlyName + " (" + host.getHostAddress() + ":" + port + ")");

        publishRoutes();
    }

    private void onChromeCastLost(String name) {
        String id = this.serviceCastIds.remove(name);
        if (id == null) return;
        if (this.registry.isConnected(id)) {
            // Announcements get lost at times. Removing the route would end the session of a device that is fine.
            Log.d(TAG, "Cast device lost but in use, keeping route: " + name);
            this.lostWhileInUse.add(id);
            return;
        }
        this.registry.removeDiscovered(id);
        Log.d(TAG, "Cast device lost: " + name);

        publishRoutes();
    }

    @SuppressLint("NewApi")
    @Override
    public void onDiscoveryRequestChanged(MediaRouteDiscoveryRequest request) {
        if (android.os.Build.VERSION.SDK_INT < 16) {
            return;
        }

        // Clients using the Cast framework request passive discovery while they are visible and only do an active scan
        // while their route chooser is open. Devices must be known before the chooser is opened, as apps commonly
        // hide the cast button unless a route is available.
        boolean wanted = false;
        if (request != null && request.isValid()) {
            boolean categoriesChanged = false;
            for (String category : request.getSelector().getControlCategories()) {
                if (CastMediaControlIntent.isCategoryForCast(category)) {
                    wanted = true;
                    categoriesChanged |= this.customCategories.add(category);
                }
            }
            if (categoriesChanged) {
                publishRoutes();
            }
        }
        this.discoveryWanted = wanted;
        updateDiscovery();
    }

    @SuppressLint("NewApi")
    private void updateDiscovery() {
        if (mNsdManager == null) return;
        try {
            if (discoveryWanted && state == State.NOT_DISCOVERING) {
                state = State.DISCOVERY_REQUESTED;
                mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
            } else if (!discoveryWanted && state == State.DISCOVERING) {
                state = State.DISCOVERY_STOP_REQUESTED;
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            }
            // Pending transitions are picked up again once NsdManager confirmed them
        } catch (Exception e) {
            Log.w(TAG, "Failed to update discovery", e);
            state = State.NOT_DISCOVERING;
        }
    }

    @Override
    public void onDeviceStateChanged(String deviceId) {
        this.mainHandler.post(() -> {
            if (!this.registry.isConnected(deviceId) && this.lostWhileInUse.remove(deviceId) && !this.serviceCastIds.containsValue(deviceId)) {
                // Kept for the client that was connected to it only
                this.registry.removeDiscovered(deviceId);
            }
            publishRoutes();
        });
    }

    private static int toRouteVolume(double volume) {
        return volume < 0 ? -1 : (int) Math.round(volume * ROUTE_VOLUME_MAX);
    }

    @Override
    public RouteController onCreateRouteController(String routeId) {
        if (this.registry.getDiscovered(routeId) == null) {
            return null;
        }
        return new CastMediaRouteController(routeId);
    }

    private void publishRoutes() {
        MediaRouteProviderDescriptor.Builder builder = new MediaRouteProviderDescriptor.Builder();
        for (CastDevice castDevice : this.registry.getDiscovered()) {
            ArrayList<IntentFilter> controlFilters = new ArrayList<>(BASE_CONTROL_FILTERS);
            // Include any app-specific control filters that have been requested.
            // TODO: Do we need to check with the device?
            for (String category : this.customCategories) {
                IntentFilter filter = new IntentFilter();
                filter.addCategory(category);
                controlFilters.add(filter);
            }

            Bundle extras = new Bundle();
            castDevice.putInBundle(extras);
            // Volume can only be changed through the connection of a running session
            int volume = toRouteVolume(this.registry.getVolume(castDevice.getDeviceId()));
            MediaRouteDescriptor route = new MediaRouteDescriptor.Builder(
                castDevice.getDeviceId(),
                castDevice.getFriendlyName())
                .setDescription(castDevice.getModelName())
                .addControlFilters(controlFilters)
                .setDeviceType(castDevice.hasCapability(CastDevice.CAPABILITY_VIDEO_OUT) ? MediaRouter.RouteInfo.DEVICE_TYPE_TV : MediaRouter.RouteInfo.DEVICE_TYPE_SPEAKER)
                .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
                .setVolumeHandling(volume < 0 ? MediaRouter.RouteInfo.PLAYBACK_VOLUME_FIXED : MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE)
                .setVolumeMax(ROUTE_VOLUME_MAX)
                .setVolume(Math.max(0, volume))
                .setEnabled(true)
                .setExtras(extras)
                .setConnectionState(MediaRouter.RouteInfo.CONNECTION_STATE_DISCONNECTED)
                .build();
            builder.addRoute(route);
        }
        this.setDescriptor(builder.build());
    }
}
