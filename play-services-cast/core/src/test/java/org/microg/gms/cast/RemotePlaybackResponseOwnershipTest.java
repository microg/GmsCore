/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.cast;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaItemStatus;
import androidx.mediarouter.media.MediaRouter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.microg.gms.cast.channel.CastDeviceSession;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class RemotePlaybackResponseOwnershipTest {
    private static final String MEDIA = "urn:x-cast:com.google.cast.media";

    @Test public void lateEmptyStopDoesNotClearReplacement() throws Exception {
        assertStaleReply(true, false, "[]");
    }

    @Test public void lateIdleStopDoesNotClearReplacement() throws Exception {
        assertStaleReply(true, false, "[{\"mediaSessionId\":42,\"playerState\":\"IDLE\"}]");
    }

    @Test public void lateSeekDoesNotRestorePreviousItem() throws Exception {
        assertStaleReply(false, true, "[{\"mediaSessionId\":42,\"playerState\":\"PAUSED\",\"currentTime\":2}]");
    }

    private static void assertStaleReply(boolean stop, boolean item, String status) throws Exception {
        CastMediaRouteController controller = currentItem();
        RecordingCallback result = new RecordingCallback();
        long requestId = track(controller, new CastMediaRouteController.PendingControl("old-session", "old-item", item, stop, 42, result));
        CastDeviceSession.Callbacks callbacks = callbacks(controller);
        String reply = "{\"type\":\"MEDIA_STATUS\",\"requestId\":" + requestId + ",\"status\":" + status + "}";
        callbacks.onTextMessage(MEDIA, reply);
        assertCurrentItem(controller);
        assertEquals(1, result.errors);
        assertEquals(0, result.successes);
        assertTrue(pending(controller).isEmpty());
        callbacks.onTextMessage(MEDIA, reply);
        assertCurrentItem(controller);
        assertEquals(1, result.errors);
    }

    @Test public void expiredStatusDoesNotRestorePreviousItem() throws Exception {
        CastMediaRouteController controller = currentItem();
        long requestId = completeRequest(controller);
        callbacks(controller).onTextMessage(MEDIA,
                "{\"type\":\"MEDIA_STATUS\",\"requestId\":" + requestId + ",\"status\":[{\"mediaSessionId\":42,\"playerState\":\"PAUSED\"}]}");
        assertCurrentItem(controller);
    }

    @Test public void expiredEmptyStatusDoesNotClearReplacement() throws Exception {
        CastMediaRouteController controller = currentItem();
        long requestId = completeRequest(controller);
        callbacks(controller).onTextMessage(MEDIA, "{\"type\":\"MEDIA_STATUS\",\"requestId\":" + requestId + ",\"status\":[]}");
        assertCurrentItem(controller);
    }

    @Test public void currentStopStillCompletesAndClearsItem() throws Exception {
        CastMediaRouteController controller = currentItem();
        RecordingCallback result = new RecordingCallback();
        long requestId = track(controller, new CastMediaRouteController.PendingControl("new-session", null, false, true, 43, result));
        callbacks(controller).onTextMessage(MEDIA, "{\"type\":\"MEDIA_STATUS\",\"requestId\":" + requestId + ",\"status\":[]}");
        assertEquals(0, field("mediaSessionId").getLong(controller));
        assertNull(field("remotePlaybackItemId").get(controller));
        assertEquals(1, result.successes);
        assertEquals(0, result.errors);
        assertEquals("new-session", result.result.getString(MediaControlIntent.EXTRA_SESSION_ID));
    }

    @Test public void currentSeekStillReturnsRealItemBundle() throws Exception {
        CastMediaRouteController controller = currentItem();
        RecordingCallback result = new RecordingCallback();
        long requestId = track(controller, new CastMediaRouteController.PendingControl("new-session", "new-item", true, false, 43, result));
        callbacks(controller).onTextMessage(MEDIA,
                "{\"type\":\"MEDIA_STATUS\",\"requestId\":" + requestId + ",\"status\":[{\"mediaSessionId\":43,\"playerState\":\"PAUSED\",\"currentTime\":2}]}");
        assertEquals(1, result.successes);
        assertEquals(0, result.errors);
        assertEquals("new-item", result.result.getString(MediaControlIntent.EXTRA_ITEM_ID));
        MediaItemStatus status = MediaItemStatus.fromBundle(result.result.getBundle(MediaControlIntent.EXTRA_ITEM_STATUS));
        assertEquals(MediaItemStatus.PLAYBACK_STATE_PAUSED, status.getPlaybackState());
        assertEquals(2000, status.getContentPosition());
        assertEquals(60000, status.getContentDuration());
    }

    @Test public void unsolicitedStatusStillUpdatesPlayback() throws Exception {
        CastMediaRouteController controller = currentItem();
        callbacks(controller).onTextMessage(MEDIA,
                "{\"type\":\"MEDIA_STATUS\",\"requestId\":0,\"status\":[{\"mediaSessionId\":43,\"playerState\":\"PAUSED\",\"currentTime\":3}]}");
        assertEquals(43, field("mediaSessionId").getLong(controller));
        assertEquals(MediaItemStatus.PLAYBACK_STATE_PAUSED, field("mediaPlaybackState").getInt(controller));
        assertEquals(3000, field("mediaPositionMs").getLong(controller));
        callbacks(controller).onTextMessage(MEDIA, "{\"type\":\"MEDIA_STATUS\",\"status\":[]}");
        assertEquals(0, field("mediaSessionId").getLong(controller));
    }

    @Test public void otherSenderNonzeroStatusStillUpdatesPlayback() throws Exception {
        CastMediaRouteController controller = currentItem();
        long otherId = field("nextMediaRequestId").getLong(controller) + 1000;
        callbacks(controller).onTextMessage(MEDIA,
                "{\"type\":\"MEDIA_STATUS\",\"requestId\":" + otherId + ",\"status\":[{\"mediaSessionId\":44,\"playerState\":\"PAUSED\"}]}");
        assertEquals(44, field("mediaSessionId").getLong(controller));
        assertEquals(MediaItemStatus.PLAYBACK_STATE_PAUSED, field("mediaPlaybackState").getInt(controller));
    }

    @Test public void stopAcknowledgementAfterSpontaneousEndStillSucceeds() throws Exception {
        CastMediaRouteController controller = currentItem();
        RecordingCallback result = new RecordingCallback();
        long requestId = track(controller, new CastMediaRouteController.PendingControl("new-session", null, false, true, 43, result));
        CastDeviceSession.Callbacks callbacks = callbacks(controller);
        callbacks.onTextMessage(MEDIA, "{\"type\":\"MEDIA_STATUS\",\"requestId\":0,\"status\":[]}");
        callbacks.onTextMessage(MEDIA, "{\"type\":\"MEDIA_STATUS\",\"requestId\":" + requestId + ",\"status\":[]}");
        assertEquals(1, result.successes);
        assertEquals(0, result.errors);
        assertEquals(0, field("mediaSessionId").getLong(controller));
    }

    @Test public void supportedRequestsAllowNullResultCallback() {
        String[] actions = {MediaControlIntent.ACTION_PLAY, MediaControlIntent.ACTION_PAUSE,
                MediaControlIntent.ACTION_RESUME, MediaControlIntent.ACTION_STOP,
                MediaControlIntent.ACTION_SEEK, MediaControlIntent.ACTION_GET_STATUS,
                MediaControlIntent.ACTION_START_SESSION, MediaControlIntent.ACTION_GET_SESSION_STATUS,
                MediaControlIntent.ACTION_END_SESSION, MediaControlIntent.ACTION_ENQUEUE,
                MediaControlIntent.ACTION_REMOVE};
        for (String action : actions) {
            CastMediaRouteController controller = new CastMediaRouteController(null, "route", "localhost", 8009, 0);
            Intent request = new Intent(action).addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
            assertTrue(action, controller.onControlRequest(request, null));
        }
    }

    @Test public void validPlayUriAllowsNullCallbackWithoutConnection() {
        CastMediaRouteController controller = new CastMediaRouteController(null, "route", "localhost", 8009, 0);
        Intent request = new Intent(MediaControlIntent.ACTION_PLAY)
                .addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK).setData(Uri.parse("https://example.com/movie.mp4"));
        assertTrue(controller.onControlRequest(request, null));
    }

    @Test public void suppliedCallbackAndUnsupportedRequestsKeepTheirContract() {
        CastMediaRouteController controller = new CastMediaRouteController(null, "route", "localhost", 8009, 0);
        RecordingCallback result = new RecordingCallback();
        Intent play = new Intent(MediaControlIntent.ACTION_PLAY).addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        assertTrue(controller.onControlRequest(play, result));
        assertEquals(1, result.errors);
        assertEquals(0, result.successes);
        assertFalse(controller.onControlRequest(new Intent("unsupported").addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK), null));
        assertFalse(controller.onControlRequest(new Intent(MediaControlIntent.ACTION_PLAY), null));
    }

    @Test public void currentSeekRejectsMismatchedReplyBeforeStateChange() throws Exception {
        assertRejectedCurrentReply(false, true,
                "[{\"mediaSessionId\":42,\"playerState\":\"PAUSED\",\"currentTime\":2,\"media\":{\"duration\":9}}]");
    }

    @Test public void currentStopRejectsMismatchedReplyBeforeStateChange() throws Exception {
        assertRejectedCurrentReply(true, false,
                "[{\"mediaSessionId\":42,\"playerState\":\"IDLE\"}]");
    }

    @Test public void currentItemRequestStillClearsAuthoritativeEmptyStatus() throws Exception {
        CastMediaRouteController controller = currentItem();
        RecordingCallback result = new RecordingCallback();
        long requestId = track(controller,
                new CastMediaRouteController.PendingControl("new-session", "new-item", true, false, 43, result));
        callbacks(controller).onTextMessage(MEDIA,
                "{\"type\":\"MEDIA_STATUS\",\"requestId\":" + requestId + ",\"status\":[]}");
        assertEquals(0, field("mediaSessionId").getLong(controller));
        assertNull(field("remotePlaybackItemId").get(controller));
        assertEquals(MediaItemStatus.PLAYBACK_STATE_FINISHED, field("mediaPlaybackState").getInt(controller));
        assertEquals(1, result.errors);
        assertEquals(0, result.successes);
    }

    private static void assertRejectedCurrentReply(boolean stop, boolean item, String status) throws Exception {
        CastMediaRouteController controller = currentItem();
        RecordingCallback result = new RecordingCallback();
        long requestId = track(controller,
                new CastMediaRouteController.PendingControl("new-session", "new-item", item, stop, 43, result));
        CastDeviceSession.Callbacks receiver = callbacks(controller);
        String reply = "{\"type\":\"MEDIA_STATUS\",\"requestId\":" + requestId + ",\"status\":" + status + "}";
        receiver.onTextMessage(MEDIA, reply);
        assertCurrentItem(controller);
        assertEquals(1, result.errors);
        assertEquals(0, result.successes);
        assertTrue(pending(controller).isEmpty());
        receiver.onTextMessage(MEDIA, reply);
        assertCurrentItem(controller);
        assertEquals(1, result.errors);
    }


    @Test public void applicationEndClearsNativeItemAndCompletesControlsOnce() throws Exception {
        CastMediaRouteController controller = currentItem();
        RecordingCallback result = new RecordingCallback();
        track(controller, new CastMediaRouteController.PendingControl("new-session", "new-item", true, false, 43, result));
        CastDeviceSession.Callbacks receiver = callbacks(controller);
        receiver.onApplicationDisconnected(CastDeviceSession.STATUS_APPLICATION_NOT_RUNNING);
        assertEquals(0, field("mediaSessionId").getLong(controller));
        assertNull(field("remotePlaybackItemId").get(controller));
        assertEquals(MediaItemStatus.PLAYBACK_STATE_CANCELED, field("mediaPlaybackState").getInt(controller));
        assertEquals(0, field("mediaPositionMs").getLong(controller));
        assertEquals(-1, field("mediaDurationMs").getLong(controller));
        assertEquals("new-session", field("remotePlaybackSessionId").get(controller));
        assertEquals(1, result.errors);
        assertEquals(0, result.successes);
        assertTrue(pending(controller).isEmpty());
        receiver.onApplicationDisconnected(CastDeviceSession.STATUS_APPLICATION_NOT_RUNNING);
        assertEquals(1, result.errors);
    }

    @Test public void applicationEndFromReplacedConnectionIsIgnored() throws Exception {
        CastMediaRouteController controller = currentItem();
        CastDeviceSession.Callbacks oldReceiver = callbacks(controller);
        CastDeviceSession replacement = new CastDeviceSession("localhost", 8009, callbacks(controller));
        field("session").set(controller, replacement);
        RecordingCallback result = new RecordingCallback();
        track(controller, new CastMediaRouteController.PendingControl("new-session", "new-item", true, false, 43, result));
        try {
            oldReceiver.onApplicationDisconnected(CastDeviceSession.STATUS_APPLICATION_NOT_RUNNING);
            assertCurrentItem(controller);
            assertSame(replacement, field("session").get(controller));
            assertEquals(1, pending(controller).size());
            assertEquals(0, result.errors);
        } finally {
            replacement.disconnect();
        }
    }

    @Test public void applicationEndPreservesLaunchesAndReentrantNewRequests() throws Exception {
        CastMediaRouteController controller = currentItem();
        RecordingCallback launching = new RecordingCallback();
        Object play = pendingLaunch("PendingPlay",
                new Class<?>[] {String.class, String.class, long.class, String.class, MediaRouter.ControlRequestCallback.class},
                new Object[] {"https://example.com/movie.mp4", "video/mp4", 0L, "new-session", launching});
        Object start = pendingLaunch("PendingSessionStart",
                new Class<?>[] {String.class, String.class, MediaRouter.ControlRequestCallback.class},
                new Object[] {"new-session", "new-app", launching});
        field("pendingPlay").set(controller, play);
        field("pendingSessionStart").set(controller, start);
        RecordingCallback fresh = new RecordingCallback();
        RecordingCallback old = new RecordingCallback() {
            @Override public void onError(String error, Bundle data) {
                super.onError(error, data);
                try {
                    track(controller, new CastMediaRouteController.PendingControl("new-session", null, true, false, 0, fresh));
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }
        };
        track(controller, new CastMediaRouteController.PendingControl("new-session", "new-item", true, false, 43, old));
        callbacks(controller).onApplicationDisconnected(CastDeviceSession.STATUS_APPLICATION_NOT_RUNNING);
        assertSame(play, field("pendingPlay").get(controller));
        assertSame(start, field("pendingSessionStart").get(controller));
        assertEquals(0, launching.errors);
        assertEquals(1, old.errors);
        assertEquals(1, pending(controller).size());
        assertEquals(0, fresh.errors);
    }

    private static Object pendingLaunch(String name, Class<?>[] types, Object[] args) throws Exception {
        Class<?> type = Class.forName(CastMediaRouteController.class.getName() + "$" + name);
        Constructor<?> constructor = type.getDeclaredConstructor(types);
        constructor.setAccessible(true);
        return constructor.newInstance(args);
    }

    private static CastMediaRouteController currentItem() throws Exception {
        CastMediaRouteController controller = new CastMediaRouteController(null, "route", "localhost", 8009, 0);
        field("mediaSessionId").setLong(controller, 43);
        field("remotePlaybackSessionId").set(controller, "new-session");
        field("remotePlaybackItemId").set(controller, "new-item");
        field("mediaPlaybackState").setInt(controller, MediaItemStatus.PLAYBACK_STATE_PLAYING);
        field("mediaPositionMs").setLong(controller, 12000);
        field("mediaDurationMs").setLong(controller, 60000);
        return controller;
    }

    private static void assertCurrentItem(CastMediaRouteController controller) throws Exception {
        assertEquals(43, field("mediaSessionId").getLong(controller));
        assertEquals("new-session", field("remotePlaybackSessionId").get(controller));
        assertEquals("new-item", field("remotePlaybackItemId").get(controller));
        assertEquals(MediaItemStatus.PLAYBACK_STATE_PLAYING, field("mediaPlaybackState").getInt(controller));
        assertEquals(12000, field("mediaPositionMs").getLong(controller));
        assertEquals(60000, field("mediaDurationMs").getLong(controller));
    }

    private static class RecordingCallback extends MediaRouter.ControlRequestCallback {
        int successes;
        int errors;
        Bundle result;
        @Override public void onResult(Bundle data) { successes++; result = data; }
        @Override public void onError(String error, Bundle data) { errors++; }
    }

    private static Field field(String name) throws Exception {
        Field field = CastMediaRouteController.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    @SuppressWarnings("unchecked")
    private static Map<Long, CastMediaRouteController.PendingControl> pending(CastMediaRouteController controller) throws Exception {
        return (Map<Long, CastMediaRouteController.PendingControl>) field("pendingControls").get(controller);
    }

    private static long track(CastMediaRouteController controller, CastMediaRouteController.PendingControl item) throws Exception {
        long requestId = field("nextMediaRequestId").getLong(controller);
        field("nextMediaRequestId").setLong(controller, requestId + 1);
        pending(controller).put(requestId, item);
        return requestId;
    }

    private static long completeRequest(CastMediaRouteController controller) throws Exception {
        RecordingCallback result = new RecordingCallback();
        long requestId = track(controller, new CastMediaRouteController.PendingControl("new-session", null, false, false, 43, result));
        callbacks(controller).onTextMessage(MEDIA, "{\"type\":\"LOAD_FAILED\",\"requestId\":" + requestId + "}");
        assertEquals(1, result.errors);
        return requestId;
    }

    private static CastDeviceSession.Callbacks callbacks(CastMediaRouteController controller) throws Exception {
        Class<?> type = Class.forName(CastMediaRouteController.class.getName() + "$SessionCallbacks");
        Constructor<?> constructor = type.getDeclaredConstructor(CastMediaRouteController.class);
        constructor.setAccessible(true);
        return (CastDeviceSession.Callbacks) constructor.newInstance(controller);
    }
}
