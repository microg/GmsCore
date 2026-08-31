/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard

import org.junit.Assert.assertEquals
import org.junit.Test

class DgVmClassLoaderTest {
    @Test
    fun namedClassDoesNotLookAnonymous() {
        assertEquals("org.microg.gms.droidguard.DgVmClassLoader", DgVmClassLoader::class.java.name)
        assertEquals(false, DgVmClassLoader::class.java.isAnonymousClass)
    }
}
