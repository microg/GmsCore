/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CastDiscoveryStateTest {
    @Test
    public void retiredListenerCannotAddOrLoseCurrentService() {
        CastDiscoveryState<String> state = new CastDiscoveryState<>();
        Object retired = new Object();
        Object current = new Object();
        state.start(retired);
        state.found(retired, "receiver", "old-address");
        state.stop();
        state.start(current);
        CastDiscoveryState.Service<String> service = state.found(current, "receiver", "new-address");

        assertNull(state.found(retired, "receiver", "old-address"));
        assertFalse(state.lost(retired, "receiver"));
        assertTrue(state.isCurrent(service));
    }

    @Test
    public void lostServiceCannotBePublishedByInFlightResolve() {
        CastDiscoveryState<String> state = new CastDiscoveryState<>();
        Object listener = new Object();
        state.start(listener);
        CastDiscoveryState.Service<String> resolving = state.found(listener, "receiver", "address");

        assertTrue(state.lost(listener, "receiver"));
        assertFalse(state.isCurrent(resolving));
    }

    @Test
    public void rediscoveryDoesNotReviveAnOlderResolve() {
        CastDiscoveryState<Object> state = new CastDiscoveryState<>();
        Object listener = new Object();
        Object info = new Object();
        state.start(listener);
        CastDiscoveryState.Service<Object> previous = state.found(listener, "receiver", info);
        state.lost(listener, "receiver");
        CastDiscoveryState.Service<Object> current = state.found(listener, "receiver", info);

        assertFalse(state.isCurrent(previous));
        assertTrue(state.isCurrent(current));
    }

    @Test
    public void newerAnnouncementSupersedesOnlyThatServicesResolve() {
        CastDiscoveryState<String> state = new CastDiscoveryState<>();
        Object listener = new Object();
        state.start(listener);
        CastDiscoveryState.Service<String> old = state.found(listener, "one", "old-address");
        CastDiscoveryState.Service<String> other = state.found(listener, "two", "other-address");
        CastDiscoveryState.Service<String> current = state.found(listener, "one", "new-address");

        assertFalse(state.isCurrent(old));
        assertTrue(state.isCurrent(current));
        assertTrue(state.isCurrent(other));
    }

    @Test
    public void endingDiscoveryInvalidatesResolvesAndLateAnnouncements() {
        CastDiscoveryState<String> state = new CastDiscoveryState<>();
        Object listener = new Object();
        state.start(listener);
        CastDiscoveryState.Service<String> resolving = state.found(listener, "receiver", "address");
        state.stop();

        assertFalse(state.isCurrent(resolving));
        assertNull(state.found(listener, "receiver", "address"));
        assertFalse(state.lost(listener, "receiver"));
    }
}
