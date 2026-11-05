/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.wearable;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class PendingRpcRequestsTest {
    private final PendingRpcRequests<String> requests = new PendingRpcRequests<>();
    private final Object connection = new Object();

    private PendingRpcRequests.Entry<String> add(String packageName, int requestId) {
        return requests.add(connection, "watch", packageName, "signature", "/path", requestId, "callback", 0);
    }

    private PendingRpcRequests.Entry<String> reply(Object session, String node, String app, String signature,
                                                  String path, int requestId, int size, long time) {
        return requests.takeReply(session, node, app, signature, path, requestId, size, time);
    }

    @Test public void consumesOnlyOneMatchingResponse() {
        PendingRpcRequests.Entry<String> entry = add("app", 7);
        assertSame(entry, reply(connection, "watch", "app", "signature", "/path", 7, 5, 1));
        assertNull(reply(connection, "watch", "app", "signature", "/path", 7, 5, 2));
        assertFalse(requests.remove(entry));
        assertEquals(0, requests.size());
    }

    @Test public void requiresApplicationPeerPathAndTransportIdentity() {
        PendingRpcRequests.Entry<String> entry = add("app", 7);
        assertNull(reply(new Object(), "watch", "app", "signature", "/path", 7, 1, 1));
        assertNull(reply(connection, "other-watch", "app", "signature", "/path", 7, 1, 1));
        assertNull(reply(connection, "watch", "other-app", "signature", "/path", 7, 1, 1));
        assertNull(reply(connection, "watch", "app", "other-signature", "/path", 7, 1, 1));
        assertNull(reply(connection, "watch", "app", "signature", "/other", 7, 1, 1));
        assertNull(reply(connection, "watch", "app", "signature", "/path", 8, 1, 1));
        assertEquals(1, requests.size());
        assertSame(entry, reply(connection, "watch", "app", "signature", "/path", 7, 1, 1));
    }

    @Test public void duplicateIdsCannotReplaceAnotherCallback() {
        PendingRpcRequests.Entry<String> entry = add("app", 7);
        assertNull(add("app", 7));
        assertNotNull(add("other-app", 7));
        assertSame(entry, reply(connection, "watch", "app", "signature", "/path", 7, 0, 1));
        assertEquals(1, requests.size());
    }

    @Test public void lateResponseCannotTurnTimeoutIntoSuccess() {
        PendingRpcRequests.Entry<String> entry = add("app", 7);
        assertNull(reply(connection, "watch", "app", "signature", "/path", 7, 1, PendingRpcRequests.TIMEOUT_MS));
        assertTrue(requests.remove(entry));
        assertNull(reply(connection, "watch", "app", "signature", "/path", 7, 1, PendingRpcRequests.TIMEOUT_MS + 1));
    }

    @Test public void independentPriorityStreamsCanUseSameIdAtDifferentPaths() {
        PendingRpcRequests.Entry<String> normal = add("app", 7);
        PendingRpcRequests.Entry<String> high = requests.add(connection, "watch", "app", "signature",
                "/s3/request", 7, "high-priority", 0);
        assertNotNull(high);
        assertSame(high, reply(connection, "watch", "app", "signature", "/s3/request", 7, 0, 1));
        assertSame(normal, reply(connection, "watch", "app", "signature", "/path", 7, 0, 1));
    }

    @Test public void oversizedReplyDoesNotConsumeTheLegitimateRequest() {
        PendingRpcRequests.Entry<String> entry = add("app", 7);
        assertNull(reply(connection, "watch", "app", "signature", "/path", 7,
                PendingRpcRequests.MAX_PAYLOAD_BYTES + 1, 1));
        assertSame(entry, reply(connection, "watch", "app", "signature", "/path", 7,
                PendingRpcRequests.MAX_PAYLOAD_BYTES, 1));
    }

    @Test public void cancellationCannotCancelLaterRequestReusingTheId() {
        PendingRpcRequests.Entry<String> cancelled = add("app", 7);
        assertTrue(requests.remove(cancelled));
        PendingRpcRequests.Entry<String> next = add("app", 7);
        assertFalse(requests.remove(cancelled));
        assertSame(next, reply(connection, "watch", "app", "signature", "/path", 7, 0, 1));
    }

    @Test public void disconnectCancelsOnlyThatTransportSession() {
        PendingRpcRequests.Entry<String> old = add("app", 7);
        Object replacement = new Object();
        PendingRpcRequests.Entry<String> fresh = requests.add(replacement, "watch", "app", "signature", "/path", 7, "new", 0);
        List<PendingRpcRequests.Entry<String>> removed = requests.removeConnection(connection);
        assertEquals(1, removed.size());
        assertSame(old, removed.get(0));
        assertNull(reply(connection, "watch", "app", "signature", "/path", 7, 0, 1));
        assertSame(fresh, reply(replacement, "watch", "app", "signature", "/path", 7, 0, 1));
    }

    @Test public void boundsTotalAndPerApplicationAdmission() {
        for (int i = 0; i < PendingRpcRequests.MAX_PENDING_PER_PACKAGE; i++) assertNotNull(add("app", i));
        assertNull(add("app", 100));
        for (int i = PendingRpcRequests.MAX_PENDING_PER_PACKAGE; i < PendingRpcRequests.MAX_PENDING; i++) {
            assertNotNull(add("app-" + i, i));
        }
        assertNull(add("new-app", 200));
        assertEquals(PendingRpcRequests.MAX_PENDING, requests.removeAll().size());
        assertEquals(0, requests.size());
        assertNotNull(add("app", 201));
    }

    private PendingRpcRequests.Entry<String> reserve(String app, long now) {
        return requests.reserve(connection, "watch", app, "signature", "/path", "callback", now);
    }

    @Test public void cancelledReservationDoesNotAllocateAnOrderedId() {
        AtomicInteger sequence = new AtomicInteger();
        PendingRpcRequests.Entry<String> cancelled = reserve("app", 0);
        assertEquals(-1, cancelled.requestId);
        assertTrue(requests.remove(cancelled));
        assertFalse(requests.activate(cancelled, 1, sequence::incrementAndGet));
        PendingRpcRequests.Entry<String> next = reserve("app", 1);
        assertTrue(requests.activate(next, 2, sequence::incrementAndGet));
        assertEquals(1, next.requestId);
        assertEquals(1, sequence.get());
    }

    @Test public void unassignedReservationCannotAcceptAReply() {
        AtomicInteger sequence = new AtomicInteger();
        PendingRpcRequests.Entry<String> entry = reserve("app", 0);
        assertNull(reply(connection, "watch", "app", "signature", "/path", -1, 0, 1));
        assertNull(reply(connection, "watch", "app", "signature", "/path", 1, 0, 1));
        assertTrue(requests.activate(entry, 1, sequence::incrementAndGet));
        assertFalse(requests.activate(entry, 1, sequence::incrementAndGet));
        assertEquals(1, sequence.get());
        assertSame(entry, reply(connection, "watch", "app", "signature", "/path", 1, 0, 2));
    }

    @Test public void expiredReservationDoesNotAllocateOrResetItsDeadline() {
        AtomicInteger sequence = new AtomicInteger();
        PendingRpcRequests.Entry<String> expired = reserve("app", 10);
        assertFalse(requests.activate(expired, 10 + PendingRpcRequests.TIMEOUT_MS, sequence::incrementAndGet));
        assertEquals(0, sequence.get());
        assertTrue(requests.remove(expired));
        PendingRpcRequests.Entry<String> next = reserve("app", 10);
        assertTrue(requests.activate(next, 10 + PendingRpcRequests.TIMEOUT_MS - 1, sequence::incrementAndGet));
        assertNull(reply(connection, "watch", "app", "signature", "/path", 1, 0,
                10 + PendingRpcRequests.TIMEOUT_MS));
    }

    @Test public void disconnectedAndStoppedReservationsCannotActivate() {
        AtomicInteger sequence = new AtomicInteger();
        PendingRpcRequests.Entry<String> disconnected = reserve("app", 0);
        assertSame(disconnected, requests.removeConnection(connection).get(0));
        assertFalse(requests.activate(disconnected, 1, sequence::incrementAndGet));
        PendingRpcRequests.Entry<String> stopped = reserve("app", 1);
        assertSame(stopped, requests.removeAll().get(0));
        assertFalse(requests.activate(stopped, 2, sequence::incrementAndGet));
        assertEquals(0, sequence.get());
    }

    @Test public void reservationsShareCapacityWithActiveRequestsWithoutFalseDuplicates() {
        AtomicInteger sequence = new AtomicInteger();
        for (int i = 0; i < PendingRpcRequests.MAX_PENDING_PER_PACKAGE; i++) {
            assertNotNull(reserve("app", 0));
        }
        assertNull(reserve("app", 0));
        assertNull(add("app", 100));
        for (int i = PendingRpcRequests.MAX_PENDING_PER_PACKAGE; i < PendingRpcRequests.MAX_PENDING; i++) {
            assertNotNull(reserve("app-" + i, 0));
        }
        assertNull(reserve("other", 0));
        List<PendingRpcRequests.Entry<String>> removed = requests.removeAll();
        assertEquals(PendingRpcRequests.MAX_PENDING, removed.size());
        for (PendingRpcRequests.Entry<String> entry : removed) {
            assertFalse(requests.activate(entry, 1, sequence::incrementAndGet));
        }
        assertEquals(0, sequence.get());
        PendingRpcRequests.Entry<String> next = reserve("app", 1);
        assertTrue(requests.activate(next, 1, sequence::incrementAndGet));
        assertSame(next, reply(connection, "watch", "app", "signature", "/path", 1, 0, 2));
    }
    @Test public void sameIdAndPathInDifferentPrioritiesKeepSeparateCallbacks() {
        PendingRpcRequests.Entry<String> normal = add("app", 7);
        PendingRpcRequests.Entry<String> high = requests.add(connection, "watch", "app", "signature",
                "/path", 7, "high", 0, 1);
        assertNotNull(high);
        assertNull(requests.add(connection, "watch", "app", "signature", "/path", 7, "duplicate", 0, 1));
        assertNull(requests.takeReply(connection, "watch", "app", "signature", "/path", 7, 0, 1, 2));
        assertSame(high, requests.takeReply(connection, "watch", "app", "signature", "/path", 7, 0, 1, 1));
        assertNull(requests.takeReply(connection, "watch", "app", "signature", "/path", 7, 0, 1, 1));
        assertSame(normal, reply(connection, "watch", "app", "signature", "/path", 7, 0, 1));
    }

    @Test public void reservedPrioritiesRetainIdentityAndCancellationIsolation() {
        PendingRpcRequests.Entry<String> high = requests.reserve(connection, "watch", "app", "signature",
                "/path", "high", 0, 1);
        PendingRpcRequests.Entry<String> normal = reserve("app", 0);
        assertTrue(requests.activate(high, 1, () -> 9));
        assertTrue(requests.activate(normal, 1, () -> 9));
        assertTrue(requests.remove(high));
        assertNull(requests.takeReply(connection, "watch", "app", "signature", "/path", 9, 0, 2, 1));
        assertSame(normal, reply(connection, "watch", "app", "signature", "/path", 9, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> requests.reserve(connection, "watch", "app",
                "signature", "/path", "invalid", 3, -1));
        assertEquals(0, requests.size());
    }

}
