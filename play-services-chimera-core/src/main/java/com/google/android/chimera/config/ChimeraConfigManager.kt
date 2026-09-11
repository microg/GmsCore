/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.config

import android.content.Context
import android.os.StrictMode
import android.util.Log
import com.google.android.chimera.config.registry.DynamicModuleRegistry
import com.google.android.gms.chimera.ModuleInfo
import com.google.android.gms.common.app.AppContext
import org.microg.gms.common.Constants
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import androidx.core.net.toUri
// Storage path (CE/DE) decisions must use the real system SDK: microG Profile's spoofed Build
// returns SDK_INT < 24 in processes where the Profile isn't active (e.g. :ui), which disables the
// forced-DE branch and falls back to CE, causing the main process (DE) and :ui (CE) to read/write
// different chimera_manifest.pb files (cross-process inconsistency in listing/deletion).
import android.os.Build

data class RemovedModuleMetadata(
    val moduleIds: Set<String> = emptySet(),
    val apkPaths: Set<String> = emptySet(),
    val persisted: Boolean = true,
)

object ChimeraConfigManager {

    private const val TAG = "ChimeraConfigManager"

    private var configFile: File? = null
    private var currentConfig: ChimeraManifestStore? = null
    private var loadedConfigLastModified = Long.MIN_VALUE
    private var loadedConfigLength = Long.MIN_VALUE
    private val lock = ReentrantReadWriteLock()

