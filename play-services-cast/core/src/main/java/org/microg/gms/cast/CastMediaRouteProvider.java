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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaRouteDescriptor;
import androidx.mediarouter.media.MediaRouteDiscoveryRequest;
import androidx.mediarouter.media.MediaRouteProvider;
import androidx.mediarouter.media.MediaRouteProviderDescriptor;
import androidx.mediarouter.media.MediaRouter;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class CastMediaRouteProvider extends MediaRouteProvider {
    private static final String TAG = CastMediaRouteProvider.class.getSimpleName();

    private static final String SERVICE_TYPE = "_googlecast._tcp.";

    // Capability bits announced in the "ca" TXT record of _googlecast._tcp services.
    private static final int TXT_CAPABILITY_VIDEO_OUT = 1;
    private static final int TXT_CAPABILITY_VIDEO_IN = 2;
    private static final int TXT_CAPABILITY_AUDIO_OUT = 4;
    private static final int TXT_CAPABILITY_AUDIO_IN = 8;
    private static final int TXT_CAPABILITY_MULTIZONE_GROUP = 32;
    // CastDevice.CAPABILITY_* values are the same bits; the group bit is CAPABILITY_MULTIZONE_GROUP.
    private static final int CAPABILITY_MASK = TXT_CAPABILITY_VIDEO_OUT | TXT_CAPABILITY_VIDEO_IN | TXT_CAPABILITY_AUDIO_OUT | TXT_CAPABILITY_AUDIO_IN | TXT_CAPABILITY_MULTIZONE_GROUP;

    static final int VOLUME_MAX = 20;
    private static final int RESOLVE_RETRY_DELAY_MS = 500;
    private static final int RESOLVE_TIMEOUT_MS = 15000;
    private static final int DISCOVERY_RETRY_MIN_DELAY_MS = 1000;
    private static final int DISCOVERY_RETRY_MAX_DELAY_MS = 60000;

    private static class CastRoute {
        CastDevice device;
        boolean group;
        int volume = VOLUME_MAX / 2;
        int connectionState = MediaRouter.RouteInfo.CONNECTION_STATE_DISCONNECTED;
        // Route controllers created and not yet released. A selected route keeps its controller
        // while its own connection to the device is down, so this, not the connection state, tells
        // whether the route is still in use.
        int controllers;
        // Connection state reported by each route controller that is not disconnected. Each client
        // of the provider has its own controller for a route.
        final Map<Object, Integer> controllerStates = new HashMap<Object, Integer>();
    }

    /** Connected while any controller is connected, connecting while any is connecting. */
    private static void updateConnectionState(CastRoute route) {
        route.connectionState = MediaRouter.RouteInfo.CONNECTION_STATE_DISCONNECTED;
        for (int state : route.controllerStates.values()) {
            if (state == MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTED || route.connectionState == MediaRouter.RouteInfo.CONNECTION_STATE_DISCONNECTED) {
                route.connectionState = state;
            }
        }
    }

    private static boolean isInUse(CastRoute route) {
        return route.controllers > 0 || route.connectionState != MediaRouter.RouteInfo.CONNECTION_STATE_DISCONNECTED;
    }

    // All mutable state below is only accessed on the main thread.
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, CastRoute> routes = new LinkedHashMap<String, CastRoute>();
    private final Map<String, String> serviceCastIds = new HashMap<String, String>();
    private final Set<String> customCategories = new LinkedHashSet<String>();
    private final Queue<NsdServiceInfo> resolveQueue = new ArrayDeque<NsdServiceInfo>();
    private boolean resolving = false;
    // Incremented when discovery ends, see onDiscoveryEnded()
    private int discoveryRun;
    private NsdManager.ResolveListener activeResolve;
    private final Runnable resolveTimeout = this::onResolveTimeout;

    private NsdManager mNsdManager;
    private DiscoveryListener discoveryListener;
    private boolean discoveryWanted = false;
    private int discoveryRetryDelay = DISCOVERY_RETRY_MIN_DELAY_MS;
    private final Runnable retryDiscovery = this::updateDiscovery;

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

        String[] remotePlaybackActions = {
            MediaControlIntent.ACTION_PAUSE,
            MediaControlIntent.ACTION_RESUME,
            MediaControlIntent.ACTION_STOP,
            MediaControlIntent.ACTION_SEEK,
            MediaControlIntent.ACTION_GET_STATUS,
            MediaControlIntent.ACTION_START_SESSION,
            MediaControlIntent.ACTION_GET_SESSION_STATUS,
            MediaControlIntent.ACTION_END_SESSION,
        };
        for (String action : remotePlaybackActions) {
            filter = new IntentFilter();
            filter.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
            filter.addAction(action);
            BASE_CONTROL_FILTERS.add(filter);
        }

        filter = new IntentFilter();
        filter.addCategory(CastMediaControlIntent.CATEGORY_CAST_REMOTE_PLAYBACK);
        filter.addAction(CastMediaControlIntent.ACTION_SYNC_STATUS);
        BASE_CONTROL_FILTERS.add(filter);
    }

    public CastMediaRouteProvider(Context context) {
        super(context);

        // The TXT records, which carry the mandatory device id, are only readable from API 21.
        if (android.os.Build.VERSION.SDK_INT < 21) {
            Log.i(TAG, "Cast discovery disabled. Android SDK version 21 or higher required.");
            return;
        }

        mNsdManager = (NsdManager) context.getApplicationContext().getSystemService(Context.NSD_SERVICE);
    }

    /**
     * Control categories that Cast routes can serve. Discovery only runs while a MediaRouter
     * client asks for one of them.
     */
    private static boolean isCastCategory(String category) {
        return category.equals(CastMediaControlIntent.CATEGORY_CAST)
                || isAppSpecificCastCategory(category)
                || category.equals(CastMediaControlIntent.CATEGORY_CAST_REMOTE_PLAYBACK)
                || category.equals(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
    }

    /**
     * Categories that carry a receiver application id or namespaces, as built by
     * {@link CastMediaControlIntent#categoryForCast(String)} and
     * {@link CastMediaControlIntent#categoryForRemotePlayback(String)}.
     */
    private static boolean isAppSpecificCastCategory(String category) {
        return CastMediaControlIntent.isCategoryForCast(category)
                || category.startsWith(CastMediaControlIntent.CATEGORY_CAST_REMOTE_PLAYBACK + "/");
    }

    @Override
    public void onDiscoveryRequestChanged(MediaRouteDiscoveryRequest request) {
        boolean wanted = false;
        boolean categoriesChanged = false;
        if (request != null && request.isValid()) {
            for (String category : request.getSelector().getControlCategories()) {
                if (isCastCategory(category)) {
                    wanted = true;
                }
                // Every Cast device can launch any receiver application, so routes advertise all
                // app specific categories requested so far. They are kept so routes already handed
                // to an app keep matching its selector.
                if (isAppSpecificCastCategory(category) && customCategories.add(category)) {
                    categoriesChanged = true;
                }
            }
        }
        if (categoriesChanged) {
            publishRoutes();
        }

        discoveryWanted = wanted;
        updateDiscovery();
    }

    /**
     * Brings NSD discovery in line with {@link #discoveryWanted}. NsdManager accepts only one
     * pending start or stop per listener, so changes requested while one is in flight are applied
     * from the listener callbacks.
     */
    @SuppressLint("NewApi")
    private void updateDiscovery() {
        if (mNsdManager == null) return;
        handler.removeCallbacks(retryDiscovery);
        if (discoveryWanted && discoveryListener == null) {
            discoveryListener = new DiscoveryListener();
            Log.d(TAG, "Starting discovery of " + SERVICE_TYPE);
            try {
                mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
            } catch (RuntimeException e) {
                Log.w(TAG, "Failed to start discovery", e);
                discoveryListener = null;
                scheduleDiscoveryRetry();
            }
        } else if (!discoveryWanted && discoveryListener != null && discoveryListener.state == DiscoveryListener.STARTED) {
            Log.d(TAG, "Stopping discovery of " + SERVICE_TYPE);
            discoveryListener.state = DiscoveryListener.STOPPING;
            try {
                mNsdManager.stopServiceDiscovery(discoveryListener);
            } catch (RuntimeException e) {
                Log.w(TAG, "Failed to stop discovery", e);
                discoveryListener = null;
                onDiscoveryEnded();
            }
        }
    }

    /** Try to start discovery again later, with backoff, while it is still wanted. */
    private void scheduleDiscoveryRetry() {
        if (!discoveryWanted) return;
        handler.removeCallbacks(retryDiscovery);
        handler.postDelayed(retryDiscovery, discoveryRetryDelay);
        discoveryRetryDelay = Math.min(discoveryRetryDelay * 2, DISCOVERY_RETRY_MAX_DELAY_MS);
    }

    @SuppressLint("NewApi")
    private class DiscoveryListener implements NsdManager.DiscoveryListener {
        static final int STARTING = 0;
        static final int STARTED = 1;
        static final int STOPPING = 2;
        int state = STARTING;

        @Override
        public void onDiscoveryStarted(String serviceType) {
            handler.post(() -> {
                if (discoveryListener != this) return;
                state = STARTED;
                discoveryRetryDelay = DISCOVERY_RETRY_MIN_DELAY_MS;
                // Stop again if the request went away while starting.
                updateDiscovery();
            });
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.w(TAG, "Starting discovery failed. Error code " + errorCode);
            handler.post(() -> {
                if (discoveryListener != this) return;
                discoveryListener = null;
                // E.g. while the network is changing: discovery stays wanted, so try again.
                scheduleDiscoveryRetry();
            });
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            handler.post(() -> {
                if (discoveryListener != this) return;
                discoveryListener = null;
                onDiscoveryEnded();
                // Start again if a new request arrived while stopping.
                updateDiscovery();
            });
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.w(TAG, "Stopping discovery failed. Error code " + errorCode);
            // NsdManager has already unregistered this listener, so nothing is running for it.
            onDiscoveryStopped(serviceType);
        }

        @Override
        public void onServiceFound(NsdServiceInfo serviceInfo) {
            handler.post(() -> {
                queueResolve(serviceInfo);
                resolveNext();
            });
        }

        @Override
        public void onServiceLost(NsdServiceInfo serviceInfo) {
            String name = serviceInfo.getServiceName();
            handler.post(() -> {
                // A resolve of a service that is gone never completes.
                dropQueuedResolve(name);
                onChromeCastLost(name);
            });
        }
    }

    /**
     * NSD reports no losses for a discovery run once it has ended, so whether the devices it found
     * are still present is unknown. A new run finds those that are.
     */
    private void onDiscoveryEnded() {
        // Results of resolves that were started in the ended discovery run are ignored.
        discoveryRun++;
        resolveQueue.clear();
        serviceCastIds.clear();
        boolean removed = false;
        for (Iterator<CastRoute> it = routes.values().iterator(); it.hasNext(); ) {
            // Keep a route that is in use; it is removed once its controller is released.
            if (!isInUse(it.next())) {
                it.remove();
                removed = true;
            }
        }
        if (removed) publishRoutes();
    }

    private void queueResolve(NsdServiceInfo serviceInfo) {
        dropQueuedResolve(serviceInfo.getServiceName());
        resolveQueue.add(serviceInfo);
    }

    private void dropQueuedResolve(String name) {
        for (Iterator<NsdServiceInfo> it = resolveQueue.iterator(); it.hasNext(); ) {
            if (TextUtils.equals(name, it.next().getServiceName())) it.remove();
        }
    }

    /**
     * NsdManager rejects concurrent resolves with FAILURE_ALREADY_ACTIVE, so services found in a
     * burst (several Cast devices on the network) are resolved one after the other.
     */
    @SuppressLint("NewApi")
    private void resolveNext() {
        if (resolving || mNsdManager == null) return;
        final NsdServiceInfo serviceInfo = resolveQueue.poll();
        if (serviceInfo == null) return;
        final int run = discoveryRun;
        resolving = true;
        NsdManager.ResolveListener listener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo info, int errorCode) {
                handler.post(() -> {
                    if (activeResolve != this) return;
                    finishResolve();
                    if (errorCode == NsdManager.FAILURE_ALREADY_ACTIVE) {
                        // A resolve for another listener in this process is still running.
                        if (run == discoveryRun) queueResolve(serviceInfo);
                        handler.postDelayed(CastMediaRouteProvider.this::resolveNext, RESOLVE_RETRY_DELAY_MS);
                        return;
                    }
                    Log.w(TAG, "Resolving " + info.getServiceName() + " failed. Error code " + errorCode);
                    resolveNext();
                });
            }

            @Override
            public void onServiceResolved(NsdServiceInfo info) {
                handler.post(() -> {
                    if (activeResolve != this) return;
                    finishResolve();
                    // A device resolved after discovery stopped would never be reported lost.
                    if (discoveryListener != null && run == discoveryRun) onServiceResolvedInternal(info);
                    resolveNext();
                });
            }
        };
        activeResolve = listener;
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            // A resolve of a service that vanished never completes. Below API 34 it cannot be
            // cancelled and keeps the per client resolve slot, so a timeout would not help there.
            handler.postDelayed(resolveTimeout, RESOLVE_TIMEOUT_MS);
        }
        try {
            mNsdManager.resolveService(serviceInfo, listener);
        } catch (RuntimeException e) {
            Log.w(TAG, "Failed to resolve " + serviceInfo.getServiceName(), e);
            finishResolve();
            handler.post(this::resolveNext);
        }
    }

    private void finishResolve() {
        handler.removeCallbacks(resolveTimeout);
        activeResolve = null;
        resolving = false;
    }

    private void onResolveTimeout() {
        NsdManager.ResolveListener stale = activeResolve;
        if (stale == null) return;
        Log.w(TAG, "Resolving timed out");
        // Late callbacks of the stale listener are ignored.
        finishResolve();
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            try {
                mNsdManager.stopServiceResolution(stale);
            } catch (RuntimeException e) {
                Log.w(TAG, "Failed to stop resolving", e);
            }
        }
        resolveNext();
    }

    private static String getTxtString(Map<String, byte[]> attributes, String key) {
        byte[] value = attributes.get(key);
        if (value == null) return null;
        try {
            return new String(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    private static int getTxtInt(Map<String, byte[]> attributes, String key, int defaultValue) {
        String value = getTxtString(attributes, key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @RequiresApi(21)
    private void onServiceResolvedInternal(NsdServiceInfo serviceInfo) {
        String name = serviceInfo.getServiceName();
        InetAddress host = serviceInfo.getHost();
        int port = serviceInfo.getPort();
        Map<String, byte[]> attributes = serviceInfo.getAttributes();
        if (host == null || attributes == null) {
            Log.w(TAG, "Incomplete DNS-SD response for " + name);
            return;
        }

        // Only the id is mandatory, older and third party receivers omit some of the other records.
        String id = getTxtString(attributes, "id");
        if (id == null || id.isEmpty()) {
            Log.w(TAG, "No device id in DNS-SD response for " + name);
            return;
        }
        String deviceVersion = getTxtString(attributes, "ve");
        String friendlyName = getTxtString(attributes, "fn");
        if (friendlyName == null || friendlyName.isEmpty()) friendlyName = name;
        String modelName = getTxtString(attributes, "md");
        String iconPath = getTxtString(attributes, "ic");
        int status = getTxtInt(attributes, "st", 0);
        int txtCapabilities = getTxtInt(attributes, "ca", TXT_CAPABILITY_VIDEO_OUT | TXT_CAPABILITY_AUDIO_OUT);

        onChromeCastDiscovered(id, name, host, port, deviceVersion, friendlyName, modelName, iconPath, status, txtCapabilities);
    }

    private void onChromeCastDiscovered(
            String id, String name, InetAddress host, int port, String
            deviceVersion, String friendlyName, String modelName, String
            iconPath, int status, int txtCapabilities) {
        int capabilities = txtCapabilities & CAPABILITY_MASK;
        CastDevice castDevice = new CastDevice(id, name, host, port, deviceVersion, friendlyName, modelName, iconPath, status, capabilities);

        CastRoute route = routes.get(id);
        if (route == null) {
            route = new CastRoute();
            routes.put(id, route);
            Log.d(TAG, "Found Cast device " + castDevice);
        }
        // Always take the latest record: address, port, name and status change over time.
        route.device = castDevice;
        route.group = (txtCapabilities & TXT_CAPABILITY_MULTIZONE_GROUP) != 0;
        serviceCastIds.put(name, id);

        publishRoutes();
    }

    private void onChromeCastLost(String name) {
        String id = serviceCastIds.remove(name);
        if (id == null) return;
        CastRoute route = routes.get(id);
        // Keep a route that is in use; it is removed once its controller disconnects.
        if (route != null && !isInUse(route)) {
            routes.remove(id);
            Log.d(TAG, "Lost Cast device " + id);
            publishRoutes();
        }
    }

    @Override
    public RouteController onCreateRouteController(String routeId) {
        CastRoute route = routes.get(routeId);
        if (route == null) {
            return null;
        }
        route.controllers++;
        return new CastMediaRouteController(this, routeId, route.device.getAddress(), route.device.getServicePort(), route.volume);
    }

    /**
     * Called by a route controller, from any thread, when MediaRouter released it.
     */
    void onRouteControllerReleased(final Object controller, final String routeId) {
        handler.post(() -> {
            CastRoute route = routes.get(routeId);
            if (route == null) return;
            if (route.controllers > 0) route.controllers--;
            if (route.controllerStates.remove(controller) != null) {
                updateConnectionState(route);
                publishRoutes();
            }
            if (!isInUse(route) && !serviceCastIds.containsValue(routeId)) {
                // The device was lost while it was in use.
                routes.remove(routeId);
                publishRoutes();
            }
        });
    }

    /**
     * Called by route controllers, from any thread, to reflect connection and volume state of a
     * route in the published descriptor. A negative connection state or volume keeps the current
     * value. When several apps selected the route, each through its own controller, the route
     * stays connected until all of them disconnected.
     */
    void onRouteStateChanged(final Object controller, final String routeId, final int connectionState, final int volume) {
        handler.post(() -> {
            CastRoute route = routes.get(routeId);
            if (route == null) return;
            if (connectionState == MediaRouter.RouteInfo.CONNECTION_STATE_DISCONNECTED) {
                route.controllerStates.remove(controller);
            } else if (connectionState >= 0) {
                route.controllerStates.put(controller, connectionState);
            }
            updateConnectionState(route);
            if (volume >= 0) route.volume = Math.max(0, Math.min(VOLUME_MAX, volume));
            if (!isInUse(route) && !serviceCastIds.containsValue(routeId)) {
                // The device was lost while it was in use.
                routes.remove(routeId);
            }
            publishRoutes();
        });
    }

    private void publishRoutes() {
        MediaRouteProviderDescriptor.Builder builder = new MediaRouteProviderDescriptor.Builder();
        for (CastRoute route : routes.values()) {
            CastDevice castDevice = route.device;
            ArrayList<IntentFilter> controlFilters = new ArrayList<IntentFilter>(BASE_CONTROL_FILTERS);
            for (String category : customCategories) {
                IntentFilter filter = new IntentFilter();
                filter.addCategory(category);
                controlFilters.add(filter);
            }

            boolean video = castDevice.hasCapability(CastDevice.CAPABILITY_VIDEO_OUT);

            Bundle extras = new Bundle();
            castDevice.putInBundle(extras);
            MediaRouteDescriptor descriptor = new MediaRouteDescriptor.Builder(
                castDevice.getDeviceId(),
                castDevice.getFriendlyName())
                .setDescription(castDevice.getModelName())
                .addControlFilters(controlFilters)
                .setDeviceType(video && !route.group ? MediaRouter.RouteInfo.DEVICE_TYPE_TV : MediaRouter.RouteInfo.DEVICE_TYPE_SPEAKER)
                .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
                .setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE)
                .setVolumeMax(VOLUME_MAX)
                .setVolume(route.volume)
                .setEnabled(true)
                .setExtras(extras)
                .setConnectionState(route.connectionState)
                .build();
            builder.addRoute(descriptor);
        }
        this.setDescriptor(builder.build());
    }
}
