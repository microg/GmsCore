/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import org.junit.Assert.*
import org.junit.Test

class SourceTransferWebPolicyTest {
    @Test fun exactGoogleHttpsOriginIsAllowed() {
        for (url in listOf("https://accounts.google.com/transfer?fixture=1", "https://accounts.google.com:443/TokenAuth"))
            assertTrue(SourceTransferWebPolicy.isNavigationAllowed(url))
    }
    @Test fun deceptiveHostsUserInfoPortsAndSchemesAreRejected() {
        for (url in listOf("http://accounts.google.com/", "https://accounts.google.com.evil.invalid/",
            "https://evilaccounts.google.com/", "https://accounts.google.com@evil.invalid/",
            "https://evil.invalid@accounts.google.com/", "https://accounts.google.com:444/",
            "https://accounts.google.com./", "intent://accounts.google.com/", "file:///etc/passwd",
            "https://accounts.google.com\\@evil.invalid/", "https://accounts.google.com/#sensitive"))
            assertFalse(url, SourceTransferWebPolicy.isNavigationAllowed(url))
    }
    @Test fun EmptyMalformedOversizedAndWhitespaceUrlsAreRejected() {
        for (url in listOf("", "https://[", "https://accounts.google.com/\n", "https://accounts.google.com/ " + "x".repeat(8200)))
            assertFalse(SourceTransferWebPolicy.isNavigationAllowed(url))
    }
    @Test fun StaticResourceHostsCannotBecomeNavigationDestinations() {
        assertTrue(SourceTransferWebPolicy.isResourceAllowed("https://www.gstatic.com/fixture.js"))
        assertFalse(SourceTransferWebPolicy.isNavigationAllowed("https://www.gstatic.com/fixture.js"))
        assertFalse(SourceTransferWebPolicy.isResourceAllowed("https://www.gstatic.com.evil.invalid/"))
    }
    @Test fun ExactGascCookiePreservesPaddingAndIgnoresOtherCookies() {
        assertEquals("synthetic==", SourceTransferWebPolicy.checkpoint("other=fixture; GASC=synthetic==; oauth_token=ignored"))
        assertNull(SourceTransferWebPolicy.checkpoint("oauth_token=synthetic; NOTGASC=synthetic"))
    }
    @Test fun MissingEmptyDuplicateOrUnsafeCookiesCannotComplete() {
        for (cookies in listOf(null, "", "GASC=", "GASC=one; GASC=two", "GASC=one\ntwo", "GASC=one,two"))
            assertNull(SourceTransferWebPolicy.checkpoint(cookies))
    }
    @Test fun CheckpointHasAResourceBound() {
        assertTrue(SourceTransferWebPolicy.isCheckpointValid("x".repeat(65536)))
        assertFalse(SourceTransferWebPolicy.isCheckpointValid("x".repeat(65537)))
    }
}
