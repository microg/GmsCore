/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.cast;

import android.net.Uri;

import com.google.android.gms.common.images.WebImage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.net.InetAddress;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class CastDeviceIconSelectionTest {

    @Test
    public void emptyDeviceHasNoBestFitIcon() throws Exception {
        assertNull(device().getIcon(96, 96));
    }

    @Test
    public void bestFitAvoidsUpscalingAndUsesClosestDimensions() throws Exception {
        CastDevice device = device();
        WebImage small = icon("small", 64, 64);
        WebImage square = icon("square", 128, 128);
        WebImage wide = icon("wide", 160, 100);
        device.getIcons().add(small);
        device.getIcons().add(wide);
        device.getIcons().add(square);

        assertSame(square, device.getIcon(96, 96));
        assertSame(square, device.getIcon(128, 128));
    }

    @Test
    public void closestKnownSizeWinsWhenEveryIconNeedsUpscaling() throws Exception {
        CastDevice device = device();
        WebImage square = icon("square", 64, 64);
        WebImage wide = icon("wide", 120, 40);
        device.getIcons().add(wide);
        device.getIcons().add(square);

        assertSame(square, device.getIcon(96, 96));
    }

    @Test
    public void unspecifiedSizeIsAStableFallback() throws Exception {
        CastDevice device = device();
        WebImage unspecified = new WebImage(Uri.parse("https://example.test/unspecified.png"));
        device.getIcons().add(unspecified);

        assertSame(unspecified, device.getIcon(96, 96));
        assertSame(unspecified, device.getIcon(0, 0));
    }

    private static CastDevice device() throws Exception {
        return new CastDevice(
                "device-id",
                "receiver.local",
                InetAddress.getByName("192.0.2.1"),
                8009,
                "1.0",
                "Living Room",
                "Model",
                null,
                0,
                CastDevice.CAPABILITY_VIDEO_OUT);
    }

    private static WebImage icon(String name, int width, int height) {
        return new WebImage(Uri.parse("https://example.test/" + name + ".png"), width, height);
    }
}