    @JvmStatic
    private fun getChimeraManifest(): ChimeraManifestStore {
        lock.read {
            currentConfig?.let { cached ->
                val file = configFile
                if (file == null ||
                    (file.lastModified() == loadedConfigLastModified && file.length() == loadedConfigLength)
                ) return cached
            }
        }

        lock.write {
            currentConfig?.let { cached ->
                val file = configFile
                if (file == null ||
                    (file.lastModified() == loadedConfigLastModified && file.length() == loadedConfigLength)
                ) return cached
            }

            if (!AppContext.isInitialized()) {
                // Don't cache into currentConfig: otherwise configFile is never set and later saveToFile silently loses writes
                return ChimeraManifestStore()
            }

            val context = AppContext.get()
            val ctx = if (Build.VERSION.SDK_INT >= 24 &&
                !context.isDeviceProtectedStorage
            ) {
                context.createDeviceProtectedStorageContext()
            } else {
                context
            }

            val file = File(getChimeraDir(ctx), "chimera_manifest.pb")
            configFile = file
            Log.d(TAG, "getChimeraManifest: reading configFile=${file.path}")

            currentConfig = try {
                withConfigFileLock(file) { readConfigFile(file) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read ChimeraManifestStore: ${e.message}", e)
                // Fail closed for later updates: updateConfig reads the file again under its cross-process lock
                // and refuses to save when decoding still fails.
                ChimeraManifestStore()
            }
            loadedConfigLastModified = file.lastModified()
            loadedConfigLength = file.length()

            return currentConfig!!
        }
    }

    fun updateConfig(
        autoSave: Boolean = true,
        transform: (ChimeraManifestStore) -> ChimeraManifestStore
    ): ChimeraManifestStore = lock.write {
        if (!autoSave) {
            val oldConfig = currentConfig ?: getChimeraManifest()
            return@write runCatching { transform(oldConfig) }
                .onSuccess { currentConfig = it }
                .getOrElse {
                    Log.e(TAG, "Failed to update in-memory ChimeraManifestStore", it)
                    oldConfig
                }
        }

        val file = resolveConfigFile()
        if (file == null) {
            Log.e(TAG, "Cannot update ChimeraManifestStore before AppContext initialization")
            return@write currentConfig ?: ChimeraManifestStore()
        }
        return@write try {
            withConfigFileLock(file) {
                // Cross-process read-modify-write: never transform a stale process-local snapshot.
                val diskConfig = readConfigFile(file)
                val newConfig = transform(diskConfig)
                if (!saveToFile(file, newConfig)) {
                    currentConfig = diskConfig
                    loadedConfigLastModified = file.lastModified()
                    loadedConfigLength = file.length()
                    diskConfig
                } else {
                    currentConfig = newConfig
                    loadedConfigLastModified = file.lastModified()
                    loadedConfigLength = file.length()
                    newConfig
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Refusing to overwrite unreadable ChimeraManifestStore: ${e.message}", e)
            currentConfig ?: ChimeraManifestStore()
        }
    }

    private fun saveToFile(file: File, config: ChimeraManifestStore): Boolean {
        val tmpFile = File(file.parentFile, "${file.name}.tmp-${UUID.randomUUID()}")
        try {
            StrictMode.allowThreadDiskWrites().use {
                tmpFile.outputStream().use { ChimeraManifestStore.ADAPTER.encode(it, config) }
            }
            if (!tmpFile.renameTo(file)) {
                throw IOException("Failed to overwrite config file: ${file.path}")
            }
            Log.d(TAG, "saveToFile: persisted ${config.chimeraModules.size} modules to ${file.path}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save ChimeraManifestStore: ${e.message}", e)
            return false
        } finally {
            tmpFile.delete()
        }
    }

    fun reload(): ChimeraManifestStore = lock.write {
        currentConfig = null
        loadedConfigLastModified = Long.MIN_VALUE
        loadedConfigLength = Long.MIN_VALUE
        getChimeraManifest()
    }

    fun getConfigFile(context: Context): File = File(getChimeraDir(context), "chimera_manifest.pb")

    fun getConfigLastModified(context: Context): Long = getConfigFile(context).takeIf { it.isFile }?.lastModified() ?: 0L

    private fun getChimeraDir(context: Context): File {
        val deviceProtectedContext = if (Build.VERSION.SDK_INT >= 24 && !context.isDeviceProtectedStorage) {
            context.createDeviceProtectedStorageContext()
        } else {
            context
        }
        val oldPolicy = StrictMode.allowThreadDiskWrites()
        try {
            return deviceProtectedContext.getDir("chimera", Context.MODE_PRIVATE)
        } finally {
            StrictMode.setThreadPolicy(oldPolicy)
        }
    }

    private inline fun <T> StrictMode.ThreadPolicy.use(block: () -> T): T {
        val old = StrictMode.getThreadPolicy()
        StrictMode.setThreadPolicy(this)
        return try {
            block()
        } finally {
            StrictMode.setThreadPolicy(old)
        }
    }

    fun findChimeraBoundService(serviceName: String): ComponentRoute? {
        val targetName = serviceName.removePrefix(Constants.GMS_PACKAGE_NAME)
        return getChimeraManifest()
            .collections
            ?.serviceRoutes
            ?.find { it.containerName == targetName }
    }

    private fun findRoute(currentClassName: String): ComponentRoute? {
        val manifest = getChimeraManifest()
        val routes = manifest.collections?.activeRoutes ?: return null

        val variants = linkedSetOf<String>()
        variants += currentClassName

        val chimeraPrefix = getChimeraPrefix()
        if (chimeraPrefix.isNotEmpty()) {
            variants += currentClassName.removePrefix(chimeraPrefix)
        }

        val gmsPkg = Constants.GMS_PACKAGE_NAME
        if (currentClassName.startsWith("$gmsPkg.")) {
            val suffix = currentClassName.removePrefix(gmsPkg)
            variants += suffix
            variants += if (suffix.startsWith(".")) suffix else ".$suffix"
            variants += currentClassName.removePrefix("$gmsPkg.")
        }

        return routes.find { route -> route.containerName in variants }
    }

    fun findComponentByComponentName(currentClassName: String): ComponentRoute? =
        findRoute(currentClassName)

    fun findModuleByComponent(currentClassName: String): ChimeraModule? {
        val route = findRoute(currentClassName) ?: return null
        return getChimeraManifest().chimeraModules.find { it.moduleId == route.moduleId }
    }
    fun findModuleByModuleId(moduleId: String): ChimeraModule? {
        return getChimeraManifest().chimeraModules.find { it.moduleId == moduleId }
    }

    fun findModuleByModuleName(moduleName: String): ChimeraModule? {
        return getChimeraManifest().chimeraModules.find { it.moduleName == moduleName }
    }

    /** Returns the module identities persisted from successfully imported Chimera artifacts. */
    fun getRegisteredModules(): List<ChimeraModule> = getChimeraManifest().chimeraModules

    private fun resolveConfigFile(): File? {
        configFile?.let { return it }
        if (!AppContext.isInitialized()) return null
        return File(getChimeraDir(AppContext.get()), "chimera_manifest.pb").also { configFile = it }
    }

    private fun readConfigFile(file: File): ChimeraManifestStore {
        if (!file.exists()) return ChimeraManifestStore()
        return StrictMode.allowThreadDiskReads().use {
            file.inputStream().use { ChimeraManifestStore.ADAPTER.decode(it) }
        }
    }

    private inline fun <T> withConfigFileLock(file: File, block: () -> T): T {
        file.parentFile?.mkdirs()
        val lockFile = File(file.parentFile, "${file.name}.lock")
        return RandomAccessFile(lockFile, "rw").channel.use { channel ->
            channel.lock().use { block() }
        }
    }

    fun isApkPathReferenced(apkPath: String): Boolean {
        if (apkPath.isEmpty()) return false
        return getChimeraManifest().chimeraModules.any { it.installedApkPath == apkPath }
    }

    /** Resolve a module by its moduleId, falling back to moduleName — the loader/manager standard two-step lookup. */
    fun findModule(moduleId: String, moduleName: String): ChimeraModule? =
        findModuleByModuleId(moduleId) ?: findModuleByModuleName(moduleName)

    /**
     * Resolve an installed module by moduleName, ignoring stale placeholder entries that carry a
     * non-positive version. A feature sub-moduleId (e.g. mlkit_docscan_detect) has no config entry of its
     * own and the loader may persist a placeholder for it with version 0; this picks the highest valid
     * version so a version lookup for a sub-moduleId reports the real parent-module version.
     */
    fun findInstalledModuleByName(moduleName: String): ChimeraModule? {
        return getChimeraManifest().chimeraModules
            .filter {
                it.moduleName == moduleName && !it.installedApkPath.isNullOrEmpty() &&
                        (it.moduleVersion?.toIntOrNull() ?: 0) > 0
            }
            .maxByOrNull { it.moduleVersion?.toIntOrNull() ?: 0 }
    }

    /** List of (moduleName, moduleVersion) for registered (imported and persisted) modules, for UI display. For cross-process reads call [reload] first. */
    fun listInstalledModules(): List<Pair<String, String>> {
        val mods = getChimeraManifest().chimeraModules
        Log.d(TAG, "listInstalledModules: ${mods.size} modules in config, AppContext.init=${AppContext.isInitialized()}")
        return mods
            .filter { !it.moduleName.isNullOrEmpty() }
            .map { it.moduleName!! to it.moduleVersion.orEmpty() }
            .distinct()
    }

    /** Returns settings-safe capability status for the latest registered version of each module name. */
    fun listInstalledModuleStatuses(context: Context): List<InstalledModuleStatus> =
        getChimeraManifest().chimeraModules
            .filter { !it.moduleName.isNullOrEmpty() }
            .groupBy { it.moduleName!! }
            .map { (moduleName, entries) ->
                val module = entries.maxByOrNull { it.moduleVersion?.toLongOrNull() ?: 0L }!!
                val capability = module.moduleId?.let { moduleId ->
                    ChimeraApkManifestReader.readVerifiedCapabilities(context, module)
                        .filter { it.moduleId == moduleId }
                }.orEmpty()
                InstalledModuleStatus(
                    moduleName = moduleName,
                    moduleVersion = module.moduleVersion.orEmpty(),
                    capabilityStatus = ChimeraCapabilitySupport.classify(context, capability),
                )
            }

    fun featureConfigByKey(key: String?): FeatureDescriptor? {
        if (key == null) {
            return null
        }

        val result = getChimeraManifest().featureDescriptors.find { it.featureName == key }
        if (result == null) {
            Log.d(TAG, "featureConfigByKey($key): NOT FOUND (total features: ${getChimeraManifest().featureDescriptors.size})")
        }
        return result
    }

    fun getChimeraPrefix(): String {
        return getChimeraManifest().collections?.chimeraClassNamePrefix ?: ""
    }

    /**
     * Registers downloaded artifacts by identities from their signed ChimeraManifest.pb. The immutable .mods
     * mapping name is intentionally not an ownership key: known IDs use [DynamicModuleRegistry]'s canonical
     * name and unknown future IDs use the signed moduleId itself.
     */
    fun updateModuleDownload(moduleInfos: List<ModuleInfo>): ChimeraManifestStore {
        check(AppContext.isInitialized() && DynamicModuleSettings.isAvailable(AppContext.get())) {
            "Dynamic modules are unavailable on this device or disabled by the user"
        }
        return updateConfig(autoSave = true) { oldConfig ->
            val newConfig = oldConfig.newBuilder()
            val newCollections = oldConfig.collections?.newBuilder() ?: ChimeraModuleCollections.Builder()
            val modules = oldConfig.chimeraModules.map { it.newBuilder().build() }.toMutableList()

            for (moduleInfo in moduleInfos) {
                val apkPath = moduleInfo.source?.toUri()?.path.orEmpty()
                val manifests = readApkChimeraManifests(apkPath)
                if (apkPath.isEmpty() || manifests.isNullOrEmpty()) {
                    throw IllegalArgumentException(
                        "Artifact has no readable ChimeraManifest: ${moduleInfo.source}"
                    )
                }
                require(manifests.all { !it.moduleId.isNullOrEmpty() && (it.moduleVersion ?: 0) > 0 }) {
                    "Artifact contains an invalid Chimera identity: ${moduleInfo.source}"
                }

                for (manifest in manifests) {
                    val moduleId = manifest.moduleId?.takeIf { it.isNotEmpty() } ?: continue
                    val moduleName = DynamicModuleRegistry.canonicalModuleName(moduleId)
                    val registeredVersion = manifest.moduleVersion
                        ?.takeIf { it > 0 }
                        ?.toString()
                        ?: moduleInfo.module_version
                    val incomingVersion = registeredVersion?.toLongOrNull()
                    val index = modules.indexOfFirst { it.moduleId == moduleId }
                    val oldEntry = index.takeIf { it >= 0 }?.let(modules::get)
                    val oldVersion = oldEntry?.moduleVersion?.toLongOrNull()
                    if (oldVersion != null && incomingVersion != null && oldVersion > incomingVersion) {
                        Log.i(TAG, "Keeping newer installed module $moduleId v$oldVersion over v$incomingVersion")
                        continue
                    }

                    val oldManifest = oldEntry?.installedApkPath
                        ?.takeIf { it.isNotEmpty() }
                        ?.let(::readApkChimeraManifests)
                        .orEmpty()
                        .firstOrNull { it.moduleId == moduleId }
                    val oldFeatures = oldManifest?.featureDescriptors.orEmpty()
                        .mapNotNull { it.featureName?.takeIf(String::isNotEmpty) }
                        .toSet()
                    val replacementFeatures = manifest.featureDescriptors
                        .mapNotNull { it.featureName?.takeIf(String::isNotEmpty) }
                        .toSet()
                    val featuresOwnedElsewhere = featureNamesFromInstalledEntries(
                        modules.filter { it.moduleId != moduleId }
                    )
                    val removableFeatures = oldFeatures - replacementFeatures - featuresOwnedElsewhere
                    if (removableFeatures.isNotEmpty()) {
                        newConfig.featureDescriptors = newConfig.featureDescriptors.filterNot {
                            it.featureName in removableFeatures
                        }
                    }

                    val replacement = (oldEntry?.newBuilder() ?: ChimeraModule.Builder()).apply {
                        this.moduleId = moduleId
                        this.moduleName = moduleName
                        moduleVersion = registeredVersion
                        installedApkPath = apkPath
                        // The APK-local signed manifest declares the host ModuleApi. Do not infer it
                        // from component bindings: a module can contain several initializer classes.
                        moduleApiClassname = manifest.requiredApis.orEmpty()
                        apkSha256 = moduleInfo.sha256_hash
                    }.build()
                    if (index >= 0) modules[index] = replacement else modules.add(replacement)

                    if (newCollections.chimeraClassNamePrefix.isNullOrEmpty()) {
                        newCollections.chimeraClassNamePrefix = manifest.chimeraClassNamePrefix
                    }
                    newCollections.activeRoutes = newCollections.activeRoutes.filterNot { it.moduleId == moduleId }
                    newCollections.serviceRoutes = newCollections.serviceRoutes.filterNot { it.moduleId == moduleId }
                    newCollections.activeRoutes += manifest.activityBindings.map {
                        ComponentRoute.build {
                            containerName = it.containerName
                            moduleChimeraName = it.moduleChimeraName
                            this.moduleId = moduleId
                        }
                    }
                    newCollections.serviceRoutes += manifest.boundServiceBindings.map {
                        ComponentRoute.build {
                            containerName = it.containerName
                            moduleChimeraName = it.moduleChimeraName
                            this.moduleId = moduleId
                        }
                    }
                    manifest.featureDescriptors.forEach { descriptor ->
                        newConfig.featureDescriptors = newConfig.featureDescriptors.filterNot {
                            it.featureName == descriptor.featureName
                        }
                        newConfig.featureDescriptors += FeatureDescriptor.build {
                            featureName = descriptor.featureName
                            featureVersion = descriptor.featureVersion
                        }
                    }
                }
            }

            newConfig.chimeraModules = modules
            newConfig.collections(newCollections.build())
            newConfig.build()
        }
    }

    fun removeModuleMetadata(moduleName: String, apkPathHint: String? = null): RemovedModuleMetadata {
        var removed = RemovedModuleMetadata()
        val updatedConfig = updateConfig(autoSave = true) { oldConfig ->
            val newConfig = oldConfig.newBuilder()
            val newCollections = oldConfig.collections?.newBuilder() ?: ChimeraModuleCollections.Builder()
            val newChimeraModules = oldConfig.chimeraModules.map { it.newBuilder().build() }.toMutableList()

            removed = cleanupModuleMetadata(
                newConfig,
                newCollections,
                newChimeraModules,
                moduleName,
                replacementManifests = emptyList(),
                apkPathHints = listOfNotNull(apkPathHint)
            )

            newConfig.chimeraModules = newChimeraModules
            newConfig.collections(newCollections.build())
            newConfig.build()
        }
        return if (updatedConfig.chimeraModules.any { it.moduleName == moduleName }) {
            Log.e(TAG, "Module metadata removal was not persisted for $moduleName")
            removed.copy(persisted = false)
        } else {
            removed
        }
    }

    private fun cleanupModuleMetadata(
        newConfig: ChimeraManifestStore.Builder,
        newCollections: ChimeraModuleCollections.Builder,
        newChimeraModules: MutableList<ChimeraModule>,
        moduleName: String,
        replacementManifests: List<ChimeraModuleManifest>,
        apkPathHints: List<String> = emptyList()
    ): RemovedModuleMetadata {
        if (moduleName.isEmpty()) return RemovedModuleMetadata()

        val oldEntries = newChimeraModules.filter { it.moduleName == moduleName }
        val oldPaths = (oldEntries.mapNotNull { it.installedApkPath?.takeIf { path -> path.isNotEmpty() } } +
                apkPathHints.filter { it.isNotEmpty() }).distinct()
        val oldModuleIds = oldEntries.mapNotNull { it.moduleId?.takeIf { id -> id.isNotEmpty() } }.toSet()
        val oldManifests = oldPaths.flatMap { path ->
            readApkChimeraManifests(path).orEmpty().filter { it.moduleId in oldModuleIds }
        }

        if (oldEntries.isEmpty() && oldModuleIds.isEmpty() && oldPaths.isEmpty()) {
            return RemovedModuleMetadata()
        }

        val replacementModuleIds = replacementManifests.mapNotNull { it.moduleId?.takeIf { id -> id.isNotEmpty() } }.toSet()
        val staleModuleIds = if (replacementModuleIds.isEmpty()) oldModuleIds else oldModuleIds - replacementModuleIds

        if (replacementModuleIds.isEmpty()) {
            newChimeraModules.removeAll { it.moduleName == moduleName }
        } else {
            newChimeraModules.removeAll { it.moduleName == moduleName && it.moduleId !in replacementModuleIds }
        }

        if (staleModuleIds.isNotEmpty()) {
            newCollections.activeRoutes = newCollections.activeRoutes.filterNot { it.moduleId in staleModuleIds }
            newCollections.serviceRoutes = newCollections.serviceRoutes.filterNot { it.moduleId in staleModuleIds }
        }

        val oldFeatureNames = featureNamesFromManifests(oldManifests)
        if (oldFeatureNames.isNotEmpty()) {
            val replacementFeatureNames = featureNamesFromManifests(replacementManifests)
            val otherInstalledFeatureNames = featureNamesFromInstalledEntries(
                newChimeraModules.filter { it.moduleName != moduleName }
            )
            val removableFeatureNames = oldFeatureNames - replacementFeatureNames - otherInstalledFeatureNames
            if (removableFeatureNames.isNotEmpty()) {
                newConfig.featureDescriptors = newConfig.featureDescriptors.filterNot {
                    it.featureName in removableFeatureNames
                }
                Log.d(TAG, "Removed stale feature descriptors for $moduleName: $removableFeatureNames")
            }
        }

        return RemovedModuleMetadata(oldModuleIds, oldPaths.toSet())
    }

    private fun featureNamesFromInstalledEntries(entries: Iterable<ChimeraModule>): Set<String> {
        val idsByPath = entries
            .mapNotNull { entry ->
                val path = entry.installedApkPath?.takeIf(String::isNotEmpty) ?: return@mapNotNull null
                val moduleId = entry.moduleId?.takeIf(String::isNotEmpty) ?: return@mapNotNull null
                path to moduleId
            }
            .groupBy({ it.first }, { it.second })
        return idsByPath.flatMap { (path, moduleIds) ->
            readApkChimeraManifests(path).orEmpty().filter { it.moduleId in moduleIds }
        }.let(::featureNamesFromManifests)
    }

    private fun featureNamesFromManifests(manifests: Iterable<ChimeraModuleManifest>): Set<String> {
        return manifests
            .flatMap { it.featureDescriptors }
            .mapNotNull { it.featureName?.takeIf { name -> name.isNotEmpty() } }
            .toSet()
    }

    private fun readApkChimeraManifests(zipPath: String): List<ChimeraModuleManifest>? =
        ChimeraApkManifestReader.readModuleManifests(File(zipPath))
}
