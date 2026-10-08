package org.microg.gms.wearable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WearableLogStore {
    public static final WearableLogStore INSTANCE = new WearableLogStore();

    private static final int MAX_NAMES = 256;

    private final Map<String, Long> counters = bounded();
    private final Map<String, Long> timers = bounded();
    private long eventCount;
    private long eventBytes;

    private WearableLogStore() {}

    private static Map<String, Long> bounded() {
        return new LinkedHashMap<String, Long>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Entry<String, Long> eldest) {
                return size() > MAX_NAMES;
            }
        };
    }

    public synchronized void counter(String name, long value, boolean increment) {
        Long current = counters.get(name);
        counters.put(name, increment && current != null ? current + value : value);
    }

    public synchronized void event(int dataLength) {
        eventCount++;
        eventBytes += dataLength;
    }

    public synchronized void timer(String name, long timestamp) {
        timers.put(name, timestamp);
    }

    public synchronized void clear() {
        counters.clear();
        timers.clear();
        eventCount = 0;
        eventBytes = 0;
    }

    public synchronized Map<String, Long> getCounters() {
        return new HashMap<>(counters);
    }

    public synchronized long getEventCount() {
        return eventCount;
    }
}