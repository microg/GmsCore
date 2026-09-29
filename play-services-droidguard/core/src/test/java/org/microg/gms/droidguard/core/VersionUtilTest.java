/*
 * SPDX-FileCopyrightText: 2026 Paul Phillips
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core;

import android.content.ContextWrapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class VersionUtilTest {
    private final VersionUtil versionUtil = new VersionUtil(new ContextWrapper(null));

    @Test
    public void acceptsFirstDpiEntry() {
        assertEquals(Integer.valueOf(0), versionUtil.getVersionOffset("000300"));
        assertEquals(Integer.valueOf(5), versionUtil.getVersionOffset("000700"));
    }

    @Test
    public void stillRejectsUnknownDpiEntry() {
        assertNull(versionUtil.getVersionOffset("000301"));
    }
}
