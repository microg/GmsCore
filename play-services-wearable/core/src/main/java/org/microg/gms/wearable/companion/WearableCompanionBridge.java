/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable.companion;

import org.microg.gms.wearable.notification.AncsNotifications;
import org.microg.wearable.WearableConnection;
import org.microg.wearable.proto.RootMessage;
import org.microg.wearable.proto.SetDataItem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okio.ByteString;

/**
 * Phone-side Wear OS companion bridge.
 *
 * <p>The companion link is a length-prefixed protobuf channel (see
 * {@code org.microg.wearable.WearableConnection}); the phone currently exposes it over TCP (port
 * 5601) and, with the Wear OS foundation work, also over Bluetooth RFCOMM (Flow / Flow15). This
 * bridge sits on the phone side of that link and performs the two remaining companion functions:
 *
 * <ol>
 *   <li><b>Notification echo (ANCS):</b> turns each parsed ANCS notification (see
 *   {@link AncsNotifications}) into a Wear data-layer {@link SetDataItem} on a reserved URI, and
 *   pushes it to every connected wear node via the already-established connection. A wear-side
 *   app subscribed through {@code WearableListenerService#onDataChanged} then renders it. The
 *   raw ANCS frame is preserved in the payload so the wear side can re-parse with the same model.</li>
 * </ol>
 *
 * <p>This class is deliberately transport-agnostic: callers pass an already-connected
 * {@link WearableConnection}, so the identical code path serves TCP, Bluetooth and (for tests)
 * in-memory loopback links. Unit-testing an ephemeral loopback pair exercises the full wire
 * handshake ({@code Connect} exchange) and one notification round-trip without any wearable
 * hardware.
 */
public final class WearableCompanionBridge {

    /** Reserved data-layer path for versioned ANCS notification envelopes. */
    public static final String ANCS_NOTIFICATION_V1_PATH = "/notification/ancs/v1";

    /** Version byte of the envelope written by {@link #envelope}. */
    public static final int ANCS_ENVELOPE_VERSION = 1;

    /** Upper bound for a single ANCS payload carried on the data layer. */
    public static final long MAX_ANCS_PAYLOAD = 16 * 1024;

    private WearableCompanionBridge() {
    }

    /**
     * Builds the data-layer URI a wear app should subscribe to see the given notification.
     *
     * @param nodeId local node id of the phone (source of the notification).
     * @param notificationUid ANCS notification uid carried by the payload.
     */
    public static String buildAncsUri(String nodeId, long notificationUid) {
        return "wear://" + nodeId + ANCS_NOTIFICATION_V1_PATH + "/" + notificationUid;
    }

    /**
     * Serializes one notification into the versioned envelope carried on the data layer.
     *
     * <pre>
     *   [0]            Version (0x01)
     *   [1 .. 4]       ANCS notification uid (little-endian, 4 bytes)
     *   [5 .. ]        Raw ANCS Notification Attributes frame (see AncsNotifications)
     * </pre>
     *
     * @return the envelope, or {@code null} if the raw frame does not fit in
     * {@link #MAX_ANCS_PAYLOAD}.
     */
    public static byte[] envelope(AncsNotifications.Notification notification, byte[] rawFrame) {
        if (notification == null || rawFrame == null || rawFrame.length == 0) {
            return null;
        }
        int payloadLength = 5 + rawFrame.length;
        if (payloadLength > MAX_ANCS_PAYLOAD) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(payloadLength);
        out.write(ANCS_ENVELOPE_VERSION);
        writeUInt32(out, notification.getNotificationUid());
        out.write(rawFrame, 0, rawFrame.length);
        return out.toByteArray();
    }

    /**
     * Builds the data-layer item that carries one notification to a connected wear node.
     *
     * @param nodeId source node id (phone local node id).
     * @param seqId monotonic sequence id expected by the wear side (see {@code SetDataItem.seqId}).
     * @param packageName package that owns the notification (used for access control).
     */
    public static SetDataItem toSetDataItem(
            String nodeId, long seqId, String packageName,
            AncsNotifications.Notification notification, byte[] rawFrame) {
        byte[] payload = envelope(notification, rawFrame);
        if (payload == null) {
            return null;
        }
        return new SetDataItem.Builder()
                .packageName(packageName)
                .uri(buildAncsUri(nodeId, notification.getNotificationUid()))
                .data(ByteString.of(payload))
                .seqId(seqId)
                .deleted(false)
                .source(nodeId)
                .build();
    }

    /**
     * Pushes one notification onto an active companion connection, using the raw ANCS frame that
     * produced {@code notification} so the baseband payload is preserved verbatim.
     *
     * @return the seq id written, or {@code -1} if the payload was oversized or the write failed.
     */
    public static long pushNotification(
            WearableConnection connection, String nodeId, long seqId, String packageName,
            AncsNotifications.Notification notification, byte[] rawFrame) {
        SetDataItem item = toSetDataItem(nodeId, seqId, packageName, notification, rawFrame);
        if (item == null) {
            return -1;
        }
        try {
            connection.writeMessage(new RootMessage.Builder().setDataItem(item).build());
            return seqId;
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * Re-loads an {@link AncsNotification} from a data-layer envelope written by
     * {@link #envelope}. Returns {@code null} for unknown versions or truncated payloads.
     */
    public static AncsNotifications.Notification fromEnvelope(byte[] payload) {
        if (payload == null || payload.length < 5 || payload[0] != ANCS_ENVELOPE_VERSION) {
            return null;
        }
        long uid = readUInt32(payload, 1);
        byte[] rawFrame = new byte[payload.length - 5];
        System.arraycopy(payload, 5, rawFrame, 0, rawFrame.length);
        try {
            AncsNotifications.Notification notification = AncsNotifications.parse(rawFrame);
            if (notification.getNotificationUid() != uid) {
                return null;
            }
            return notification;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void writeUInt32(ByteArrayOutputStream out, long value) {
        out.write((int) (value & 0xFF));
        out.write((int) ((value >>> 8) & 0xFF));
        out.write((int) ((value >>> 16) & 0xFF));
        out.write((int) ((value >>> 24) & 0xFF));
    }

    private static long readUInt32(byte[] src, int offset) {
        return (src[offset] & 0xFFL) | ((src[offset + 1] & 0xFFL) << 8)
                | ((src[offset + 2] & 0xFFL) << 16) | ((src[offset + 3] & 0xFFL) << 24);
    }
}