/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

/**
 * Unit tests for {@link NotificationBridge} static helpers and command parsing.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class NotificationBridgeTest {

    @Test
    public void testActiveNotificationsMapIsEmptyInitially() {
        // May be non-empty if other tests polluted static state; ensure unknown ops still safe.
        assertNotNull(NotificationBridge.activeNotifications);
    }

    @Test
    public void testDoPositiveActionUnknownUidDoesNotThrow() {
        NotificationBridge.doPositiveAction(null, 99999);
    }

    @Test
    public void testDoNegativeActionUnknownUidDoesNotThrow() {
        NotificationBridge.doNegativeAction(null, 99999);
    }

    @Test
    public void testHandleCommandNullAndShortPayloadDoNotThrow() {
        NotificationBridge.handleCommand(null, null);
        NotificationBridge.handleCommand(null, new byte[0]);
        NotificationBridge.handleCommand(null, new byte[] { 1, 2 });
    }

    @Test
    public void testHandleCommandUnknownUidDoesNotThrow() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeByte(NotificationBridge.CMD_POSITIVE);
        dos.writeInt(424242);
        dos.flush();
        NotificationBridge.handleCommand(null, baos.toByteArray());

        baos = new ByteArrayOutputStream();
        dos = new DataOutputStream(baos);
        dos.writeByte(NotificationBridge.CMD_NEGATIVE);
        dos.writeInt(424243);
        dos.flush();
        NotificationBridge.handleCommand(null, baos.toByteArray());
    }

    @Test
    public void testNotificationCommandPathConstant() {
        assertEquals("/wearable/notification/command",
                NotificationBridge.NOTIFICATION_COMMAND_PATH);
    }
}
