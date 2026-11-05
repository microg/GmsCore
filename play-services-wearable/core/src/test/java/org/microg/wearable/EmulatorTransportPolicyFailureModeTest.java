/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.wearable;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

/** Regression coverage for the fail-closed transport policy contract. */
public class EmulatorTransportPolicyFailureModeTest {
    @Test public void nullModernSignalCannotFallBackToLegacySignal() {
        assertFalse(EmulatorTransportPolicy.isAllowed(true, null, "1"));
    }

    @Test public void emptyModernSignalMayUseLegacySignal() {
        assertFalse(EmulatorTransportPolicy.isAllowed(true, "", "0"));
    }

    @Test public void releaseBuildIsAlwaysRejected() {
        assertFalse(EmulatorTransportPolicy.isAllowed(false, "1", "1"));
    }
}
