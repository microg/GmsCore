/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.wearable;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** Application-scoped paths in the system capability namespace. */
public final class CapabilityPaths {
    public static final String PACKAGE = "com.google.android.gms";
    public static final String SIGNATURE = "38918a453d07199354f8b19af05ec6562ced5788";
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private CapabilityPaths() { }

    public static boolean visibleNode(String host, String local, boolean connected, boolean reachableOnly) {
        return host != null && !host.isEmpty() && !host.equals(local) && (!reachableOnly || connected);
    }

    public static boolean validName(String name) {
        if (name == null || name.isEmpty() || name.indexOf('\0') >= 0) return false;
        try {
            return StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                    .encode(java.nio.CharBuffer.wrap(name)).remaining() <= 256;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    public static String prefix(String packageName, String signature) {
        if (packageName == null || !packageName.matches("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+")
                || signature == null || !signature.matches("[a-f0-9]{40}")) {
            throw new IllegalArgumentException("Invalid application key");
        }
        return "/capabilities/" + packageName + "/" + signature + "/";
    }

    public static String path(String packageName, String signature, String name) {
        if (!validName(name)) throw new IllegalArgumentException("Invalid capability name");
        return prefix(packageName, signature) + encode(name);
    }

    /** Decode the owner before routing a system capability record to an application. */
    public static Key parse(String path) {
        if (path == null || path.length() > 1079 || !path.startsWith("/capabilities/")) return null;
        int packageEnd = path.indexOf('/', 14);
        int signatureEnd = packageEnd < 0 ? -1 : path.indexOf('/', packageEnd + 1);
        // Bound untrusted owner text before the application-key validator's regex.
        if (packageEnd < 15 || packageEnd > 269 || signatureEnd != packageEnd + 41) return null;
        String packageName = path.substring(14, packageEnd);
        String signature = path.substring(packageEnd + 1, signatureEnd);
        try {
            String name = name(path, packageName, signature);
            return name == null ? null : new Key(packageName, signature, name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static final class Key {
        public final String packageName;
        public final String signature;
        public final String name;

        private Key(String packageName, String signature, String name) {
            this.packageName = packageName;
            this.signature = signature;
            this.name = name;
        }
    }

    public static String name(String path, String packageName, String signature) {
        if (path == null) return null;
        String prefix = prefix(packageName, signature);
        if (!path.startsWith(prefix)) return null;
        String suffix = path.substring(prefix.length());
        if (suffix.length() > 768 || suffix.indexOf('/') >= 0) return null;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i < suffix.length(); i++) {
            char c = suffix.charAt(i);
            if (c == '%') {
                if (i + 2 >= suffix.length()) return null;
                int high = Character.digit(suffix.charAt(++i), 16);
                int low = Character.digit(suffix.charAt(++i), 16);
                if (high < 0 || low < 0) return null;
                bytes.write((high << 4) | low);
            } else {
                if (c > 127) return null;
                bytes.write(c);
            }
        }
        try {
            String name = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
            return validName(name) && encode(name).equals(suffix) ? name : null;
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private static String encode(String name) {
        StringBuilder result = new StringBuilder();
        for (byte raw : name.getBytes(StandardCharsets.UTF_8)) {
            int b = raw & 255;
            if (b >= 'a' && b <= 'z' || b >= 'A' && b <= 'Z' || b >= '0' && b <= '9'
                    || "_-!.~'()*".indexOf(b) >= 0) {
                result.append((char) b);
            } else {
                result.append('%').append(HEX[b >> 4]).append(HEX[b & 15]);
            }
        }
        return result.toString();
    }
}
