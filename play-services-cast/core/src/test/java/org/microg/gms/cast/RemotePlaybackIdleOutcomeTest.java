/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.cast;

import static org.junit.Assert.assertEquals;

import androidx.mediarouter.media.MediaItemStatus;
import org.json.JSONObject;
import org.junit.Test;

/** Receiver outcome translation, using the same parser as MEDIA_STATUS callbacks. */
public class RemotePlaybackIdleOutcomeTest {
    private CastMediaRouteController.MediaStatusSnapshot parse(String playerState, String reason) throws Exception {
        JSONObject status = new JSONObject()
                .put("mediaSessionId", 77)
                .put("playerState", playerState)
                .put("currentTime", 12.5)
                .put("media", new JSONObject().put("duration", 95.0));
        if (reason != null) status.put("idleReason", reason);
        CastMediaRouteController.MediaStatusSnapshot snapshot = CastMediaRouteController.parseMediaStatus(status);
        assertEquals(77, snapshot.mediaSessionId);
        assertEquals(12500, snapshot.positionMs);
        assertEquals(95000, snapshot.durationMs);
        return snapshot;
    }

    @Test public void receiverErrorIsNotNormalCompletion() throws Exception {
        assertEquals(MediaItemStatus.PLAYBACK_STATE_ERROR, parse("IDLE", "ERROR").playbackState);
    }

    @Test public void stoppedPlaybackIsCanceled() throws Exception {
        assertEquals(MediaItemStatus.PLAYBACK_STATE_CANCELED, parse("IDLE", "CANCELLED").playbackState);
    }

    @Test public void replacementLoadCancelsTheOldItem() throws Exception {
        assertEquals(MediaItemStatus.PLAYBACK_STATE_CANCELED, parse("IDLE", "INTERRUPTED").playbackState);
    }

    @Test public void finishedAndLegacyUnspecifiedIdleKeepTheirMapping() throws Exception {
        for (String reason : new String[] {"FINISHED", null, "FUTURE_REASON"}) {
            assertEquals(MediaItemStatus.PLAYBACK_STATE_FINISHED, parse("IDLE", reason).playbackState);
        }
        assertEquals(MediaItemStatus.PLAYBACK_STATE_FINISHED, CastMediaRouteController.toItemPlaybackState("IDLE"));
    }

    @Test public void idleReasonDoesNotOverrideAnActivePlayerState() throws Exception {
        assertEquals(MediaItemStatus.PLAYBACK_STATE_PLAYING, parse("PLAYING", "ERROR").playbackState);
        assertEquals(MediaItemStatus.PLAYBACK_STATE_PAUSED, parse("PAUSED", "CANCELLED").playbackState);
        assertEquals(MediaItemStatus.PLAYBACK_STATE_BUFFERING, parse("BUFFERING", "INTERRUPTED").playbackState);
        assertEquals(MediaItemStatus.PLAYBACK_STATE_PENDING, parse("UNKNOWN", "ERROR").playbackState);
    }
}
