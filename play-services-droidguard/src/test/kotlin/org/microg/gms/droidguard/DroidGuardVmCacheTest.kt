/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard

import org.junit.Assert.assertEquals
import org.junit.Test

class DroidGuardVmCacheTest {
    @Test
    fun vmKeyUsesUppercaseHex() {
        assertEquals("DEADBEEF", DroidGuardVmCache.vmKey("deadbeef"))
        assertEquals("00FF", DroidGuardVmCache.vmKey("00ff"))
        assertEquals("ABC", DroidGuardVmCache.vmKey("ABC"))
    }
}
