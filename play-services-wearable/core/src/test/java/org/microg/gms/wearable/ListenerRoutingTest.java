/*
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.wearable;

import org.junit.Test;
import static org.junit.Assert.*;

public class ListenerRoutingTest {
    @Test public void targetsOnlyTheDestinationPackage() {
        assertTrue(WearableImpl.matchesTargetPackage("app.a", "app.a"));
        assertFalse(WearableImpl.matchesTargetPackage("app.a", "app.b"));
    }

    @Test public void nodeEventsRemainBroadcasts() {
        assertTrue(WearableImpl.matchesTargetPackage(null, "app.a"));
        assertTrue(WearableImpl.matchesTargetPackage(null, "app.b"));
    }
}
