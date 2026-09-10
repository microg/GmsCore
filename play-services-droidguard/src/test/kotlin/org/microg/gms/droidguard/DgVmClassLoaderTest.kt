/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DgVmClassLoaderTest {

    @Test
    fun usesNamedClassInsteadOfAnonymousDexClassLoader() {
        assertEquals("DgVmClassLoader", DgVmClassLoader::class.java.simpleName)
        assertFalse(DgVmClassLoader::class.java.simpleName.contains("org.microg"))
    }
}
