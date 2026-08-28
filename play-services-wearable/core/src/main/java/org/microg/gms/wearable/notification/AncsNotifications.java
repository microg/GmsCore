/*
 * Copyright (C) 2026 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.wearable.notification;

import com.google.android.gms.wearable.AncsNotification;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Parser and model for ANCS (Apple Notification Center Service) notification
 * attributes, the wire protocol used by Wear Apps to mirror phone notifications
 * on a companion device.
 * <p>
 * This is the first block of the notification bridge for
 * <a href="https://github.com/microg/GmsCore/issues/2843">WearOS support</a>:
 * it turns a "Get Notification Attributes" Data Source frame into a structured
 * value object that can be routed through {@code WearableListenerService#onNotificationReceived}.
 * <p>
 * Frame layout (see ANCS specification, *Notification attributes*):
 * <pre>
 *   [0]        Command ID (0x02 = Notification Attributes)
 *   [1 .. 4]   Notification UID (4 bytes, little-endian)
 *   then, per requested attribute:
 *   [n]        Attribute ID (1 byte)
 *   [n+1 ..]   Attribute length (2 bytes, little-endian)
 *   [...]      Attribute value
 * </pre>
 * Not-yet-requested attributes are ignored by the device, so a frame may carry a
 * subset of the supported attributes; truncated or oversized frames are rejected
 * instead of producing partial state.
 */
public final class AncsNotifications {

    public static final int COMMAND_GET_NOTIFICATION_ATTRIBUTES = 0x02;

    public static final int ATTR_APP_IDENTIFIER = 0x00;
    public static final int ATTR_TITLE = 0x01;
    public static final int ATTR_SUBTITLE = 0x02;
    public static final int ATTR_MESSAGE = 0x03;
    /** 4-byte little-endian counter, unlike the other (string) attributes. */
    public static final int ATTR_MESSAGE_SIZE = 0x04;
    public static final int ATTR_DATE = 0x05;
    public static final int ATTR_POSITIVE_ACTION_LABEL = 0x06;
    public static final int ATTR_NEGATIVE_ACTION_LABEL = 0x07;

    private AncsNotifications() {
    }

    /**
     * Parses one complete "Notification Attributes" frame from the ANCS Data Source.
     *
     * @throws IllegalArgumentException if the frame is truncated, malformed or too large.
     */
    public static Notification parse(byte[] frame) {
        Notification result = new Notification();
        if (frame == null || frame.length < 5) {
            throw new IllegalArgumentException("ANCS notification attributes frame is too short");
        }
        if ((frame[0] & 0xFF) != COMMAND_GET_NOTIFICATION_ATTRIBUTES) {
            throw new IllegalArgumentException("Unexpected ANCS command id: 0x" + Integer.toHexString(frame[0] & 0xFF));
        }
        result.notificationUid = readUInt32(frame, 1);
        for (int i = 5; i < frame.length; ) {
            int attributeId = frame[i] & 0xFF;
            if (i + 3 > frame.length) {
                throw new IllegalArgumentException("Truncated ANCS attribute header at offset " + i);
            }
            int length = readUInt16(frame, i + 1);
            int valueOffset = i + 3;
            if (valueOffset + length > frame.length) {
                throw new IllegalArgumentException("Truncated ANCS attribute value at offset " + valueOffset);
            }
            if (attributeId == ATTR_MESSAGE_SIZE) {
                if (length != 4) {
                    throw new IllegalArgumentException("MESSAGE_SIZE attribute must carry 4 bytes, got " + length);
                }
                result.messageSize = readUInt32(frame, valueOffset);
            } else {
                String value = new String(frame, valueOffset, length, StandardCharsets.UTF_8);
                switch (attributeId) {
                    case ATTR_APP_IDENTIFIER:
                        result.appIdentifier = stripTerminator(value);
                        break;
                    case ATTR_TITLE:
                        result.title = value;
                        break;
                    case ATTR_SUBTITLE:
                        result.subtitle = value;
                        break;
                    case ATTR_MESSAGE:
                        result.message = value;
                        break;
                    case ATTR_DATE:
                        result.date = stripTerminator(value);
                        break;
                    case ATTR_POSITIVE_ACTION_LABEL:
                        result.positiveActionLabel = value;
                        break;
                    case ATTR_NEGATIVE_ACTION_LABEL:
                        result.negativeActionLabel = value;
                        break;
                    default:
                        // Unknown future attribute: skip value, keep parsing.
                        break;
                }
            }
            i = valueOffset + length;
        }
        return result;
    }

    /** Builds a Control Point request to read the wanted attributes of a notification. */
    public static byte[] readAttributesRequest(long notificationUid, int... attributeIds) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(8 + attributeIds.length);
        out.write(COMMAND_GET_NOTIFICATION_ATTRIBUTES);
        writeUInt32(out, notificationUid);
        for (int attributeId : attributeIds) {
            out.write(attributeId & 0xFF);
        }
        return out.toByteArray();
    }

    private static int readUInt16(byte[] src, int offset) {
        return (src[offset] & 0xFF) | ((src[offset + 1] & 0xFF) << 8);
    }

    private static long readUInt32(byte[] src, int offset) {
        return (src[offset] & 0xFFL) | ((src[offset + 1] & 0xFFL) << 8)
                | ((src[offset + 2] & 0xFFL) << 16) | ((src[offset + 3] & 0xFFL) << 24);
    }

    private static void writeUInt32(ByteArrayOutputStream out, long value) {
        out.write((int) (value & 0xFF));
        out.write((int) ((value >>> 8) & 0xFF));
        out.write((int) ((value >>> 16) & 0xFF));
        out.write((int) ((value >>> 24) & 0xFF));
    }

    private static String stripTerminator(String value) {
        int index = value.indexOf('\0');
        return index < 0 ? value : value.substring(0, index);
    }

    /**
     * Structured view of a parsed ANCS notification. Field values match the ANCS
     * attribute semantics; unknown future attributes are skipped by the parser.
     */
    public static class Notification implements AncsNotification {
        private long notificationUid;
        private String appIdentifier;
        private String title;
        private String subtitle;
        private String message;
        private long messageSize;
        private String date;
        private String positiveActionLabel;
        private String negativeActionLabel;

        public long getNotificationUid() {
            return notificationUid;
        }

        public String getAppIdentifier() {
            return appIdentifier;
        }

        public String getTitle() {
            return title;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public String getMessage() {
            return message;
        }

        public long getMessageSize() {
            return messageSize;
        }

        public String getDate() {
            return date;
        }

        public String getPositiveActionLabel() {
            return positiveActionLabel;
        }

        public String getNegativeActionLabel() {
            return negativeActionLabel;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("AncsNotification{uid=").append(notificationUid);
            sb.append(", app=").append(appIdentifier);
            sb.append(", title='").append(title).append('\'');
            if (subtitle != null) {
                sb.append(", subtitle='").append(subtitle).append('\'');
            }
            if (message != null) {
                sb.append(", message='").append(message).append('\'');
            }
            if (date != null) {
                sb.append(", date='").append(date).append('\'');
            }
            return sb.append('}').toString();
        }
    }
}