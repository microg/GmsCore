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

/** Hardware Chromecast identity. Keep loopback cases in {@link CastLoopbackReceiverTest}. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class CastHardwareReceiverTest {
    private CastMediaRouteProvider provider;
    private Method resolved;

    @Before
    public void setUp() throws Exception {
        provider = new CastMediaRouteProvider(RuntimeEnvironment.getApplication());
        resolved = CastMediaRouteProvider.class.getDeclaredMethod("onServiceResolvedInternal", NsdServiceInfo.class);
        resolved.setAccessible(true);
    }

    @Test
    public void lanUnicastIsHardwareOnLocalNetwork() throws Exception {
        CastDevice device = new CastDevice(
                "chromecast-id", "Chromecast-living", InetAddress.getByName("192.168.1.40"),
                8009, "1", "Living room", "Chromecast", "/setup/icon.png", 0,
                CastDevice.CAPABILITY_VIDEO_OUT | CastDevice.CAPABILITY_AUDIO_OUT);
        assertEquals(CastReceiverKind.Kind.HARDWARE, CastReceiverKind.of(device));
        assertTrue(CastReceiverKind.isHardware(device));
        assertFalse(CastReceiverKind.isLoopback(device));
        assertFalse(device.getInetAddress().isLoopbackAddress());
        assertTrue(device.isOnLocalNetwork());
        assertEquals("192.168.1.40", device.getInetAddress().getHostAddress());
    }

    @Test
    public void nearbyPrefixIsNotLocalHardware() throws Exception {
        CastDevice device = new CastDevice(
                "__cast_nearby__guest", "nearby", InetAddress.getByName("192.168.1.40"),
                8009, "1", "Guest TV", "Chromecast", null, 0,
                CastDevice.CAPABILITY_VIDEO_OUT);
        assertEquals(CastReceiverKind.Kind.NEARBY, CastReceiverKind.of(device));
        assertFalse(device.isOnLocalNetwork());
        assertFalse(CastReceiverKind.isHardware(device));
        assertFalse(CastReceiverKind.isLoopback(device));
    }

    @Test
    public void multizoneCapabilityIsPreservedOnHardware() throws Exception {
        CastDevice device = new CastDevice(
                "group-id", "Speaker-group", InetAddress.getByName("192.168.1.41"),
                8009, "1", "Home group", "Google Home", null, 0,
                CastDevice.CAPABILITY_AUDIO_OUT | CastDevice.CAPABILITY_MULTIZONE_GROUP);
        assertTrue(device.hasCapability(CastDevice.CAPABILITY_MULTIZONE_GROUP));
        assertTrue(device.isOnLocalNetwork());
        assertEquals(CastReceiverKind.Kind.HARDWARE, CastReceiverKind.of(device));
    }

    @Test
    public void discoveredHardwareRouteStaysHardwareInExtras() throws Exception {
        NsdServiceInfo service = new NsdServiceInfo();
        service.setServiceName("Chromecast-living");
        service.setServiceType("_googlecast._tcp.");
        service.setHost(InetAddress.getByName("192.168.1.40"));
        service.setPort(8009);
        service.setAttribute("id", "chromecast-route");
        service.setAttribute("fn", "Living room");
        service.setAttribute("md", "Chromecast");
        service.setAttribute("ca", "5");
        resolved.invoke(provider, service);

        MediaRouteDescriptor route = provider.getDescriptor().getRoutes().get(0);
        CastDevice device = CastDevice.getFromBundle(route.getExtras());
        assertNotNull(device);
        assertEquals("chromecast-route", device.getDeviceId());
        assertEquals(CastReceiverKind.Kind.HARDWARE, CastReceiverKind.of(device));
        assertFalse(device.getInetAddress().isLoopbackAddress());
        assertTrue(device.isOnLocalNetwork());
        assertEquals("192.168.1.40", device.getAddress());
    }
}
