/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast;

import androidx.mediarouter.media.MediaControlIntent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Additional remote-playback actions implemented by CastMediaRouteController. */
final class RemotePlaybackControlActions {
    private RemotePlaybackControlActions() {}

    static List<String> advertisedAdditionalActions() {
        return Collections.unmodifiableList(Arrays.asList(
                MediaControlIntent.ACTION_PAUSE,
                MediaControlIntent.ACTION_RESUME,
                MediaControlIntent.ACTION_STOP,
                MediaControlIntent.ACTION_SEEK,
                MediaControlIntent.ACTION_GET_STATUS,
                MediaControlIntent.ACTION_START_SESSION,
                MediaControlIntent.ACTION_GET_SESSION_STATUS,
                MediaControlIntent.ACTION_END_SESSION,
                MediaControlIntent.ACTION_REMOVE));
    }
}
