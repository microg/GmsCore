/*
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.wearable;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class AssetTransfersTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void completesVerifiedAsset() throws IOException {
        File directory = temporary.newFolder();
        byte[] content = {1, 2, 3};
        String digest = WearableConnection.calculateDigest(content);
        assertNull(AssetTransfers.append(directory, digest, new byte[]{1}, null));
        File file = AssetTransfers.append(directory, digest, new byte[]{2, 3}, digest);
        assertArrayEquals(content, Files.readAllBytes(file.toPath()));
    }

    @Test public void rejectsPathTraversalBeforeCreatingAFile() throws IOException {
        File directory = temporary.newFolder();
        assertThrows(IOException.class, () -> AssetTransfers.append(directory, "../../target", new byte[]{1}, null));
        assertEquals(0, directory.listFiles().length);
        assertFalse(AssetTransfers.isDigest("../aaaaaaaaaaaaaaaaaaaaaaaa"));
    }

    @Test public void deletesAssetWithInvalidDigest() throws IOException {
        File directory = temporary.newFolder();
        String digest = WearableConnection.calculateDigest(new byte[]{1});
        assertThrows(IOException.class, () -> AssetTransfers.append(directory, digest, new byte[]{2}, digest));
        assertEquals(0, directory.listFiles().length);
    }

    @Test public void boundsIncompleteTransferCount() throws IOException {
        File directory = temporary.newFolder();
        for (int index = 0; index < 8; index++) {
            AssetTransfers.append(directory, WearableConnection.calculateDigest(new byte[]{(byte) index}), new byte[]{1}, null);
        }
        assertThrows(IOException.class, () -> AssetTransfers.append(directory,
                WearableConnection.calculateDigest(new byte[]{9}), new byte[]{1}, null));
        assertEquals(8, directory.listFiles().length);
    }

    @Test public void boundsAccumulatedAssetSize() throws IOException {
        File directory = temporary.newFolder();
        String name = WearableConnection.calculateDigest(new byte[]{1});
        AssetTransfers.append(directory, name, new byte[16 * 1024 * 1024], null);
        assertThrows(IOException.class, () -> AssetTransfers.append(directory, name, new byte[]{1}, null));
        assertFalse(new File(directory, name).exists());
    }

    @Test public void clearsOnlyTheDisconnectedSessionsFiles() throws IOException {
        File first = temporary.newFolder();
        File second = temporary.newFolder();
        String digest = WearableConnection.calculateDigest(new byte[]{1});
        AssetTransfers.append(first, digest, new byte[]{1}, null);
        AssetTransfers.append(second, digest, new byte[]{1}, null);
        AssetTransfers.clear(first);
        assertFalse(first.exists());
        assertTrue(new File(second, digest).exists());
    }
}
