/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AuthManagerIidTokenTest {

    @Test
    fun requireIidToken_returnsTokenWhenPresent() {
        assertEquals("iid-token", requireIidToken("iid-token"))
    }

    @Test
    fun requireIidToken_rejectsNull() {
        assertThrows(IllegalStateException::class.java) {
            requireIidToken(null)
        }
    }

    @Test
    fun requireIidToken_rejectsEmptyString() {
        val thrown = assertThrows(IllegalStateException::class.java) {
            requireIidToken("")
        }
        assertEquals("IID token is empty", thrown.message)
    }
}
