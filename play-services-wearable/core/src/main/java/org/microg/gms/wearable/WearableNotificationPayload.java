/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable;

import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Compact JSON payload used to mirror phone notifications onto a paired Wear OS node.
 * Kept free of Android types so the encoder can be unit-tested on the JVM.
 */
public final class WearableNotificationPayload {
    public static final String PATH_POSTED = "/notification/posted";
    public static final String PATH_REMOVED = "/notification/removed";
    public static final String DATA_PATH_PREFIX = "/notification/";

    private static final Charset UTF8 = Charset.forName("UTF-8");

    public final String key;
    public final String packageName;
    public final String title;
    public final String text;
    public final boolean ongoing;
    public final int id;

    public WearableNotificationPayload(String key, String packageName, String title, String text, boolean ongoing, int id) {
        this.key = key == null ? "" : key;
        this.packageName = packageName == null ? "" : packageName;
        this.title = title == null ? "" : title;
        this.text = text == null ? "" : text;
        this.ongoing = ongoing;
        this.id = id;
    }

    public byte[] encode() {
        return toJson().getBytes(UTF8);
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder(128);
        sb.append('{');
        appendField(sb, "key", key, true);
        appendField(sb, "pkg", packageName, false);
        appendField(sb, "title", title, false);
        appendField(sb, "text", text, false);
        sb.append(",\"ongoing\":").append(ongoing);
        sb.append(",\"id\":").append(id);
        sb.append('}');
        return sb.toString();
    }

    public static WearableNotificationPayload parse(String json) {
        Map<String, String> values = parseObject(json);
        return new WearableNotificationPayload(
                values.get("key"),
                values.get("pkg"),
                values.get("title"),
                values.get("text"),
                "true".equals(values.get("ongoing")),
                parseInt(values.get("id"), 0)
        );
    }

    public static String dataItemPathForKey(String key) {
        if (key == null || key.length() == 0) {
            return DATA_PATH_PREFIX + "unknown";
        }
        StringBuilder sb = new StringBuilder(key.length());
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.' || c == '_' || c == '-') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return DATA_PATH_PREFIX + sb;
    }

    private static void appendField(StringBuilder sb, String name, String value, boolean first) {
        if (!first) sb.append(',');
        sb.append('"').append(name).append("\":\"").append(escape(value)).append('"');
    }

    static String escape(String value) {
        if (value == null) return "";
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    static Map<String, String> parseObject(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        if (json == null) return result;
        String trimmed = json.trim();
        if (trimmed.length() < 2 || trimmed.charAt(0) != '{') return result;
        String body = trimmed.substring(1, trimmed.length() - 1);
        int i = 0;
        while (i < body.length()) {
            int keyStart = body.indexOf('"', i);
            if (keyStart < 0) break;
            int keyEnd = findStringEnd(body, keyStart + 1);
            String key = unescape(body.substring(keyStart + 1, keyEnd));
            int colon = body.indexOf(':', keyEnd);
            if (colon < 0) break;
            int valueStart = skipSpaces(body, colon + 1);
            String value;
            int next;
            if (valueStart < body.length() && body.charAt(valueStart) == '"') {
                int valueEnd = findStringEnd(body, valueStart + 1);
                value = unescape(body.substring(valueStart + 1, valueEnd));
                next = valueEnd + 1;
            } else {
                int comma = body.indexOf(',', valueStart);
                next = comma < 0 ? body.length() : comma;
                value = body.substring(valueStart, next).trim();
            }
            result.put(key, value);
            i = next + 1;
        }
        return result;
    }

    private static int findStringEnd(String s, int from) {
        boolean escape = false;
        for (int i = from; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) {
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                return i;
            }
        }
        return s.length();
    }

    private static int skipSpaces(String s, int from) {
        int i = from;
        while (i < s.length() && s.charAt(i) <= ' ') i++;
        return i;
    }

    private static String unescape(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char n = value.charAt(++i);
                switch (n) {
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case '"':
                    case '\\':
                        sb.append(n);
                        break;
                    default:
                        sb.append(n);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
