/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable.bridge;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

/**
 * Parses watch→phone media control messages sent on paths under
 * {@link #PATH_PREFIX}.
 */
public final class MediaControlCommand {

    public static final String PATH_PREFIX = "/wearable/media/control/";
    public static final String STATE_PATH = "/wearable/media/state";

    public enum Action {
        PLAY,
        PAUSE,
        TOGGLE,
        NEXT,
        PREVIOUS,
        SEEK,
        RATE
    }

    public final Action action;
    public final long seekPositionMs;
    public final float playbackRate;

    public MediaControlCommand(Action action, long seekPositionMs, float playbackRate) {
        this.action = action;
        this.seekPositionMs = seekPositionMs;
        this.playbackRate = playbackRate;
    }

    public static boolean isControlPath(String path) {
        return path != null && path.startsWith(PATH_PREFIX);
    }

    public static MediaControlCommand parse(String path, byte[] data) {
        if (!isControlPath(path)) {
            return null;
        }
        String name = path.substring(PATH_PREFIX.length()).toLowerCase(Locale.US);
        switch (name) {
            case "play":
                return new MediaControlCommand(Action.PLAY, 0L, 1f);
            case "pause":
                return new MediaControlCommand(Action.PAUSE, 0L, 1f);
            case "toggle":
                return new MediaControlCommand(Action.TOGGLE, 0L, 1f);
            case "next":
                return new MediaControlCommand(Action.NEXT, 0L, 1f);
            case "previous":
                return new MediaControlCommand(Action.PREVIOUS, 0L, 1f);
            case "seek":
                return new MediaControlCommand(Action.SEEK, readLong(data), 1f);
            case "rate":
                return new MediaControlCommand(Action.RATE, 0L, readFloat(data));
            default:
                return null;
        }
    }

    private static long readLong(byte[] data) {
        if (data == null || data.length < 8) {
            return 0L;
        }
        return ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getLong();
    }

    private static float readFloat(byte[] data) {
        if (data == null || data.length < 4) {
            return 1f;
        }
        return ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getFloat();
    }
}
