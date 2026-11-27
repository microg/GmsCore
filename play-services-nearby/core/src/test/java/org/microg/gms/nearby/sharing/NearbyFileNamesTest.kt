/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.charset.StandardCharsets

class NearbyFileNamesTest {
    @Test
    fun pathComponentsCannotEscapeTheDownloadDirectory() {
        assertEquals("passwd.txt", NearbyFileNames.sanitize("../../../../etc/passwd.txt", "fallback"))
        assertEquals("notes.txt", NearbyFileNames.sanitize("C:\\Users\\me\\notes.txt", "fallback"))
        assertEquals("fallback", NearbyFileNames.sanitize("..", "fallback"))
        assertEquals("fallback", NearbyFileNames.sanitize(null, "fallback"))
    }

    @Test
    fun controlAndPlatformReservedCharactersAreReplaced() {
        assertEquals("a_b_c_.txt", NearbyFileNames.sanitize("a:b?c*.txt", "fallback"))
        assertEquals("photo.jpg", NearbyFileNames.sanitize("photo.jpg. . ", "fallback"))
    }

    @Test
    fun longNamesKeepTheirExtension() {
        val result = NearbyFileNames.sanitize("😀".repeat(200) + ".tar.gz", "fallback")
        assertTrue(result.toByteArray(StandardCharsets.UTF_8).size <= 220)
        assertTrue(result.endsWith(".gz"))
    }

    @Test
    fun existingFilesGetAStableSuffix() {
        val directory = Files.createTempDirectory("nearby-names").toFile()
        try {
            val first = NearbyFileNames.unique(directory, "photo.jpg")
            assertTrue(first.createNewFile())
            val second = NearbyFileNames.unique(directory, "photo.jpg")
            assertEquals("photo (1).jpg", second.name)
            assertFalse(second.exists())
        } finally {
            directory.deleteRecursively()
        }
    }
}
