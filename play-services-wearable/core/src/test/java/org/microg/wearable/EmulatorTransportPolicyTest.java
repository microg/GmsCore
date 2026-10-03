/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.wearable;

import org.junit.Test;

import static org.junit.Assert.*;

public class EmulatorTransportPolicyTest {
    @Test public void permitsADebugBuildOnAModernEmulator() {
        assertTrue(EmulatorTransportPolicy.isAllowed(true, "1", ""));
        assertTrue(EmulatorTransportPolicy.isAllowed(true, "1", null));
    }

    @Test public void permitsALegacyEmulatorOnlyWhenModernPropertyIsAbsent() {
        assertTrue(EmulatorTransportPolicy.isAllowed(true, "", "1"));
    }

    @Test public void rejectsAReleaseBuildEvenOnAnEmulator() {
        assertFalse(EmulatorTransportPolicy.isAllowed(false, "1", "1"));
        assertFalse(EmulatorTransportPolicy.isAllowed(false, "", "1"));
    }

    @Test public void rejectsPhysicalDeviceAndUnknownSignals() {
        assertFalse(EmulatorTransportPolicy.isAllowed(true, "0", "0"));
        assertFalse(EmulatorTransportPolicy.isAllowed(true, "", ""));
        assertFalse(EmulatorTransportPolicy.isAllowed(true, "", null));
    }

    @Test public void rejectsAnUnreadableModernProperty() {
        assertFalse(EmulatorTransportPolicy.isAllowed(true, null, "1"));
        assertFalse(EmulatorTransportPolicy.isAllowed(true, null, null));
    }

    @Test public void modernDenialCannotBeOverriddenByLegacyProperty() {
        assertFalse(EmulatorTransportPolicy.isAllowed(true, "0", "1"));
        assertFalse(EmulatorTransportPolicy.isAllowed(true, "false", "1"));
    }

    @Test public void rejectsMalformedSignalsWithoutBuildHeuristics() {
        for (String value : new String[]{"true", "yes", " 1", "1 ", "01", "emulator", "ranchu"}) {
            assertFalse(EmulatorTransportPolicy.isAllowed(true, value, "1"));
            assertFalse(EmulatorTransportPolicy.isAllowed(true, "", value));
        }
    }
}
