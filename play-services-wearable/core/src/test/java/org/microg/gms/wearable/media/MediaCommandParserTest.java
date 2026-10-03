/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable.media;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class MediaCommandParserTest {
    @Test public void parsesCapturedCommands() {
        assertEquals(MediaCommandParser.Command.PAUSE, MediaCommandParser.parse(hex("0a160a07636f6d6d616e64120b0802120712057061757365")));
        assertEquals(MediaCommandParser.Command.PLAY, MediaCommandParser.parse(hex("0a150a07636f6d6d616e64120a080212061204706c6179")));
        assertEquals(MediaCommandParser.Command.NEXT, MediaCommandParser.parse(hex("0a150a07636f6d6d616e64120a0802120612046e657874")));
    }

    @Test public void rejectsUnknownOrMalformedCommands() {
        assertNull(MediaCommandParser.parse(null));
        assertNull(MediaCommandParser.parse(new byte[]{1, 2, 3}));
        assertNull(MediaCommandParser.parse(hex("0a150a07636f6d6d616e64120a08021206120468616c74")));
    }

    @Test public void parsesPrevious() {
        assertEquals(MediaCommandParser.Command.PREVIOUS,
                MediaCommandParser.parse(hex("0a190a07636f6d6d616e64120e0802120a120870726576696f7573")));
    }

    @Test public void boundsCommandPayload() {
        assertNull(MediaCommandParser.parse(new byte[0]));
        assertNull(MediaCommandParser.parse(new byte[257]));
    }

    @Test public void rejectsDuplicateCommands() {
        String play = "0a150a07636f6d6d616e64120a080212061204706c6179";
        assertNull(MediaCommandParser.parse(hex(play + play)));
    }

    private static byte[] hex(String value) {
        byte[] result = new byte[value.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }
}
