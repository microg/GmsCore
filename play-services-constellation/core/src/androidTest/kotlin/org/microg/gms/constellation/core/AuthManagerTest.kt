/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AuthManagerTest {
    @Test
    fun iidFailureIsNotConvertedToEmptyToken() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val authManager = AuthManager.get(instrumentation.targetContext)
        var failure: Throwable? = null

        instrumentation.runOnMainSync {
            try {
                authManager.getIidToken("test-sender")
            } catch (throwable: Throwable) {
                failure = throwable
            }
        }

        assertNotNull("InstanceID MAIN_THREAD failure was suppressed", failure)
        assertTrue("Expected IOException, got ${failure?.javaClass?.name}", failure is IOException)
    }
}
