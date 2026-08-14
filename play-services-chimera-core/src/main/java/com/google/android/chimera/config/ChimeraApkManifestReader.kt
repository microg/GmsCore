/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.config

import android.content.Context
import android.util.Log
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile

/** Authoritative identity of a Chimera module APK, read from assets/ChimeraManifest.pb. */
data class ChimeraApkIdentity(
    val moduleId: String?,
    val moduleVersion: Int?,
)

/**
 * Shared reader for the APK-local assets/ChimeraManifest.pb format.
 *
 * Google Chimera module APKs store a 4-byte big-endian length followed by a ChimeraManifest protobuf.
 * Keep this parsing in one place so import, config registration, cleanup, and Provider disk fallback use the
 * same length checks and malformed-APK behavior.
 */
object ChimeraApkManifestReader {
    private const val TAG = "ChimeraApkManifestReader"
    private const val ENTRY_CHIMERA_MANIFEST = "assets/ChimeraManifest.pb"
    private const val MAX_CHIMERA_MANIFEST_BYTES = 1024L * 1024

    fun readManifest(apkFile: File): ChimeraManifest? {
        return try {
            ZipFile(apkFile).use { zip ->
                val entry = zip.getEntry(ENTRY_CHIMERA_MANIFEST) ?: return null
                zip.getInputStream(entry).use { input ->
                    val header = ByteArray(4)
                    if (readFully(input, header) != 4) return null
                    val size = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN).int
                    if (size <= 0 || size > MAX_CHIMERA_MANIFEST_BYTES) return null
                    val body = ByteArray(size)
                    if (readFully(input, body) != size) return null
                    ChimeraManifest.ADAPTER.decode(body)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "readManifest failed for ${apkFile.name}: ${e.message}")
            null
        }
    }

    /** Returns ALL module manifests bundled in the APK; one APK can expose several Chimera module IDs. */
    fun readModuleManifests(apkFile: File): List<ChimeraModuleManifest>? =
        readManifest(apkFile)?.chimeraModuleManifests

    fun readCapabilities(apkFile: File): List<ChimeraModuleCapabilities> =
        readModuleManifests(apkFile).orEmpty().mapNotNull { manifest ->
            val moduleId = manifest.moduleId?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val moduleVersion = manifest.moduleVersion?.takeIf { it > 0 } ?: return@mapNotNull null
            ChimeraModuleCapabilities(
                moduleId = moduleId,
                moduleVersion = moduleVersion,
                initializerMode = InitializerMode.fromRequiredApis(manifest.requiredApis),
                requiredApis = manifest.requiredApis,
                activityBindings = manifest.activityBindings,
                boundServiceBindings = manifest.boundServiceBindings,
                providerBindings = manifest.providerBindings,
                sliceProviderBindings = manifest.sliceProviderBindings,
            )
        }

    /** Returns capabilities only when the configured artifact still passes the persisted hash check. */
    fun readVerifiedCapabilities(
        context: Context,
        chimeraModule: ChimeraModule,
    ): List<ChimeraModuleCapabilities> {
        val installedApkPath = chimeraModule.installedApkPath?.takeIf { it.isNotEmpty() } ?: return emptyList()
        val verifiedApk = ChimeraStorage.verifiedModuleApk(
            context = context,
            file = File(installedApkPath),
            expectedModuleName = null,
            expectedSha256 = chimeraModule.apkSha256,
        ) ?: return emptyList()
        return readCapabilities(verifiedApk)
    }

    /**
     * Returns every identity carried by an APK. Chimera APKs commonly expose several module IDs from one
     * signed artifact, so callers must not collapse the artifact to the first manifest for version decisions.
     */
    fun readIdentities(apkFile: File): List<ChimeraApkIdentity> =
        readModuleManifests(apkFile).orEmpty().map {
            ChimeraApkIdentity(it.moduleId, it.moduleVersion)
        }

    private fun readFully(input: InputStream, buf: ByteArray): Int {
        var off = 0
        while (off < buf.size) {
            val r = input.read(buf, off, buf.size - off)
            if (r < 0) break
            off += r
        }
        return off
    }
}
