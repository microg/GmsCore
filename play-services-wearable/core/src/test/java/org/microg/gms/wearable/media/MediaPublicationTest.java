/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.wearable.media;

import org.junit.Test;
import static org.junit.Assert.*;

public class MediaPublicationTest {
    @Test public void retainsLatestOnly() {
        MediaPublication queue = new MediaPublication();
        Object owner = new Object();
        assertTrue(queue.offer(owner, new byte[]{1}));
        assertFalse(queue.offer(owner, new byte[]{2}));
        assertArrayEquals(new byte[]{2}, queue.take(owner));
        assertNull(queue.take(owner));
        assertTrue(queue.offer(owner, new byte[]{3}));
    }

    @Test public void oldHandlerCannotConsumeNewServiceSnapshot() {
        MediaPublication queue = new MediaPublication();
        Object old = new Object(), current = new Object();
        queue.offer(old, new byte[]{1});
        assertTrue(queue.offer(current, new byte[]{2}));
        assertNull(queue.take(old));
        queue.cancel(old);
        assertArrayEquals(new byte[]{2}, queue.take(current));
    }

    @Test public void cancellationAndRevocationReleasePendingSlot() {
        MediaPublication queue = new MediaPublication();
        Object owner = new Object();
        queue.offer(owner, new byte[]{1});
        queue.cancel(owner);
        assertNull(queue.take(owner));
        assertTrue(queue.offer(owner, new byte[]{2}));
        queue.clear();
        assertNull(queue.take(owner));
    }

    @Test(expected = IllegalArgumentException.class) public void boundsRetainedState() {
        new MediaPublication().offer(new Object(), new byte[32769]);
    }
}
