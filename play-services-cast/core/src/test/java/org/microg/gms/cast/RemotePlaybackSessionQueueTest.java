/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cast;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaRouter;
import androidx.mediarouter.media.MediaSessionStatus;
import com.google.android.gms.cast.CastMediaControlIntent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import okio.ByteString;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.microg.gms.cast.channel.CastChannel;
import org.microg.gms.cast.channel.CastDeviceSession;
import org.microg.gms.cast.proto.CastMessage;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class RemotePlaybackSessionQueueTest {

    @Test
    public void queueLoadCommandStartsABufferedQueue() throws Exception {
        JSONObject command = new JSONObject(CastMediaRouteController.buildQueueLoadCommand(
                "https://example.com/movie.mp4", "video/mp4", 2500, 11));
        assertEquals("QUEUE_LOAD", command.getString("type"));
        assertEquals(11, command.getLong("requestId"));
        assertEquals(0, command.getInt("startIndex"));
        assertEquals("REPEAT_OFF", command.getString("repeatMode"));
        assertEquals(2.5, command.getDouble("currentTime"), 0.0001);
        JSONObject item = command.getJSONArray("items").getJSONObject(0);
        assertTrue(item.getBoolean("autoplay"));
        assertEquals("https://example.com/movie.mp4", item.getJSONObject("media").getString("contentId"));
        assertEquals("video/mp4", item.getJSONObject("media").getString("contentType"));
    }

    @Test
    public void queueInsertCommandAppendsWithoutReplacing() throws Exception {
        JSONObject command = new JSONObject(CastMediaRouteController.buildQueueInsertCommand(
                "https://example.com/next.mp4", "video/mp4", 42, 12));
        assertEquals("QUEUE_INSERT", command.getString("type"));
        assertEquals(42, command.getLong("mediaSessionId"));
        assertFalse(command.getJSONArray("items").getJSONObject(0).getBoolean("autoplay"));
    }

    @Test
    public void queueRemoveCommandCarriesItemIds() throws Exception {
        JSONObject command = new JSONObject(CastMediaRouteController.buildQueueRemoveCommand(42, 7, 13));
        assertEquals("QUEUE_REMOVE", command.getString("type"));
        assertEquals(42, command.getLong("mediaSessionId"));
        JSONArray ids = command.getJSONArray("itemIds");
        assertEquals(1, ids.length());
        assertEquals(7, ids.getInt(0));
    }

    @Test
    public void removeCanTargetANonCurrentQueueItem() throws Exception {
        try (ConnectedReceiver receiver = new ConnectedReceiver()) {
            CastMediaRouteController controller = receiver.controller;
            field("remotePlaybackSessionId").set(controller, "session-1");
            field("remotePlaybackItemId").set(controller, "7");
            field("mediaSessionId").setLong(controller, 42);
            member(CastDeviceSession.class, "application").set(receiver.session,
                    new org.microg.gms.cast.channel.ReceiverApplication("CC1AD845", null,
                            "receiver-session", "transport-1", null, null, java.util.Collections.emptyList()));

            RecordingCallback result = new RecordingCallback();
            Intent request = new Intent(MediaControlIntent.ACTION_REMOVE)
                    .addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                    .putExtra(MediaControlIntent.EXTRA_SESSION_ID, "session-1")
                    .putExtra(MediaControlIntent.EXTRA_ITEM_ID, "8");

            assertTrue(controller.onControlRequest(request, result));
            receiver.drain();
            assertEquals(0, result.errors);
            assertEquals(0, result.successes);
            JSONObject command = receiver.lastRequest("urn:x-cast:com.google.cast.media");
            assertEquals("QUEUE_REMOVE", command.getString("type"));
            assertEquals(42, command.getLong("mediaSessionId"));
            assertEquals(8, command.getJSONArray("itemIds").getInt(0));
        }
    }

    @Test
    public void parseMediaStatusReadsQueueItemId() throws Exception {
        JSONObject status = new JSONObject(
                "{\"mediaSessionId\":77,\"playerState\":\"PLAYING\",\"currentTime\":1,"
                        + "\"currentItemId\":9,\"items\":[{\"itemId\":9}]}");
        CastMediaRouteController.MediaStatusSnapshot snapshot = CastMediaRouteController.parseMediaStatus(status);
        assertEquals(77, snapshot.mediaSessionId);
        assertEquals(Integer.valueOf(9), snapshot.queueItemId);
    }

    @Test
    public void sessionManagementActionsAllowNullCallback() {
        String[] actions = {
                MediaControlIntent.ACTION_START_SESSION,
                MediaControlIntent.ACTION_GET_SESSION_STATUS,
                MediaControlIntent.ACTION_END_SESSION,
                MediaControlIntent.ACTION_ENQUEUE,
                MediaControlIntent.ACTION_REMOVE
        };
        for (String action : actions) {
            CastMediaRouteController controller = new CastMediaRouteController(null, "route", "localhost", 8009, 0);
            Intent request = new Intent(action).addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
            assertTrue(action, controller.onControlRequest(request, null));
        }
    }

    @Test
    public void startSessionWaitsForReceiverApplicationBeforeReportingActive() throws Exception {
        try (ConnectedReceiver receiver = new ConnectedReceiver()) {
            CastMediaRouteController controller = receiver.controller;
            RecordingCallback result = new RecordingCallback();
            Intent request = new Intent(MediaControlIntent.ACTION_START_SESSION)
                    .addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                    .putExtra(CastMediaControlIntent.EXTRA_CAST_RELAUNCH_APPLICATION, false)
                    .putExtra(CastMediaControlIntent.EXTRA_CAST_APPLICATION_ID, "CC1AD845");
            assertTrue(controller.onControlRequest(request, result));
            receiver.drain();
            assertEquals(0, result.successes);
            assertEquals(0, result.errors);
            JSONObject statusRequest = receiver.lastReceiverRequest();
            assertEquals("GET_STATUS", statusRequest.getString("type"));
            receiver.reply(new JSONObject().put("type", "RECEIVER_STATUS")
                    .put("requestId", statusRequest.getLong("requestId"))
                    .put("status", new JSONObject().put("applications", new JSONArray().put(
                            new JSONObject().put("appId", "CC1AD845")
                                    .put("sessionId", "receiver-session").put("transportId", "transport-1")))));
            assertEquals(1, result.successes);
            assertEquals(0, result.errors);
            String sessionId = result.result.getString(MediaControlIntent.EXTRA_SESSION_ID);
            assertFalse(sessionId == null || sessionId.isEmpty());
            MediaSessionStatus status = MediaSessionStatus.fromBundle(
                    result.result.getBundle(MediaControlIntent.EXTRA_SESSION_STATUS));
            assertEquals(MediaSessionStatus.SESSION_STATE_ACTIVE, status.getSessionState());

            RecordingCallback lookup = new RecordingCallback();
            Intent getStatus = new Intent(MediaControlIntent.ACTION_GET_SESSION_STATUS)
                    .addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                    .putExtra(MediaControlIntent.EXTRA_SESSION_ID, sessionId);
            assertTrue(controller.onControlRequest(getStatus, lookup));
            assertEquals(1, lookup.successes);
            assertEquals(sessionId, lookup.result.getString(MediaControlIntent.EXTRA_SESSION_ID));
        }
    }

    @Test
    public void startSessionRelaunchHonorsApplicationAndLanguageAndReportsLaunchFailure() throws Exception {
        try (ConnectedReceiver receiver = new ConnectedReceiver()) {
            RecordingCallback result = new RecordingCallback();
            Intent request = new Intent(MediaControlIntent.ACTION_START_SESSION)
                    .addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                    .putExtra(CastMediaControlIntent.EXTRA_CAST_APPLICATION_ID, "A1B2C3D4")
                    .putExtra(CastMediaControlIntent.EXTRA_CAST_RELAUNCH_APPLICATION, true)
                    .putExtra(CastMediaControlIntent.EXTRA_CAST_LANGUAGE_CODE, "fr");
            assertTrue(receiver.controller.onControlRequest(request, result));
            receiver.drain();
            assertEquals(0, result.successes);
            assertEquals(0, result.errors);
            JSONObject launch = receiver.lastReceiverRequest();
            assertEquals("LAUNCH", launch.getString("type"));
            assertEquals("A1B2C3D4", launch.getString("appId"));
            assertEquals("fr", launch.getString("language"));
            receiver.reply(new JSONObject().put("type", "LAUNCH_ERROR")
                    .put("requestId", launch.getLong("requestId")).put("reason", "NOT_FOUND"));
            assertEquals(1, result.errors);
            assertEquals(0, result.successes);
        }
    }

    @Test
    public void failedEnqueueWithoutUsableRouteDoesNotPublishSessionState() throws Exception {
        CastMediaRouteController controller = new CastMediaRouteController(null, "route", "localhost", 8009, 0);
        RecordingCallback result = new RecordingCallback();
        Intent request = new Intent(MediaControlIntent.ACTION_ENQUEUE)
                .addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                .setDataAndType(Uri.parse("https://example.com/queued.mp4"), "video/mp4")
                .putExtra(MediaControlIntent.EXTRA_SESSION_ID, "failed-enqueue-session");

        assertTrue(controller.onControlRequest(request, result));
        assertEquals(1, result.errors);
        assertEquals(0, result.successes);
        assertEquals(null, field("remotePlaybackSessionId").get(controller));
        assertEquals(null, field("pendingPlay").get(controller));

        RecordingCallback lookup = new RecordingCallback();
        Intent getStatus = new Intent(MediaControlIntent.ACTION_GET_SESSION_STATUS)
                .addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                .putExtra(MediaControlIntent.EXTRA_SESSION_ID, "failed-enqueue-session");
        assertTrue(controller.onControlRequest(getStatus, lookup));
        assertEquals(1, lookup.errors);
        assertEquals(0, lookup.successes);
    }
    @Test
    public void failedStartWithoutUsableRouteDoesNotPublishSessionState() throws Exception {
        CastMediaRouteController controller = new CastMediaRouteController(null, "route", "localhost", 8009, 0);
        RecordingCallback result = new RecordingCallback();
        Intent request = new Intent(MediaControlIntent.ACTION_START_SESSION)
                .addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                .putExtra(MediaControlIntent.EXTRA_SESSION_ID, "failed-session")
                .putExtra(CastMediaControlIntent.EXTRA_CAST_APPLICATION_ID, "A1B2C3D4")
                .putExtra(CastMediaControlIntent.EXTRA_CAST_STOP_APPLICATION_WHEN_SESSION_ENDS, true);

        assertTrue(controller.onControlRequest(request, result));
        assertEquals(1, result.errors);
        assertEquals(0, result.successes);
        assertEquals(null, field("remotePlaybackSessionId").get(controller));
        assertEquals("CC1AD845", field("remotePlaybackApplicationId").get(controller));
        assertFalse(field("stopApplicationWhenSessionEnds").getBoolean(controller));
        assertEquals(null, field("pendingSessionStart").get(controller));

        RecordingCallback lookup = new RecordingCallback();
        Intent getStatus = new Intent(MediaControlIntent.ACTION_GET_SESSION_STATUS)
                .addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                .putExtra(MediaControlIntent.EXTRA_SESSION_ID, "failed-session");
        assertTrue(controller.onControlRequest(getStatus, lookup));
        assertEquals(1, lookup.errors);
        assertEquals(0, lookup.successes);
    }

    @Test
    public void endSessionReportsEndedAndForgetsTheSession() throws Exception {
        CastMediaRouteController controller = new CastMediaRouteController(null, "route", "192.168.1.40", 8009, 0);
        field("remotePlaybackSessionId").set(controller, "session-1");
        field("mediaSessionId").setLong(controller, 42);
        RecordingCallback result = new RecordingCallback();
        Intent request = new Intent(MediaControlIntent.ACTION_END_SESSION)
                .addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                .putExtra(MediaControlIntent.EXTRA_SESSION_ID, "session-1");
        assertTrue(controller.onControlRequest(request, result));
        assertEquals(1, result.successes);
        MediaSessionStatus status = MediaSessionStatus.fromBundle(
                result.result.getBundle(MediaControlIntent.EXTRA_SESSION_STATUS));
        assertEquals(MediaSessionStatus.SESSION_STATE_ENDED, status.getSessionState());
        assertEquals(0, field("mediaSessionId").getLong(controller));
        assertEquals(null, field("remotePlaybackSessionId").get(controller));
    }

    @Test
    public void unknownSessionIsRejected() {
        CastMediaRouteController controller = new CastMediaRouteController(null, "route", "localhost", 8009, 0);
        RecordingCallback result = new RecordingCallback();
        Intent request = new Intent(MediaControlIntent.ACTION_GET_SESSION_STATUS)
                .addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                .putExtra(MediaControlIntent.EXTRA_SESSION_ID, "missing");
        assertTrue(controller.onControlRequest(request, result));
        assertEquals(1, result.errors);
        assertEquals(0, result.successes);
    }

    @Test
    public void temporaryDisconnectExtraIsPublished() {
        Bundle extras = CastMediaRouteController.temporaryDisconnectExtras();
        assertEquals(CastMediaControlIntent.ERROR_CODE_TEMPORARILY_DISCONNECTED,
                extras.getInt(CastMediaControlIntent.EXTRA_ERROR_CODE));
    }

    /** Actual session/channel code with an in-memory output; no network or receiver hardware. */
    private static class ConnectedReceiver implements AutoCloseable {
        private static final String RECEIVER_NAMESPACE = "urn:x-cast:com.google.cast.receiver";
        final CastMediaRouteController controller =
                new CastMediaRouteController(null, "route", "192.168.1.40", 8009, 0);
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final CastDeviceSession session;
        final ScheduledThreadPoolExecutor executor;

        ConnectedReceiver() throws Exception {
            Class<?> type = Class.forName(CastMediaRouteController.class.getName() + "$SessionCallbacks");
            Constructor<?> constructor = type.getDeclaredConstructor(CastMediaRouteController.class);
            constructor.setAccessible(true);
            CastDeviceSession.Callbacks callbacks = (CastDeviceSession.Callbacks) constructor.newInstance(controller);
            session = new CastDeviceSession("192.168.1.40", 8009, callbacks);
            member(type, "session").set(callbacks, session);
            field("session").set(controller, session);
            field("sessionConnected").setBoolean(controller, true);
            CastChannel channel = new CastChannel("192.168.1.40", 8009, session, "sender-0");
            member(CastChannel.class, "output").set(channel, new DataOutputStream(output));
            member(CastDeviceSession.class, "channel").set(session, channel);
            executor = (ScheduledThreadPoolExecutor) member(CastDeviceSession.class, "executor").get(session);
        }

        void drain() throws Exception {
            executor.submit(() -> {}).get(2, TimeUnit.SECONDS);
        }

        JSONObject lastRequest(String namespace) throws Exception {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(output.toByteArray()));
            JSONObject request = null;
            while (input.available() > 0) {
                byte[] frame = new byte[input.readInt()];
                input.readFully(frame);
                CastMessage message = CastMessage.ADAPTER.decode(frame);
                if (namespace.equals(message.getNamespace())) {
                    request = new JSONObject(message.getPayload_utf8());
                }
            }
            if (request == null) throw new AssertionError("No request was sent on " + namespace);
            return request;
        }

        JSONObject lastReceiverRequest() throws Exception {
            return lastRequest(RECEIVER_NAMESPACE);
        }

        void reply(JSONObject reply) throws Exception {
            session.onMessage(new CastMessage(CastMessage.ProtocolVersion.CASTV2_1_0,
                    "receiver-0", "sender-0", RECEIVER_NAMESPACE, CastMessage.PayloadType.STRING,
                    reply.toString(), null, ByteString.EMPTY));
            drain();
        }

        @Override
        public void close() {
            executor.shutdownNow();
        }
    }

    private static Field member(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static class RecordingCallback extends MediaRouter.ControlRequestCallback {
        int successes;
        int errors;
        Bundle result;

        @Override
        public void onResult(Bundle data) {
            successes++;
            result = data;
        }

        @Override
        public void onError(String error, Bundle data) {
            errors++;
        }
    }

    private static Field field(String name) throws Exception {
        Field field = CastMediaRouteController.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
