/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import static org.junit.Assert.*;

import com.google.android.gms.wearable.Wearable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

/**
 * T4 / P1-B: client Channel API must be a real surface, not silent stubs.
 *
 * <p>{@code Wearable.ChannelApi} is asserted via reflection so a missing field
 * is a runtime failure instead of a compile error that would hide T1/T2.
 * After Slice 3 wires the field, this still asserts it is non-null.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class WearableChannelApiSurfaceTest {

    @Test
    public void testChannelImplGetInputStreamIsNotASilentStub() {
        ChannelImpl channel = new ChannelImpl("t", "n", "/p");
        assertNotNull(
                "ChannelImpl.getInputStream must not return null solely because the method is a stub",
                channel.getInputStream(null));
    }

    @Test
    public void testChannelImplGetOutputStreamIsNotASilentStub() {
        ChannelImpl channel = new ChannelImpl("t", "n", "/p");
        assertNotNull(
                "ChannelImpl.getOutputStream must not return null solely because the method is a stub",
                channel.getOutputStream(null));
    }

    @Test
    public void testChannelImplCloseIsNotASilentStub() {
        ChannelImpl channel = new ChannelImpl("t", "n", "/p");
        assertNotNull(
                "ChannelImpl.close must not return null solely because the method is a stub",
                channel.close(null));
    }

    @Test
    public void testWearableChannelApiFieldExists() throws Exception {
        Field field;
        try {
            field = Wearable.class.getField("ChannelApi");
        } catch (NoSuchFieldException e) {
            fail("Wearable.ChannelApi must be a public static field (client Channel API surface)");
            return;
        }
        assertNotNull("Wearable.ChannelApi must be non-null", field.get(null));
    }
}
