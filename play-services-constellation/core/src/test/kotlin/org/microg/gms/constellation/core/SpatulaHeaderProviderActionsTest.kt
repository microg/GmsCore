/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SpatulaHeaderProviderActionsTest {
    @Test
    fun exposesClassicAndAlternateAppCertActions() {
        assertEquals(
            "com.google.android.gms.auth.be.appcert.AppCertService",
            APP_CERT_SERVICE_ACTION
        )
        assertEquals("com.google.android.gms.auth.APP_CERT", APP_CERT_ACTION)
    }
}
