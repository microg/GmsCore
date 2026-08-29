/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RcsStartIntentFilterTest {

    @Test
    public void rcsStartIsBoundToRcsServiceNotDummyService() throws Exception {
        Path manifest = Paths.get("src/main/AndroidManifest.xml");
        String xml = new String(Files.readAllBytes(manifest), StandardCharsets.UTF_8);

        assertTrue(xml.contains("org.microg.gms.rcs.RcsService"));

        int dummyStart = xml.indexOf("android:name=\"org.microg.gms.DummyService\"");
        assertTrue(dummyStart >= 0);
        int dummyEnd = xml.indexOf("</service>", dummyStart);
        String dummyBlock = xml.substring(dummyStart, dummyEnd);
        assertFalse(dummyBlock.contains("com.google.android.gms.rcs.START"));

        int rcsStart = xml.indexOf("android:name=\"org.microg.gms.rcs.RcsService\"");
        int rcsEnd = xml.indexOf("</service>", rcsStart);
        String rcsBlock = xml.substring(rcsStart, rcsEnd);
        assertTrue(rcsBlock.contains("com.google.android.gms.rcs.START"));
    }
}
