/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast;

import android.net.nsd.NsdServiceInfo;
import androidx.mediarouter.media.MediaRouteDescriptor;
import com.google.android.gms.cast.CastDevice;
import java.lang.reflect.Method;
import java.net.InetAddress;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Loopback/software receiver identity. Keep hardware cases in {@link CastHardwareReceiverTest}. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class CastLoopbackReceiverTest {
    private CastMediaRouteProvider provider;
    private Method resolved;

    @Before
    public void setUp() throws Exception {
        provider = new CastMediaRouteProvider(RuntimeEnvironment.getApplication());
        resolved = CastMediaRouteProvider.class.getDeclaredMethod("onServiceResolvedInternal", NsdServiceInfo.class);
        resolved.setAccessible(true);
    }

    @Test
    public void loopbackAddressIsSoftwareReceiverNotLanHardware() throws Exception {
        CastDevice device = new CastDevice(
                "loopback-id", "loopback-service", InetAddress.getByName("127.0.0.1"),
                8009, "1", "Local software receiver", "Test receiver", null, 0,
                CastDevice.CAPABILITY_AUDIO_OUT);
        assertEquals(CastReceiverKind.Kind.LOOPBACK, CastReceiverKind.of(device));
        assertTrue(CastReceiverKind.isLoopback(device));
        assertFalse(CastReceiverKind.isHardware(device));
        assertTrue(device.getInetAddress().isLoopbackAddress());
        assertFalse(device.isOnLocalNetwork());
    }

    @Test
    public void unspecifiedAddressIsSoftwareReceiver() throws Exception {
        CastDevice device = new CastDevice(
                "any-id", "any-service", InetAddress.getByName("0.0.0.0"),
                8009, "1", "Unspecified receiver", "Test receiver", null, 0,
                CastDevice.CAPABILITY_AUDIO_OUT);
        assertEquals(CastReceiverKind.Kind.LOOPBACK, CastReceiverKind.of(device));
        assertFalse(device.isOnLocalNetwork());
    }

    @Test
    public void discoveredLoopbackRouteStaysLoopbackInExtras() throws Exception {
        NsdServiceInfo service = new NsdServiceInfo();
        service.setServiceName("loopback-service");
        service.setServiceType("_googlecast._tcp.");
        service.setHost(InetAddress.getByName("127.0.0.1"));
        service.setPort(8009);
        service.setAttribute("id", "loopback-route");
        service.setAttribute("fn", "Software receiver");
        service.setAttribute("md", "Test receiver");
        resolved.invoke(provider, service);

        MediaRouteDescriptor route = provider.getDescriptor().getRoutes().get(0);
        CastDevice device = CastDevice.getFromBundle(route.getExtras());
        assertNotNull(device);
        assertEquals("loopback-route", device.getDeviceId());
        assertEquals(CastReceiverKind.Kind.LOOPBACK, CastReceiverKind.of(device));
        assertTrue(device.getInetAddress().isLoopbackAddress());
        assertFalse(device.isOnLocalNetwork());
    }
}
