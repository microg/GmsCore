/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.gcm

import org.junit.Assert.assertTrue
import org.junit.Test

class PushRegisterServiceTest {
    @Test
    fun unregisterRequestUsesDeleteSemantics() {
        assertTrue(newUnregisterRequest().delete)
    }
}
