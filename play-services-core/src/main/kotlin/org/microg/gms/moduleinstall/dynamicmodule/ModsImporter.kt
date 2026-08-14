/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.moduleinstall.dynamicmodule

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.chimera.config.ChimeraApkManifestReader
import com.google.android.chimera.config.ChimeraConfigManager
import com.google.android.chimera.config.DynamicModuleSettings
import com.google.android.chimera.config.registry.DynamicModuleRegistry
import org.microg.gms.moduleinstall.ModuleInstaller
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

private const val TAG = "GmsModule/Mods"
private const val MAX_CONTAINER_BYTES = 512L * 1024 * 1024

internal enum class VersionDecision {
    INSTALL,
    SKIP_SAME,
    OVERWRITE,
    SKIP_LOWER,
    SKIP_DUPLICATE,
}

internal enum class ModsImportFailure {
    UNAVAILABLE,
    SOURCE_UNAVAILABLE,
    INVALID_CONTAINER,
    VALIDATION_FAILED,
    TRANSACTION_FAILED,
}

internal data class ModsImportResult(
    val decisions: List<VersionDecision> = emptyList(),
    val rejected: Int = 0,
    val failure: ModsImportFailure? = null,
) {
    val installed
        get() = decisions.count {
            it == VersionDecision.INSTALL || it == VersionDecision.OVERWRITE
        }
    val skipped
        get() = decisions.count {
            it == VersionDecision.SKIP_SAME ||
                    it == VersionDecision.SKIP_LOWER ||
                    it == VersionDecision.SKIP_DUPLICATE
        }
    val accepted
        get() = failure == null && decisions.isNotEmpty() && rejected == 0 &&
                decisions.any { it != VersionDecision.SKIP_DUPLICATE }
}

/** Complete `.mods` import use case: bounded URI ingestion, validation, planning, and atomic commit. */
internal object ModsImporter {
    private data class PlannedArtifact(
        val apk: ModsApk,
        val moduleName: String,
        val version: Long,
        val decision: VersionDecision,
        val oldApkPaths: Set<String>,
    )

    @Synchronized
    fun importFrom(context: Context, source: Uri): ModsImportResult {
        if (!DynamicModuleSettings.isAvailable(context)) {
            Log.d(TAG, "Dynamic modules unavailable; refusing import")
            return ModsImportResult(failure = ModsImportFailure.UNAVAILABLE)
        }
        return importTemporaryContainer(context, copySourceToTemp(context, source))
    }

    private fun importTemporaryContainer(context: Context, container: File?): ModsImportResult {
        container ?: return ModsImportResult(failure = ModsImportFailure.SOURCE_UNAVAILABLE)
        return try {
            importContainer(context, container)
        } finally {
            container.delete()
        }
    }

    private fun importContainer(context: Context, container: File): ModsImportResult {
        val extractionDir = File(context.cacheDir, "mods_import/${UUID.randomUUID()}")
        val apks = try {
            ModsContainer.open(container, extractionDir)
        } catch (error: Exception) {
            Log.w(TAG, "Invalid .mods container")
            runCatching { extractionDir.deleteRecursively() }
            return ModsImportResult(failure = ModsImportFailure.INVALID_CONTAINER)
        }

        try {
            if (!validateAllArtifacts(context, apks)) {
                return rejected(apks, ModsImportFailure.VALIDATION_FAILED)
            }
            val selected = selectNonConflictingArtifacts(apks)
                ?: return rejected(apks, ModsImportFailure.VALIDATION_FAILED)

            ChimeraConfigManager.reload()
            if (!validateComponentRoutes(apks, selected)) {
                return rejected(apks, ModsImportFailure.VALIDATION_FAILED)
            }
            val plans = apks.map { apk ->
                if (apk !in selected) {
                    PlannedArtifact(
                        apk = apk,
                        moduleName = canonicalArtifactName(apk),
                        version = artifactVersion(apk),
                        decision = VersionDecision.SKIP_DUPLICATE,
                        oldApkPaths = emptySet(),
                    )
                } else {
                    createPlan(apk)
                }
            }
            val installRequests = plans.mapNotNull { plan ->
                if (plan.decision != VersionDecision.INSTALL &&
                    plan.decision != VersionDecision.OVERWRITE
                ) return@mapNotNull null
                ModuleInstaller.Request(
                    sourceApk = plan.apk.apkFile,
                    moduleName = plan.moduleName,
                    version = plan.version,
                    oldApkPaths = plan.oldApkPaths,
                    invalidateModuleIds = if (plan.decision == VersionDecision.OVERWRITE) {
                        plan.apk.identities.mapNotNull { it.moduleId }.toSet()
                    } else {
                        emptySet()
                    },
                )
            }
            ModuleInstaller.installBatch(context, installRequests)
            plans.forEach { plan ->
                Log.d(TAG, "Import ${plan.apk.identities.mapNotNull { it.moduleId }} decision=${plan.decision}")
            }
            return ModsImportResult(decisions = plans.map(PlannedArtifact::decision))
        } catch (error: Exception) {
            Log.w(TAG, "Atomic import failed")
            return rejected(apks, ModsImportFailure.TRANSACTION_FAILED)
        } finally {
            runCatching { extractionDir.deleteRecursively() }
        }
    }

