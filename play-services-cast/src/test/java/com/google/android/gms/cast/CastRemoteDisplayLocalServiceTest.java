/*
 * Copyright (C) 2013-2026 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.cast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class CastRemoteDisplayLocalServiceTest {

    @Test
    public void testNotificationSettingsBuilder() {
        CastRemoteDisplayLocalService.NotificationSettings settings =
                new CastRemoteDisplayLocalService.NotificationSettings.Builder()
                        .setNotificationTitle("Test Cast")
                        .setNotificationText("Mirroring display to TV")
                        .build();

        assertNotNull(settings);
        assertEquals("Test Cast", settings.getNotificationTitle());
        assertEquals("Mirroring display to TV", settings.getNotificationText());
        assertNull(settings.getNotification());
        assertNull(settings.getPendingIntent());
    }

    @Test
    public void testInitialServiceState() {
        assertFalse(CastRemoteDisplayLocalService.isServiceRunning());
        assertNull(CastRemoteDisplayLocalService.getInstance());
    }
}
