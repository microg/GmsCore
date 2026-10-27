/*
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.wearable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Bounded staging for assets received from a wearable peer. */
public final class AssetTransfers {
    private static final long MAX_ASSET_SIZE = 16 * 1024 * 1024;
    private static final long MAX_PENDING_SIZE = 32 * 1024 * 1024;
    private static final int MAX_PENDING_FILES = 8;

    private AssetTransfers() {}

    public static boolean isDigest(String value) {
        return value != null && value.matches("[A-Za-z0-9_-]{27}");
    }

    public static synchronized void clear(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && isDigest(file.getName())) file.delete();
            }
        }
        directory.delete();
    }

    /** Returns a verified staging file when the final piece completes the transfer. */
    public static synchronized File append(File directory, String name, byte[] bytes, String finalDigest) throws IOException {
        if (!isDigest(name) || bytes == null || (finalDigest != null && !isDigest(finalDigest))) {
            throw new IOException("Invalid wearable asset metadata");
        }
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cannot create wearable asset staging");
        File file = new File(directory, name);
        if (file.length() > MAX_ASSET_SIZE - bytes.length) {
            file.delete();
            throw new IOException("Wearable asset is too large");
        }
        File[] pending = directory.listFiles();
        if (pending == null) throw new IOException("Cannot inspect wearable asset staging");
        long pendingSize = 0;
        int pendingFiles = 0;
        for (File item : pending) {
            if (item.isFile()) {
                pendingSize += item.length();
                pendingFiles++;
            }
        }
        if (pendingSize > MAX_PENDING_SIZE - bytes.length || (!file.exists() && pendingFiles >= MAX_PENDING_FILES)) {
            throw new IOException("Wearable asset staging is full");
        }
        try (FileOutputStream output = new FileOutputStream(file, true)) {
            output.write(bytes);
        }
        if (finalDigest == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            try (FileInputStream input = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
            }
            if (!WearableConnection.base64encode(digest.digest()).equals(finalDigest)) {
                file.delete();
                throw new IOException("Invalid wearable asset digest");
            }
            return file;
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA1 is unavailable", e);
        }
    }
}
