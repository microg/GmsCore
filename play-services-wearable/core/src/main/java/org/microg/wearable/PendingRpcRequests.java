/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.wearable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Bounded requests tied to the transport session and application that sent them. */
public final class PendingRpcRequests<T> {
    public static final int MAX_PENDING = 32;
    public static final int MAX_PENDING_PER_PACKAGE = 8;
    public static final int MAX_PAYLOAD_BYTES = 100 * 1024;
    public static final long TIMEOUT_MS = 60_000;

    public static final class Entry<T> {
        public final Object connection;
        public final String nodeId, packageName, signature, path;
        public volatile int requestId;
        public final T callback;
        public final int priority;
        private final long startedAt;
        private boolean active;

        private Entry(Object connection, String nodeId, String packageName, String signature,
                      String path, int requestId, T callback, long startedAt, boolean active, int priority) {
            this.connection = connection;
            this.nodeId = nodeId;
            this.packageName = packageName;
            this.signature = signature;
            this.path = path;
            this.requestId = requestId;
            this.callback = callback;
            this.priority = priority;
            this.startedAt = startedAt;
            this.active = active;
        }
    }

    private final List<Entry<T>> pending = new ArrayList<>();

    public synchronized Entry<T> add(Object connection, String nodeId, String packageName, String signature,
                                     String path, int requestId, T callback, long now) {
        return add(connection, nodeId, packageName, signature, path, requestId, callback, now, 0);
    }

    public synchronized Entry<T> add(Object connection, String nodeId, String packageName, String signature,
                                     String path, int requestId, T callback, long now, int priority) {
        return add(connection, nodeId, packageName, signature, path, requestId, callback, now, true, priority);
    }

    /** Reserves capacity before a caller is linked and its send is admitted to the transport queue. */
    public synchronized Entry<T> reserve(Object connection, String nodeId, String packageName, String signature,
                                         String path, T callback, long now) {
        return reserve(connection, nodeId, packageName, signature, path, callback, now, 0);
    }

    public synchronized Entry<T> reserve(Object connection, String nodeId, String packageName, String signature,
                                         String path, T callback, long now, int priority) {
        return add(connection, nodeId, packageName, signature, path, -1, callback, now, false, priority);
    }

    private Entry<T> add(Object connection, String nodeId, String packageName, String signature,
                         String path, int requestId, T callback, long now, boolean active, int priority) {
        if (priority < 0 || priority > 1) throw new IllegalArgumentException("Invalid RPC priority");
        if (connection == null || nodeId == null || packageName == null || signature == null
                || path == null || callback == null) throw new IllegalArgumentException("Incomplete RPC request");
        if (pending.size() >= MAX_PENDING) return null;
        int packageCount = 0;
        for (Entry<T> entry : pending) {
            if (entry.packageName.equals(packageName)) packageCount++;
            if (active && entry.active && entry.priority == priority
                    && entry.connection == connection && entry.requestId == requestId
                    && entry.packageName.equals(packageName) && entry.nodeId.equals(nodeId)
                    && entry.signature.equals(signature) && entry.path.equals(path)) return null;
        }
        if (packageCount >= MAX_PENDING_PER_PACKAGE) return null;
        Entry<T> entry = new Entry<>(connection, nodeId, packageName, signature, path, requestId, callback, now, active, priority);
        pending.add(entry);
        return entry;
    }

    public interface RequestIdSupplier { int get(); }

    /**
     * Commits a live reservation to sending. The supplier must only construct the envelope, not do IO.
     * Once this returns true, cancellation removes the reply wait but must not suppress the write:
     * the peer's ordered stream now expects this ID.
     */
    public synchronized boolean activate(Entry<T> entry, long now, RequestIdSupplier requestId) {
        if (!pending.contains(entry) || entry.active || now - entry.startedAt >= TIMEOUT_MS) return false;
        entry.requestId = requestId.get();
        entry.active = true;
        return true;
    }

    public synchronized Entry<T> takeReply(Object connection, String nodeId, String packageName,
                                           String signature, String path, int requestId, int payloadBytes, long now) {
        return takeReply(connection, nodeId, packageName, signature, path, requestId, payloadBytes, now, 0);
    }

    public synchronized Entry<T> takeReply(Object connection, String nodeId, String packageName,
                                           String signature, String path, int requestId, int payloadBytes,
                                           long now, int priority) {
        if (priority < 0 || priority > 1) return null;
        if (payloadBytes < 0 || payloadBytes > MAX_PAYLOAD_BYTES) return null;
        Iterator<Entry<T>> iterator = pending.iterator();
        while (iterator.hasNext()) {
            Entry<T> entry = iterator.next();
            if (entry.active && entry.priority == priority
                    && entry.connection == connection && entry.requestId == requestId
                    && entry.nodeId.equals(nodeId) && entry.packageName.equals(packageName)
                    && entry.signature.equals(signature) && entry.path.equals(path)
                    && now - entry.startedAt < TIMEOUT_MS) {
                iterator.remove();
                return entry;
            }
        }
        return null;
    }

    public synchronized boolean remove(Entry<T> entry) {
        return pending.remove(entry);
    }

    public synchronized boolean contains(Entry<T> entry) {
        return pending.contains(entry);
    }

    public synchronized List<Entry<T>> removeConnection(Object connection) {
        List<Entry<T>> removed = new ArrayList<>();
        Iterator<Entry<T>> iterator = pending.iterator();
        while (iterator.hasNext()) {
            Entry<T> entry = iterator.next();
            if (entry.connection == connection) {
                iterator.remove();
                removed.add(entry);
            }
        }
        return removed;
    }

    public synchronized List<Entry<T>> removeAll() {
        List<Entry<T>> removed = new ArrayList<>(pending);
        pending.clear();
        return removed;
    }

    public synchronized int size() {
        return pending.size();
    }
}
