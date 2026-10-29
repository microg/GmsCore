/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.sharing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ShareTargetTest {

    @Test
    public void testShareTargetProperties() {
        ShareTarget target = new ShareTarget(12345L, "Pixel Phone", ShareTarget.TYPE_PHONE, "https://example.com/avatar.png", false);

        assertEquals(12345L, target.getId());
        assertEquals("Pixel Phone", target.getName());
        assertEquals(ShareTarget.TYPE_PHONE, target.getDeviceType());
        assertEquals("https://example.com/avatar.png", target.getImageUrl());
        assertFalse(target.isIncoming());
    }

    @Test
    public void testShareTargetEquality() {
        ShareTarget target1 = new ShareTarget(12345L, "Pixel Phone", ShareTarget.TYPE_PHONE, "https://example.com/avatar.png", false);
        ShareTarget target2 = new ShareTarget(12345L, "Pixel Phone", ShareTarget.TYPE_PHONE, "https://example.com/avatar.png", false);

        assertEquals(target1, target2);
        assertEquals(target1.hashCode(), target2.hashCode());
    }
}
