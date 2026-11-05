/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.wearable;

import java.util.concurrent.atomic.AtomicInteger;

/** Bounds decoded requests awaiting dispatch, including eight bidirectional channels' ACK/data/control bursts. */
public final class ChannelRequestBudget {
    public static final int MAX_PENDING = 32;
    public static final int MAX_ENCODED_SIZE = ChannelFlow.MAX_PAYLOAD + 4096;
    private final AtomicInteger pending = new AtomicInteger();

    public void acquire(int encodedSize) {
        if (encodedSize < 0 || encodedSize > MAX_ENCODED_SIZE) {
            throw new IllegalArgumentException("Channel envelope exceeds size limit");
        }
        if (pending.incrementAndGet() > MAX_PENDING) {
            pending.decrementAndGet();
            throw new IllegalArgumentException("Too many pending channel requests");
        }
    }

    public void release() {
        if (pending.decrementAndGet() < 0) throw new IllegalStateException("Unbalanced channel admission");
    }
}
