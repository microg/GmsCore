/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.wearable;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class EmulatorTransportPolicyFailureModeTest2 {
    @Test public void malformedModernSignalDoesNotEnableTransport() {
        assertFalse(EmulatorTransportPolicy.isAllowed(true, "0", "1"));
        assertFalse(EmulatorTransportPolicy.isAllowed(true, "false", "1"));
    }
}
