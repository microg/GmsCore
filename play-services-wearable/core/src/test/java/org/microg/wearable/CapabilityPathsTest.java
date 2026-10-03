/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.wearable;

import org.junit.Test;
import static org.junit.Assert.*;

public class CapabilityPathsTest {
    private static final String PACKAGE = "com.example.app";
    private static final String SIGNATURE = "0123456789012345678901234567890123456789";

    @Test public void excludesLocalAndAbsentNodes() {
        assertFalse(CapabilityPaths.visibleNode("phone", "phone", true, false));
        assertFalse(CapabilityPaths.visibleNode("phone", "phone", true, true));
        assertFalse(CapabilityPaths.visibleNode(null, "phone", false, false));
        assertFalse(CapabilityPaths.visibleNode("", "phone", false, false));
    }

    @Test public void filtersDisconnectedRemoteNodes() {
        assertTrue(CapabilityPaths.visibleNode("watch", "phone", true, true));
        assertFalse(CapabilityPaths.visibleNode("watch", "phone", false, true));
        assertTrue(CapabilityPaths.visibleNode("watch", "phone", false, false));
    }

    @Test public void scopesToPackageAndCertificate() {
        String path = CapabilityPaths.path(PACKAGE, SIGNATURE, "mobile");
        assertEquals("mobile", CapabilityPaths.name(path, PACKAGE, SIGNATURE));
        assertNull(CapabilityPaths.name(path, "com.other.app", SIGNATURE));
        assertNull(CapabilityPaths.name(path, PACKAGE, "1123456789012345678901234567890123456789"));
    }

    @Test public void roundTripsReservedAndUnicodeNames() {
        for (String name : new String[]{"a/b", "a%b", "a b", "móvil", "雪", "play!*", "+", ".."}) {
            assertEquals(name, CapabilityPaths.name(CapabilityPaths.path(PACKAGE, SIGNATURE, name), PACKAGE, SIGNATURE));
        }
        assertTrue(CapabilityPaths.path(PACKAGE, SIGNATURE, "a/b").endsWith("a%2Fb"));
    }

    @Test public void rejectsMalformedAndAmbiguousPaths() {
        String prefix = CapabilityPaths.prefix(PACKAGE, SIGNATURE);
        for (String suffix : new String[]{"", "%", "%GG", "%C0%AF", "a/b", "%00", "%61", "%2f", "móvil"}) {
            assertNull(CapabilityPaths.name(prefix + suffix, PACKAGE, SIGNATURE));
        }
    }

    @Test public void boundsUtf8Names() {
        assertFalse(CapabilityPaths.validName(null));
        assertFalse(CapabilityPaths.validName(""));
        assertFalse(CapabilityPaths.validName("\uD800"));
        assertFalse(CapabilityPaths.validName(new String(new char[257]).replace('\0', 'a')));
        assertTrue(CapabilityPaths.validName(new String(new char[256]).replace('\0', 'a')));
        assertFalse(CapabilityPaths.validName(new String(new char[86]).replace('\0', '雪')));
    }

    @Test public void parsesOwnerAndCanonicalNameForRouting() {
        CapabilityPaths.Key key = CapabilityPaths.parse(CapabilityPaths.path(PACKAGE, SIGNATURE, "a/b 雪"));
        assertNotNull(key);
        assertEquals(PACKAGE, key.packageName);
        assertEquals(SIGNATURE, key.signature);
        assertEquals("a/b 雪", key.name);
    }

    @Test public void rejectsInvalidOwnerOrAmbiguousRoutingPath() {
        for (String path : new String[]{null, "", "/Capabilities/" + PACKAGE + "/" + SIGNATURE + "/a",
                "/capabilities/" + PACKAGE, "/capabilities//" + SIGNATURE + "/a",
                "/capabilities/com.example%2Fother/" + SIGNATURE + "/a",
                "/capabilities/" + PACKAGE + "/ABCDEF0123456789012345678901234567890123/a",
                "/capabilities/" + PACKAGE + "/" + SIGNATURE + "/a/b",
                "/capabilities/" + PACKAGE + "/" + SIGNATURE + "/%61"}) {
            assertNull(CapabilityPaths.parse(path));
        }
        String oversizedPackage = "com." + new String(new char[252]).replace('\0', 'a');
        assertNull(CapabilityPaths.parse("/capabilities/" + oversizedPackage + "/" + SIGNATURE + "/a"));
        assertNull(CapabilityPaths.parse("/capabilities/" + PACKAGE + "/" + SIGNATURE + "/"
                + new String(new char[769]).replace('\0', 'a')));
    }

    @Test(expected = IllegalArgumentException.class) public void rejectsPathInjectionInPackage() {
        CapabilityPaths.path("com.example/other", SIGNATURE, "mobile");
    }

    @Test(expected = IllegalArgumentException.class) public void rejectsInvalidCertificate() {
        CapabilityPaths.path(PACKAGE, "*", "mobile");
    }
}
