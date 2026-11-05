/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.wearable.media;

/** Retains one latest snapshot and binds its scheduled delivery to its service instance. */
public final class MediaPublication {
    private Object owner;
    private byte[] state;

    public synchronized boolean offer(Object owner, byte[] state) {
        if (owner == null || state == null || state.length > 32768) throw new IllegalArgumentException();
        boolean schedule = this.owner != owner;
        this.owner = owner;
        this.state = state;
        return schedule;
    }

    public synchronized byte[] take(Object owner) {
        if (this.owner != owner) return null;
        byte[] result = state;
        clear();
        return result;
    }

    public synchronized void cancel(Object owner) {
        if (this.owner == owner) clear();
    }

    public synchronized void clear() {
        owner = null;
        state = null;
    }
}