    private fun copySourceToTemp(context: Context, source: Uri): File? {
        val output = runCatching { File.createTempFile("import_", ".mods", context.cacheDir) }
            .getOrElse {
                Log.w(TAG, "Unable to allocate import file")
                return null
            }
        return try {
            val input = context.contentResolver.openInputStream(source)
                ?: throw IllegalArgumentException("Unable to open module container")
            input.use { sourceStream ->
                FileOutputStream(output).use { destination ->
                    copyBounded(sourceStream, destination, MAX_CONTAINER_BYTES)
                }
            }
            output.takeIf { it.length() > 0L }
        } catch (error: Exception) {
            Log.w(TAG, "Unable to ingest module container")
            null
        }.also { result ->
            if (result == null) output.delete()
        }
    }

    private fun validateAllArtifacts(context: Context, apks: List<ModsApk>): Boolean {
        if (apks.isEmpty()) return false
        return apks.all { apk ->
            val identities = apk.identities
            val validIdentities = identities.isNotEmpty() &&
                    identities.mapNotNull { it.moduleId }.distinct().size == identities.size &&
                    identities.all { !it.moduleId.isNullOrEmpty() && (it.moduleVersion ?: 0) > 0 }
            when {
                !validIdentities -> {
                    Log.w(TAG, "Reject artifact: invalid Chimera identities")
                    false
                }

                !ApkSignatureVerifier.isGoogleSignedApk(context, apk.apkFile) -> {
                    Log.w(TAG, "Reject artifact: untrusted signer")
                    false
                }

                !hasValidComponentBindings(apk) -> false
                else -> true
            }
        }
    }

    /** Validates route structure without restricting future Google-signed module IDs. */
    private fun hasValidComponentBindings(apk: ModsApk): Boolean {
        val manifests = ChimeraApkManifestReader.readModuleManifests(apk.apkFile) ?: run {
            Log.w(TAG, "Reject artifact: unreadable Chimera manifests")
            return false
        }
        val valid = manifests.all { manifest ->
            (
                    manifest.activityBindings +
                            manifest.boundServiceBindings +
                            manifest.providerBindings +
                            manifest.sliceProviderBindings
                    ).all { binding ->
                    binding.containerName?.isValidRouteName() == true &&
                            binding.moduleChimeraName?.isValidRouteName() == true
                }
        }
        if (!valid) Log.w(TAG, "Reject artifact: invalid component binding")
        return valid
    }

