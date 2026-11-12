/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.cast.framework.internal;

import android.os.IInterface;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.ObjectWrapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The listeners a client registered for one kind of event.
 * <p>
 * The client library wraps a listener in a new binder object for every call that adds or removes it, so the binder can
 * not be used to recognise a listener that is to be removed. The listener object it wraps can, and as the module runs
 * in the process of the client we can get hold of it.
 */
final class ListenerRegistry<T extends IInterface> {
    interface WrappedObject<T> {
        IObjectWrapper of(T listener) throws RemoteException;
    }

    interface Call<T> {
        void call(T listener) throws RemoteException;
    }

    private final String tag;
    private final WrappedObject<T> wrappedObject;
    private final Map<Object, T> listeners = new LinkedHashMap<>();

    ListenerRegistry(String tag, WrappedObject<T> wrappedObject) {
        this.tag = tag;
        this.wrappedObject = wrappedObject;
    }

    private Object keyOf(T listener) {
        try {
            Object wrapped = ObjectWrapper.unwrap(this.wrappedObject.of(listener));
            if (wrapped != null) return wrapped;
        } catch (Exception e) {
            // Fall through
        }
        return listener.asBinder();
    }

    void add(T listener) {
        if (listener == null) return;
        synchronized (this.listeners) {
            this.listeners.put(keyOf(listener), listener);
        }
    }

    void remove(T listener) {
        if (listener == null) return;
        synchronized (this.listeners) {
            this.listeners.remove(keyOf(listener));
        }
    }

    int size() {
        synchronized (this.listeners) {
            return this.listeners.size();
        }
    }

    /**
     * Calls every listener. One that fails does not keep the others from being called.
     *
     * @param what name of the event, for the log
     */
    void forEach(String what, Call<T> call) {
        List<T> current;
        synchronized (this.listeners) {
            current = new ArrayList<>(this.listeners.values());
        }
        for (T listener : current) {
            try {
                call.call(listener);
            } catch (Exception e) {
                Log.w(this.tag, "Error calling " + what, e);
            }
        }
    }
}
