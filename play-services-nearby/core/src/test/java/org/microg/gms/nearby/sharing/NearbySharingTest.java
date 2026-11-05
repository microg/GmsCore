/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

/**
 * Companion regression tests for the Nearby Share implementation (issue #1272).
 *
 * <p>They exercise the pure Kotlin advertisement/identity engine that backs the
 * {@code com.google.android.gms.nearby.sharing} API surface.
 */
public class NearbySharingTest {
    private static final byte[] ACCOUNT_KEY = "microg-account".getBytes(StandardCharsets.UTF_8);

    private static byte[] targetId(int seed) {
        byte[] id = new byte[16];
        for (int i = 0; i < id.length; i++) {
            id[i] = (byte) (seed + i);
        }
        return id;
    }

    private static SharingAdvertisement advertisement(String name, int deviceType, byte[] targetId) {
        return new SharingAdvertisement(1, deviceType, name, targetId);
    }

    // SCENARIO_HAPPY_PATH: valid inputs round-trip through the wire codec.
    @Test
    public void happyPathRoundTrip() {
        SharingAdvertisement original = advertisement("Pixel 7", 1, targetId(0));
        byte[] payload = SharingAdvertisementKt.encodeSharingAdvertisement(original);
        assertEquals(4 + "Pixel 7".length() + 16, payload.length);
        assertEquals(0x4E, payload[0] & 0xFF);

        SharingAdvertisement decoded = SharingAdvertisementKt.decodeSharingAdvertisement(payload);
        assertEquals(original.getVersion(), decoded.getVersion());
        assertEquals(original.getDeviceType(), decoded.getDeviceType());
        assertEquals(original.getDeviceName(), decoded.getDeviceName());
        assertArrayEquals(original.getShareTargetId(), decoded.getShareTargetId());
        assertEquals(original, decoded);
        assertEquals(original.hashCode(), decoded.hashCode());
    }

    // SCENARIO_NULL_INPUT: null arguments are rejected with a typed validation error.
    @Test
    public void nullInputsAreRejected() {
        assertThrows(InvalidSharingAdvertisementException.class,
                () -> SharingAdvertisementKt.encodeSharingAdvertisement(null));
        assertThrows(InvalidSharingAdvertisementException.class,
                () -> SharingAdvertisementKt.decodeSharingAdvertisement(null));
        assertThrows(InvalidSharingAdvertisementException.class,
                () -> SharingAdvertisementKt.shareTargetId(null, ACCOUNT_KEY));
        assertThrows(InvalidSharingAdvertisementException.class,
                () -> SharingAdvertisementKt.shareTargetId("endpoint", null));
    }

    // SCENARIO_EMPTY_INPUT: empty strings/collections are rejected, never silently accepted.
    @Test
    public void emptyInputsAreRejected() {
        assertThrows(InvalidSharingAdvertisementException.class,
                () -> SharingAdvertisementKt.encodeSharingAdvertisement(advertisement("", 1, targetId(1))));
        assertThrows(InvalidSharingAdvertisementException.class,
                () -> SharingAdvertisementKt.shareTargetId("", ACCOUNT_KEY));
        assertThrows(InvalidSharingAdvertisementException.class,
                () -> SharingAdvertisementKt.shareTargetId("endpoint", new byte[0]));
        assertThrows(InvalidSharingAdvertisementException.class,
                () -> SharingAdvertisementKt.decodeSharingAdvertisement(new byte[0]));
    }

    // SCENARIO_BOUNDARY_LIMIT: the closed device-name interval [1, 64] is enforced strictly.
    @Test
    public void boundaryLimitsAreEnforced() {
        SharingAdvertisement lower = advertisement("a", 1, targetId(2));
        assertEquals(4 + 1 + 16, SharingAdvertisementKt.encodeSharingAdvertisement(lower).length);

        char[] maxName = new char[64];
        Arrays.fill(maxName, 'b');
        SharingAdvertisement upper = advertisement(new String(maxName), 1, targetId(3));
        assertEquals(4 + 64 + 16, SharingAdvertisementKt.encodeSharingAdvertisement(upper).length);

        char[] tooLong = new char[65];
        Arrays.fill(tooLong, 'c');
        assertThrows(InvalidSharingAdvertisementException.class,
                () -> SharingAdvertisementKt.encodeSharingAdvertisement(advertisement(new String(tooLong), 1, targetId(4))));

        assertThrows(InvalidSharingAdvertisementException.class,
                () -> SharingAdvertisementKt.encodeSharingAdvertisement(advertisement("ok", 1, new byte[15])));
        assertEquals(16, targetId(0).length);
    }

    // SCENARIO_NEGATIVE_FAILURE: malformed payloads fail immediately with the typed error.
    @Test
    public void malformedPayloadsAreRejected() {
        byte[] good = SharingAdvertisementKt.encodeSharingAdvertisement(advertisement("Phone", 1, targetId(5)));

        byte[] badMagic = good.clone();
        badMagic[0] = 0x00;
        assertThrows(InvalidSharingAdvertisementException.class,
                () -> SharingAdvertisementKt.decodeSharingAdvertisement(badMagic));

        byte[] badVersion = good.clone();
        badVersion[1] = 0x7F;
        assertThrows(InvalidSharingAdvertisementException.class,
                () -> SharingAdvertisementKt.decodeSharingAdvertisement(badVersion));

        byte[] badType = good.clone();
        badType[2] = 0x63;
        assertThrows(InvalidSharingAdvertisementException.class,
                () -> SharingAdvertisementKt.decodeSharingAdvertisement(badType));

        byte[] truncated = Arrays.copyOf(good, good.length - 1);
        assertThrows(InvalidSharingAdvertisementException.class,
                () -> SharingAdvertisementKt.decodeSharingAdvertisement(truncated));

        assertThrows(InvalidSharingAdvertisementException.class,
                () -> SharingAdvertisementKt.decodeSharingAdvertisement(new byte[3]));
    }

    // SCENARIO_CONCURRENCY_RACE: the stateless codec yields identical bytes under contention.
    @Test
    public void concurrentEncodingIsAtomic() throws Exception {
        final SharingAdvertisement advertisement = advertisement("Concurrent", 1, targetId(6));
        final byte[] expected = SharingAdvertisementKt.encodeSharingAdvertisement(advertisement);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Future<byte[]>> futures = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                futures.add(executor.submit(() -> SharingAdvertisementKt.encodeSharingAdvertisement(advertisement)));
            }
            for (Future<byte[]> future : futures) {
                assertArrayEquals(expected, future.get(30, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
        }
    }

    // SCENARIO_DEFECT_REGRESSION: the share-target identifier is deterministic and collision free.
    @Test
    public void shareTargetIdIsDeterministic() {
        byte[] first = SharingAdvertisementKt.shareTargetId("endpoint-a", ACCOUNT_KEY);
        byte[] second = SharingAdvertisementKt.shareTargetId("endpoint-a", ACCOUNT_KEY);
        assertEquals(16, first.length);
        assertArrayEquals(first, second);

        byte[] otherEndpoint = SharingAdvertisementKt.shareTargetId("endpoint-b", ACCOUNT_KEY);
        assertFalse(Arrays.equals(first, otherEndpoint));
        assertNotEquals(first, otherEndpoint);

        byte[] otherKey = SharingAdvertisementKt.shareTargetId("endpoint-a", "other-account".getBytes(StandardCharsets.UTF_8));
        assertFalse(Arrays.equals(first, otherKey));
    }
}
