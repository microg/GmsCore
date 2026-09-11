/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.moduleinstall.dynamicmodule

import android.util.Log
import com.google.android.chimera.config.ChimeraApkIdentity
import com.google.android.chimera.config.ChimeraApkManifestReader
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipFile

/** One module APK extracted from a `.mods` container with its signed Chimera identities. */
internal data class ModsApk(
    val apkFile: File,
    val identities: List<ChimeraApkIdentity>,
)

/** Parses and extracts the immutable `.mods` container format without trusting mapping identities. */
internal object ModsContainer {
    private const val TAG = "GmsModule/Mods"
    private const val ENTRY_MAPPING = "mapping.json"
    private const val MAX_APK_BYTES = 256L * 1024 * 1024
    private const val MAX_TOTAL_APK_BYTES = 512L * 1024 * 1024
    private const val MAX_APK_COUNT = 64
    private const val MAX_MAPPING_BYTES = 1024L * 1024

    /**
     * Collects every APK referenced by the feature mapping, extracts it under a fixed filename, and
     * reads its authoritative module identities from the signed APK manifest.
     */
    fun open(mods: File, destDir: File): List<ModsApk> {
        require(destDir.exists() || destDir.mkdirs()) { "mods: unable to create extraction directory" }
        ZipFile(mods).use { zip ->
            val mappingEntry = zip.getEntry(ENTRY_MAPPING)
                ?: throw IllegalArgumentException("mods: missing '$ENTRY_MAPPING'")
            val mapping = JSONObject(
                zip.getInputStream(mappingEntry).use { readBoundedText(it, MAX_MAPPING_BYTES) }
            )
            require(mapping.optString("format").startsWith("mods/")) {
                "mods: unsupported or missing format"
            }
            val features = mapping.optJSONArray("features")
                ?: throw IllegalArgumentException("mods: mapping.json missing 'features'")

            val apkPaths = LinkedHashSet<String>()
            for (featureIndex in 0 until features.length()) {
                val feature = features.optJSONObject(featureIndex)
                if (feature == null) {
                    Log.w(TAG, "mods: features[$featureIndex] is not an object, skipping")
                    continue
                }
                val paths = feature.optJSONArray("apks")
                if (paths == null) {
                    Log.w(TAG, "mods: features[$featureIndex] has no 'apks' array, skipping")
                    continue
                }
                for (pathIndex in 0 until paths.length()) {
                    val path = paths.optString(pathIndex)
                    require(
                        path.isNotEmpty() &&
                                !path.contains("..") &&
                                !path.startsWith("/") &&
                                !path.contains('\\')
                    ) { "mods: unsafe apk path '$path'" }
                    require(path.startsWith("apks/") && path.endsWith(".apk", ignoreCase = true)) {
                        "mods: invalid apk entry '$path'"
                    }
                    apkPaths += path
                    require(apkPaths.size <= MAX_APK_COUNT) {
                        "mods: too many apks (max $MAX_APK_COUNT)"
                    }
                }
            }
            require(apkPaths.isNotEmpty()) { "mods: no apks referenced" }

            var totalExtracted = 0L
            return apkPaths.mapIndexed { index, path ->
                val entry = zip.getEntry(path)
                    ?: throw IllegalArgumentException("mods: missing apk '$path'")
                val output = File(destDir, "module_$index.apk")
                zip.getInputStream(entry).use { input ->
                    val entryLimit = minOf(MAX_APK_BYTES, MAX_TOTAL_APK_BYTES - totalExtracted)
                    totalExtracted += output.outputStream().use { copyBounded(input, it, entryLimit) }
                }
                ModsApk(
                    apkFile = output,
                    identities = ChimeraApkManifestReader.readIdentities(output),
                )
            }
        }
    }
}

private fun readBoundedText(input: InputStream, limit: Long): String =
    ByteArrayOutputStream().use { output ->
        copyBounded(input, output, limit)
        output.toString(Charsets.UTF_8.name())
    }

/** Copies a stream while aborting as soon as the byte cap is exceeded. */
internal fun copyBounded(input: InputStream, output: OutputStream, limit: Long): Long {
    require(limit >= 0L) { "negative byte limit" }
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val count = input.read(buffer)
        if (count < 0) return total
        total += count
        require(total <= limit) { "entry exceeds $limit bytes" }
        output.write(buffer, 0, count)
    }
}
