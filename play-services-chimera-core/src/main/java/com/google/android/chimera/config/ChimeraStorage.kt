/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.config

import android.content.Context
import android.os.Build
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

object ChimeraStorage {
    const val CHIMERA_DIR = "chimera"
    const val MODULE_SUBDIR = "m"
    const val APK_PREFIX = "dl-"
    const val APK_SUFFIX = ".apk"

    private val MODULE_APK_RE = Regex("^${Regex.escape(APK_PREFIX)}(.+)_(\\d+)${Regex.escape(APK_SUFFIX)}$")
    private val MODULE_CONTAINER_RE = Regex("[0-9a-fA-F]{8}")

    data class ModuleApkFile(
        val file: File,
        val moduleName: String,
        val version: String,
    )

    data class ModuleApkDestination(
        val file: File,
        val priority: Int,
    )

    fun moduleRoot(context: Context): File {
        val base = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !context.isDeviceProtectedStorage) {
            context.createDeviceProtectedStorageContext()
        } else {
            context
        }
        return File(base.getDir(CHIMERA_DIR, Context.MODE_PRIVATE), MODULE_SUBDIR)
    }

    fun apkFileName(moduleName: String, version: String): String =
        "$APK_PREFIX${moduleName}_$version$APK_SUFFIX"

    /**
     * Reserves a unique Chimera container directory and returns the matching config priority.
     * Creating the directory while holding the object monitor makes concurrent imports in the
     * same process unable to select the same slot.
     */
    @Synchronized
    fun allocateModuleApkFile(context: Context, moduleName: String, version: String): ModuleApkDestination {
        val root = ensureModuleRoot(context)
        var priority = 0
        while (true) {
            val container = File(root, String.format("%08x", priority + 1))
            if (!container.exists()) {
                check(container.mkdir()) { "Failed to reserve Chimera module directory: ${container.absolutePath}" }
                makeDirectoryReadable(container)
                return ModuleApkDestination(
                    File(container, apkFileName(moduleName, version)),
                    priority
                )
            }
            check(priority < Int.MAX_VALUE - 1) { "No free Chimera module directory" }
            priority++
        }
    }

    fun ensureModuleRoot(context: Context): File {
        val root = moduleRoot(context)
        root.mkdirs()
        root.parentFile?.let { makeDirectoryReadable(it) }
        makeDirectoryReadable(root)
        return root
    }

    /** Make the module apk and its container directory readable by other processes. */
    fun makeModuleApkReadable(apkFile: File) {
        apkFile.parentFile?.let { makeDirectoryReadable(it) }
        apkFile.setReadable(true, false)
    }

    fun parseModuleApkFile(file: File): ModuleApkFile? {
        val match = MODULE_APK_RE.matchEntire(file.name) ?: return null
        return ModuleApkFile(file, match.groupValues[1], match.groupValues[2])
    }

    fun listDownloadedApks(context: Context, moduleName: String? = null): List<ModuleApkFile> =
        listDownloadedApks(moduleRoot(context), moduleName)

    fun listDownloadedApks(moduleRoot: File, moduleName: String? = null): List<ModuleApkFile> {
        if (!moduleRoot.isDirectory) return emptyList()
        return runCatching {
            buildList {
                moduleRoot.listFiles()?.forEach { entry ->
                    if (entry.isDirectory) {
                        entry.listFiles()?.forEach { file -> addIfModuleApk(file, moduleName) }
                    } else {
                        addIfModuleApk(entry, moduleName)
                    }
                }
            }.sortedWith(compareByDescending<ModuleApkFile> { it.version.toLongOrNull() ?: Long.MIN_VALUE }
                .thenBy { it.file.absolutePath })
        }.getOrDefault(emptyList())
    }

    fun findDownloadedApk(context: Context, moduleName: String): File? {
        return findDownloadedApkInRoot(moduleRoot(context), moduleName)
    }

    fun findDownloadedApkInRoot(moduleRoot: File, moduleName: String): File? {
        return listDownloadedApks(moduleRoot, moduleName).firstOrNull()?.file
    }

    /**
     * Confirms that a config-owned module artifact is still the APK imported for that config entry.
     * This intentionally requires a persisted digest: disk-scan recovery without one is not proof of
     * artifact integrity and must not be reported as an installed module.
     */
    fun verifiedModuleApk(
        context: Context,
        file: File?,
        expectedModuleName: String?,
        expectedSha256: String?,
    ): File? = verifiedModuleApkInRoot(
        file = file,
        moduleRoot = moduleRoot(context),
        expectedModuleName = expectedModuleName,
        expectedSha256 = expectedSha256,
    )

    /**
     * File-only variant of [verifiedModuleApk] for callers that already own the Chimera root.
     * A module artifact is executable only when it remains under that root and still matches the
     * digest persisted by its import transaction.
     */
    internal fun verifiedModuleApkInRoot(
        file: File?,
        moduleRoot: File,
        expectedModuleName: String?,
        expectedSha256: String?,
    ): File? {
        val candidate = file?.canonicalOrNull() ?: return null
        val root = moduleRoot.canonicalOrNull() ?: return null
        if (!candidate.isUnder(root) || !candidate.isFile || !candidate.canRead()) return null
        val parsed = parseModuleApkFile(candidate) ?: return null
        if (!expectedModuleName.isNullOrEmpty() && parsed.moduleName != expectedModuleName) return null
        if (expectedSha256.isNullOrEmpty() || !isApk(candidate)) return null
        val actual = sha256Hex(candidate)
        return candidate.takeIf { actual.equals(expectedSha256, ignoreCase = true) }
    }

    /** Deletes only a canonical dl-*.apk below this user's Chimera module root. */
    fun safeDeleteModuleApk(context: Context, apkFile: File): Boolean = runCatching {
        val candidate = apkFile.canonicalOrNull() ?: return@runCatching false
        val root = moduleRoot(context).canonicalOrNull() ?: return@runCatching false
        if (!candidate.isUnder(root) || parseModuleApkFile(candidate) == null) return@runCatching false
        val parent = candidate.parentFile
        val deleted = !candidate.exists() || candidate.delete()
        cleanupEmptyModuleContainer(parent)
        deleted
    }.getOrDefault(false)

    private fun MutableList<ModuleApkFile>.addIfModuleApk(file: File, moduleName: String?) {
        if (!file.isFile) return
        val parsed = parseModuleApkFile(file) ?: return
        if (moduleName == null || parsed.moduleName == moduleName) add(parsed)
    }

    private fun isApk(file: File): Boolean = runCatching {
        ZipFile(file).use { zip -> zip.getEntry("AndroidManifest.xml") != null }
    }.getOrDefault(false)

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun makeDirectoryReadable(dir: File) {
        dir.mkdirs()
        dir.setReadable(true, false)
        dir.setExecutable(true, false)
    }

    private fun cleanupEmptyModuleContainer(dir: File?) {
        dir?.takeIf {
            it.parentFile?.name == MODULE_SUBDIR &&
                    MODULE_CONTAINER_RE.matches(it.name) &&
                    it.isDirectory &&
                    it.list()?.isEmpty() == true
        }?.delete()
    }

    private fun File.canonicalOrNull(): File? = runCatching { canonicalFile }.getOrNull()

    private fun File.isUnder(root: File): Boolean {
        val rootPath = root.absolutePath
        return absolutePath == rootPath || absolutePath.startsWith(rootPath + File.separator)
    }
}
