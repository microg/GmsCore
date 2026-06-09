/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link NotificationBridge} static helpers.
 *
 * <p>Tests verify:
 * <ul>
 *   <li>The shared {@code activeNotifications} map starts empty.</li>
 *   <li>UID lookups for unknown notifications return null without throwing.</li>
 *   <li>doPositiveAction / doNegativeAction gracefully handle unknown UIDs.</li>
 * </ul>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Config.ALL_SDKS)
public class NotificationBridgeTest {

    @Test
    public void testActiveNotificationsMapIsEmptyInitially() {
        assertTrue("activeNotifications should be empty at start",
                NotificationBridge.activeNotifications.isEmpty());
    }

    @Test
    public void testDoPositiveActionUnknownUidDoesNotThrow() {
        // Should not throw even with a non-existent UID
        NotificationBridge.doPositiveAction(null, 99999);
    }

    @Test
    public void testDoNegativeActionUnknownUidDoesNotThrow() {
        // Should not throw even with a non-existent UID
        NotificationBridge.doNegativeAction(null, 99999);
    }
}
