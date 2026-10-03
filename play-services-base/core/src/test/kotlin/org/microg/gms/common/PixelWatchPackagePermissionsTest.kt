/*
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelWatchPackagePermissionsTest {
    private val certificate = "48ed0058d1b6638e39a6e4c2df1c4d5fdf593f696bf31b09a88f93141eaf600f"

    @Test fun officialCompanionGetsAccountDiscoveryOnly() {
        assertEquals(setOf(GooglePackagePermission.ACCOUNT), getGooglePackagePermissions(
            PackageAndCertHash("com.google.android.apps.wear.companion", "SHA-256", certificate)))
    }

    @Test fun anotherPackageCannotUseCompanionCertificateEntry() {
        assertTrue(getGooglePackagePermissions(
            PackageAndCertHash("example.companion", "SHA-256", certificate)).isEmpty())
    }

    @Test fun companionWithAnotherCertificateHasNoSpecialAccess() {
        assertTrue(getGooglePackagePermissions(
            PackageAndCertHash("com.google.android.apps.wear.companion", "SHA-256", "0".repeat(64))).isEmpty())
    }
}
