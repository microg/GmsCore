/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.wearable.bridge;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wire payload for phone→watch notification mirroring over the Wearable Data Layer.
 *
 * <p>Encoding is a simple {@code key=value} line protocol (UTF-8) so it can be unit-tested on
 * the JVM without Android's {@code org.json} package.
 */
public final class NotificationPayload {

    public static final String PATH_POSTED = "/wearable/notification/posted";
    public static final String PATH_REMOVED = "/wearable/notification/removed";

    public final String key;
    public final String packageName;
    public final int id;
    public final String title;
    public final String text;
    public final String category;
    public final int priority;
    public final boolean ongoing;
    public final List<String> actions;

    public NotificationPayload(
            String key,
            String packageName,
            int id,
            String title,
            String text,
            String category,
            int priority,
            boolean ongoing,
            List<String> actions) {
        this.key = key != null ? key : "";
        this.packageName = packageName != null ? packageName : "";
        this.id = id;
        this.title = title != null ? title : "";
        this.text = text != null ? text : "";
        this.category = category != null ? category : "";
        this.priority = priority;
        this.ongoing = ongoing;
        this.actions = actions != null
                ? Collections.unmodifiableList(new ArrayList<String>(actions))
                : Collections.<String>emptyList();
    }

    public byte[] toBytes() {
        Map<String, String> fields = new LinkedHashMap<String, String>();
        fields.put("key", key);
        fields.put("packageName", packageName);
        fields.put("id", Integer.toString(id));
        fields.put("title", title);
        fields.put("text", text);
        fields.put("category", category);
        fields.put("priority", Integer.toString(priority));
        fields.put("ongoing", Boolean.toString(ongoing));
        fields.put("actions", joinActions(actions));
        return encode(fields);
    }

    public static NotificationPayload fromBytes(byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("empty payload");
        }
        Map<String, String> fields = decode(data);
        List<String> actions = splitActions(fields.get("actions"));
        return new NotificationPayload(
                fields.get("key"),
                fields.get("packageName"),
                parseInt(fields.get("id"), 0),
                fields.get("title"),
                fields.get("text"),
                fields.get("category"),
                parseInt(fields.get("priority"), 0),
                Boolean.parseBoolean(fields.get("ongoing")),
                actions);
    }

    public static byte[] removedBytes(String key, String packageName, int id) {
        Map<String, String> fields = new LinkedHashMap<String, String>();
        fields.put("key", key != null ? key : "");
        fields.put("packageName", packageName != null ? packageName : "");
        fields.put("id", Integer.toString(id));
        return encode(fields);
    }

    static byte[] encode(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            sb.append(entry.getKey())
                    .append('=')
                    .append(escape(entry.getValue()))
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    static Map<String, String> decode(byte[] data) {
        Map<String, String> fields = new LinkedHashMap<String, String>();
        String raw = new String(data, StandardCharsets.UTF_8);
        String[] lines = raw.split("\n", -1);
        for (String line : lines) {
            if (line.isEmpty()) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            fields.put(line.substring(0, eq), unescape(line.substring(eq + 1)));
        }
        return fields;
    }

    private static String joinActions(List<String> actions) {
        if (actions == null || actions.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < actions.size(); i++) {
            if (i > 0) {
                sb.append('\u001f');
            }
            sb.append(actions.get(i));
        }
        return sb.toString();
    }

    private static List<String> splitActions(String value) {
        List<String> actions = new ArrayList<String>();
        if (value == null || value.isEmpty()) {
            return actions;
        }
        String[] parts = value.split("\u001f", -1);
        Collections.addAll(actions, parts);
        return actions;
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("=", "\\=");
    }

    private static String unescape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(++i);
                if (next == 'n') {
                    sb.append('\n');
                } else {
                    sb.append(next);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
