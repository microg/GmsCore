/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable.media;

import org.microg.gms.wearable.databundle.DataBundle;
import org.microg.gms.wearable.databundle.DataBundleEntry;

import java.io.IOException;

/** Parses the small Data Layer command sent by Wear OS media controls. */
public final class MediaCommandParser {
    private MediaCommandParser() {}

    public enum Command { PLAY, PAUSE, NEXT, PREVIOUS }

    public static Command parse(byte[] payload) {
        if (payload == null || payload.length == 0 || payload.length > 256) return null;
        final String value;
        try {
            String found = null;
            for (DataBundleEntry entry : DataBundle.ADAPTER.decode(payload).entries) {
                if (!"command".equals(entry.key) || entry.typedValue == null
                        || entry.typedValue.type == null || entry.typedValue.type != 2
                        || entry.typedValue.value == null) continue;
                if (found != null) return null;
                found = entry.typedValue.value.stringVal;
            }
            value = found;
        } catch (IOException | RuntimeException e) {
            return null;
        }
        if (value == null) return null;
        switch (value) {
            case "play": return Command.PLAY;
            case "pause": return Command.PAUSE;
            case "next": return Command.NEXT;
            case "previous": return Command.PREVIOUS;
            default: return null;
        }
    }
}
