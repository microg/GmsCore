/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RcsTraceTest {
    @Test
    public void recordAndSnapshot() {
        RcsTrace trace = new RcsTrace();
        trace.record("RCS", "com.google.android.apps.messaging", 0, "bind", 0);
        trace.record("RCS", "com.google.android.apps.messaging", 3, "com.example.IRcs", 128);

        assertEquals(2, trace.size());
        List<RcsTrace.Entry> entries = trace.snapshot();
        assertEquals("RCS", entries.get(0).service);
        assertEquals("com.google.android.apps.messaging", entries.get(0).callerPackage);
        assertEquals(0, entries.get(0).code);
        assertEquals("bind", entries.get(0).descriptor);
        assertEquals(3, entries.get(1).code);
        assertEquals("com.example.IRcs", entries.get(1).descriptor);
        assertEquals(128, entries.get(1).dataSize);
        assertTrue(entries.get(0).timestampMs <= entries.get(1).timestampMs);
    }

    @Test
    public void capsAtMaxEntries() {
        RcsTrace trace = new RcsTrace();
        for (int i = 0; i < RcsTrace.MAX_ENTRIES + 50; i++) {
            trace.record("RCS", "caller", i, "descriptor", i);
        }

        assertEquals(RcsTrace.MAX_ENTRIES, trace.size());
        List<RcsTrace.Entry> entries = trace.snapshot();
        // The oldest 50 entries were dropped.
        assertEquals(50, entries.get(0).code);
        assertEquals(RcsTrace.MAX_ENTRIES + 49, entries.get(entries.size() - 1).code);
    }

    @Test
    public void clearDropsAllEntries() {
        RcsTrace trace = new RcsTrace();
        trace.record("RCS", "caller", 1, "descriptor", 10);
        trace.clear();

        assertEquals(0, trace.size());
        assertTrue(trace.snapshot().isEmpty());
    }

    @Test
    public void snapshotIsACopy() {
        RcsTrace trace = new RcsTrace();
        trace.record("RCS", "caller", 1, "descriptor", 10);
        List<RcsTrace.Entry> snapshot = trace.snapshot();
        snapshot.clear();

        assertEquals(1, trace.size());
    }
}
