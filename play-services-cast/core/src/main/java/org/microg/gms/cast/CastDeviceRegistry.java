/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast;

import com.google.android.gms.cast.CastDevice;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What this process knows about Cast devices: which ones discovery found, and which ones a client is connected to.
 * <p>
 * This is where the two halves of Cast support meet. {@link CastMediaRouteProvider} discovers devices and publishes
 * them as routes, {@link CastDeviceControllerImpl} connects to them on behalf of clients. The controller only connects
 * to devices the provider found, and the provider needs to know about connections to show the volume of a device and
 * to pass on volume requests.
 */
final class CastDeviceRegistry {
    interface Listener {
        /**
         * A client connected to or disconnected from the device, or its volume changed. Invoked on any thread.
         */
        void onDeviceStateChanged(String deviceId);
    }

    private static final class Connection {
        final CastDeviceControllerImpl controller;
        volatile double volume = -1;

        Connection(CastDeviceControllerImpl controller) {
            this.controller = controller;
        }
    }

    private static final CastDeviceRegistry INSTANCE = new CastDeviceRegistry();

    private final Map<String, CastDevice> discovered = new ConcurrentHashMap<>();
    private final Map<String, Connection> connections = new ConcurrentHashMap<>();
    // Weak, as the route provider that listens is recreated whenever its service is
    private volatile WeakReference<Listener> listener = new WeakReference<>(null);

    private CastDeviceRegistry() {
    }

    static CastDeviceRegistry getInstance() {
        return INSTANCE;
    }

    void setListener(Listener listener) {
        this.listener = new WeakReference<>(listener);
    }

    private void notifyListener(String deviceId) {
        Listener listener = this.listener.get();
        if (listener != null) listener.onDeviceStateChanged(deviceId);
    }

    // region Devices found by discovery

    void addDiscovered(CastDevice device) {
        if (device != null && device.getDeviceId() != null) this.discovered.put(device.getDeviceId(), device);
    }

    void removeDiscovered(String deviceId) {
        if (deviceId != null) this.discovered.remove(deviceId);
    }

    CastDevice getDiscovered(String deviceId) {
        return deviceId == null ? null : this.discovered.get(deviceId);
    }

    Collection<CastDevice> getDiscovered() {
        return new ArrayList<>(this.discovered.values());
    }

    /**
     * Clients tell us which device to connect to as part of their request. Without a check, any app could make us
     * connect to an address and port of its choice, including apps that may not use the network themselves.
     *
     * @return whether the given device was found by discovery, at the same address and port
     */
    boolean isDiscovered(CastDevice device) {
        if (device == null || device.getAddress() == null) return false;
        CastDevice known = getDiscovered(device.getDeviceId());
        return known != null && device.getAddress().equals(known.getAddress()) && device.getServicePort() == known.getServicePort();
    }

    // endregion

    // region Devices a client is connected to

    void setConnected(String deviceId, CastDeviceControllerImpl controller) {
        if (deviceId == null) return;
        Connection previous = this.connections.get(deviceId);
        if (previous != null && previous.controller == controller) return;
        this.connections.put(deviceId, new Connection(controller));
        notifyListener(deviceId);
    }

    void setDisconnected(String deviceId, CastDeviceControllerImpl controller) {
        if (deviceId == null) return;
        Connection current = this.connections.get(deviceId);
        // Another client might have connected to the device since
        if (current == null || current.controller != controller) return;
        this.connections.remove(deviceId);
        notifyListener(deviceId);
    }

    /**
     * @return the controller of a client that is connected to the device, or null if there is none
     */
    CastDeviceControllerImpl getController(String deviceId) {
        Connection connection = deviceId == null ? null : this.connections.get(deviceId);
        return connection == null ? null : connection.controller;
    }

    boolean isConnected(String deviceId) {
        return getController(deviceId) != null;
    }

    void setVolume(String deviceId, CastDeviceControllerImpl controller, double volume) {
        Connection connection = deviceId == null ? null : this.connections.get(deviceId);
        if (connection == null || connection.controller != controller || connection.volume == volume) return;
        connection.volume = volume;
        notifyListener(deviceId);
    }

    /**
     * @return volume of the device from 0 to 1, or a negative value if it is not known, which is the case unless a
     * client is connected to it
     */
    double getVolume(String deviceId) {
        Connection connection = deviceId == null ? null : this.connections.get(deviceId);
        return connection == null ? -1 : connection.volume;
    }

    // endregion
}
