/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Small in-memory ring buffer for RCS-related binder traces.
 *
 * <p>This is a diagnostic aid only: it records <em>metadata</em> about incoming binder
 * transactions (which service was bound, which transaction code was attempted, the
 * interface descriptor the caller announced, and the parcel size). It never records parcel
 * contents, so no phone numbers, tokens, IMSIs or other personal data end up in the trace.
 * Entries are visible through {@code dumpsys} (see {@link RcsService}) and logcat.
 *
 * <p>Pure Java on purpose, so it can be unit tested without Android.
 */
public class RcsTrace {
    /** Maximum number of entries kept; older entries are dropped. */
    public static final int MAX_ENTRIES = 100;

    /** One recorded binder interaction. */
    public static class Entry {
        /** {@link System#currentTimeMillis()} when the interaction was observed. */
        public final long timestampMs;
        /** GMS service name, e.g. {@code RCS} or {@code CONSTELLATION}. */
        public final String service;
        /** Package that bound the service, e.g. {@code com.google.android.apps.messaging}. */
        public final String callerPackage;
        /** Binder transaction code, or 0 for a service bind. */
        public final int code;
        /** Interface descriptor announced by the caller, or {@code "bind"}. */
        public final String descriptor;
        /** Size of the incoming parcel in bytes, or 0 for a service bind. */
        public final int dataSize;

        public Entry(long timestampMs, String service, String callerPackage, int code, String descriptor, int dataSize) {
            this.timestampMs = timestampMs;
            this.service = service;
            this.callerPackage = callerPackage;
            this.code = code;
            this.descriptor = descriptor;
            this.dataSize = dataSize;
        }

        @Override
        public String toString() {
            return timestampMs + " " + service + " caller=" + callerPackage
                    + " code=" + code + " descriptor=" + descriptor + " size=" + dataSize;
        }
    }

    private final Deque<Entry> entries = new ArrayDeque<>();

    /** Records one interaction, dropping the oldest entry when full. */
    public synchronized void record(String service, String callerPackage, int code, String descriptor, int dataSize) {
        if (entries.size() >= MAX_ENTRIES) {
            entries.removeFirst();
        }
        entries.addLast(new Entry(System.currentTimeMillis(), service, callerPackage, code, descriptor, dataSize));
    }

    /** Returns a copy of all recorded entries, oldest first. */
    public synchronized List<Entry> snapshot() {
        return new ArrayList<>(entries);
    }

    /** Returns the number of recorded entries. */
    public synchronized int size() {
        return entries.size();
    }

    /** Drops all recorded entries. */
    public synchronized void clear() {
        entries.clear();
    }
}