    /** Rejects ambiguous routes rather than letting import order choose the module implementation. */
    private fun validateComponentRoutes(apks: List<ModsApk>, selected: Set<ModsApk>): Boolean {
        val activityRoutes = linkedMapOf<String, String>()
        val serviceRoutes = linkedMapOf<String, String>()
        for (apk in apks) {
            if (apk !in selected) continue
            for (manifest in ChimeraApkManifestReader.readModuleManifests(apk.apkFile).orEmpty()) {
                val moduleId = manifest.moduleId ?: return false
                for (binding in manifest.activityBindings) {
                    val containerName = binding.containerName ?: return false
                    if (!validateRoute(activityRoutes, containerName, moduleId) ||
                        ChimeraConfigManager.findComponentByComponentName(containerName)
                            ?.moduleId
                            ?.let { it != moduleId } == true
                    ) {
                        Log.w(TAG, "Reject conflicting activity route $containerName")
                        return false
                    }
                }
                for (binding in manifest.boundServiceBindings) {
                    val containerName = binding.containerName ?: return false
                    if (!validateRoute(serviceRoutes, containerName, moduleId) ||
                        ChimeraConfigManager.findChimeraBoundService(containerName)
                            ?.moduleId
                            ?.let { it != moduleId } == true
                    ) {
                        Log.w(TAG, "Reject conflicting service route $containerName")
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun validateRoute(routes: MutableMap<String, String>, containerName: String, moduleId: String): Boolean {
        val previous = routes.putIfAbsent(containerName, moduleId)
        return previous == null || previous == moduleId
    }

    /** Selects one whole APK per duplicated module ID and rejects ambiguous multi-ID overlap. */
    private fun selectNonConflictingArtifacts(apks: List<ModsApk>): Set<ModsApk>? {
        val bestByModuleId = HashMap<String, ModsApk>()
        apks.forEach { apk ->
            apk.identities.forEach { identity ->
                val moduleId = checkNotNull(identity.moduleId)
                val current = bestByModuleId[moduleId]
                if (current == null || (identity.moduleVersion ?: 0) >
                    (current.identities.first { it.moduleId == moduleId }.moduleVersion ?: 0)
                ) {
                    bestByModuleId[moduleId] = apk
                }
            }
        }
        val selected = LinkedHashSet<ModsApk>()
        apks.forEach { apk ->
            val wins = apk.identities.count { bestByModuleId[it.moduleId] === apk }
            if (wins == apk.identities.size) {
                selected += apk
            } else if (wins != 0) {
                Log.w(TAG, "Reject ambiguous overlapping multi-ID artifact")
                return null
            }
        }
        return selected
    }

    private fun createPlan(apk: ModsApk): PlannedArtifact {
        val comparisons = apk.identities.map { identity ->
            val moduleId = checkNotNull(identity.moduleId)
            val incoming = checkNotNull(identity.moduleVersion).toLong()
            val installed = ChimeraConfigManager.findModuleByModuleId(moduleId)
            Triple(incoming, installed?.moduleVersion?.toLongOrNull(), installed?.installedApkPath)
        }
        val hasMissing = comparisons.any { it.second == null }
        val hasUpgrade = comparisons.any { (incoming, installed) ->
            installed != null && incoming > installed
        }
        val allEqual = comparisons.all { (incoming, installed) ->
            installed != null && incoming == installed
        }
        val decision = when {
            hasMissing && comparisons.all { it.second == null } -> VersionDecision.INSTALL
            hasMissing || hasUpgrade -> VersionDecision.OVERWRITE
            allEqual -> VersionDecision.SKIP_SAME
            else -> VersionDecision.SKIP_LOWER
        }
        return PlannedArtifact(
            apk = apk,
            moduleName = canonicalArtifactName(apk),
            version = artifactVersion(apk),
            decision = decision,
            oldApkPaths = comparisons.mapNotNull { (incoming, installed, path) ->
                path?.takeIf { installed == null || incoming >= installed }
            }.toSet(),
        )
    }

    private fun artifactVersion(apk: ModsApk): Long =
        apk.identities.maxOf { checkNotNull(it.moduleVersion).toLong() }

    private fun canonicalArtifactName(apk: ModsApk): String {
        val names = apk.identities.map { identity ->
            DynamicModuleRegistry.canonicalModuleName(checkNotNull(identity.moduleId))
        }.distinct()
        val raw = names.singleOrNull() ?: checkNotNull(apk.identities.first().moduleId)
        return raw.replace(Regex("[^A-Za-z0-9_.-]"), "_").take(180)
    }

    private fun String.isValidRouteName(): Boolean =
        isNotEmpty() && length <= 300 && all { it.isLetterOrDigit() || it == '.' || it == '$' || it == '_' }

    private fun rejected(apks: List<ModsApk>, failure: ModsImportFailure): ModsImportResult {
        apks.forEach { apk ->
            Log.w(TAG, "Import ${apk.identities.mapNotNull { it.moduleId }} failure=$failure")
        }
        return ModsImportResult(rejected = apks.size, failure = failure)
    }

}
