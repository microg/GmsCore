/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast;

import java.util.HashMap;
import java.util.Map;

/** Main-thread discovery observations used to reject late NSD callbacks. */
final class CastDiscoveryState<T> {
    static final class Service<T> {
        final String name;
        final T info;

        Service(String name, T info) {
            this.name = name;
            this.info = info;
        }
    }

    private Object listener;
    private final Map<String, Service<T>> services = new HashMap<>();

    void start(Object listener) {
        this.listener = listener;
        services.clear();
    }

    void stop() {
        listener = null;
        services.clear();
    }

    Service<T> found(Object source, String name, T info) {
        if (listener == null || listener != source) return null;
        // A fresh observation has a fresh identity, even when NSD reuses its info object.
        Service<T> service = new Service<>(name, info);
        services.put(name, service);
        return service;
    }

    boolean lost(Object source, String name) {
        if (listener == null || listener != source) return false;
        services.remove(name);
        return true;
    }

    boolean isCurrent(Service<T> service) {
        return services.get(service.name) == service;
    }
}
