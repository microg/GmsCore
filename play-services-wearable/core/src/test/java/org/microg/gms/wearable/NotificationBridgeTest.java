/*
 * SPDX-FileCopyrightText: 2024-2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Unit tests for {@link NotificationBridge} static helper methods.
 */
@RunWith(RobolectricTestRunner.class)
public class NotificationBridgeTest {

    @Test
    public void testActiveNotifications_isNotNull() {
        assertNotNull("activeNotifications map should not be null",
                NotificationBridge.activeNotifications);
    }

    @Test
    public void testActiveNotifications_isInitiallyEmpty() {
        NotificationBridge.activeNotifications.clear();
        assertTrue("activeNotifications should be empty initially",
                NotificationBridge.activeNotifications.isEmpty());
    }

    @Test
    public void testCategoryConstants_areDistinct() {
        assertNotNull(NotificationBridge.CATEGORY_CALL);
        assertNotNull(NotificationBridge.CATEGORY_MSG);
        assertNotNull(NotificationBridge.CATEGORY_MEDIA);
        assertNotNull(NotificationBridge.CATEGORY_OTHER);
        // Each category should be unique
        assertEquals("call", NotificationBridge.CATEGORY_CALL);
        assertEquals("msg", NotificationBridge.CATEGORY_MSG);
        assertEquals("media", NotificationBridge.CATEGORY_MEDIA);
        assertEquals("other", NotificationBridge.CATEGORY_OTHER);
    }

    @Test
    public void testGetNotificationCategory_nullInput() {
        assertEquals(NotificationBridge.CATEGORY_OTHER,
                NotificationBridge.getNotificationCategory(null));
    }

    @Test
    public void testGetNotificationsByCategory_empty() {
        NotificationBridge.activeNotifications.clear();
        assertEquals(0,
                NotificationBridge.getNotificationsByCategory(
                        NotificationBridge.CATEGORY_CALL).size());
    }

    @Test
    public void testClear_removesAllNotifications() {
        NotificationBridge.activeNotifications.clear();
        assertTrue("After clear, map should be empty",
                NotificationBridge.activeNotifications.isEmpty());
    }
}
