/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.config

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.StringRes
import com.google.android.chimera.component.ModuleDownloadActivity
import com.google.android.chimera.component.TAG
import com.google.android.chimera.config.registry.DynamicModuleRegistry
import com.google.android.gms.common.Feature
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import com.google.android.gms.common.moduleinstall.internal.ApiFeatureRequest
import org.microg.gms.chimera.core.R
import java.io.File
import java.util.UUID

/** Resolves requested Chimera features to independently downloadable `.mods` release assets. */
object ModuleDownloadRegistry {
    /** [ApiFeatureRequest] retained by the internal module authorization activity. */
    const val EXTRA_API_FEATURE_REQUEST =
        "com.google.android.chimera.config.EXTRA_API_FEATURE_REQUEST"
    private const val FEATURE_MLKIT_DOCUMENT_SCANNER = "mlkit.docscan.ui"
    private const val COMPONENT_MLKIT_DOCUMENT_SCANNER =
        "com.google.android.gms.mlkit.docscan.ui.DocumentScanningActivity"
    private const val MODULE_RELEASE_BASE_URL =
        "https://github.com/david200101/gms-chimera/releases/download/v1.0.0/"
    private const val DOCUMENT_SCANNER_BUNDLE = "mlkit-document-scanner.mods"
    private const val DOCUMENT_SCANNER_X86_BUNDLE = "mlkit-document-scanner_x86.mods"

    data class RequestedFeature(
        val name: String,
        val minVersion: Long = 0L,
    )

    data class PermissionRequirement(
        val permission: String,
        @StringRes val labelRes: Int,
        @StringRes val descriptionRes: Int
    )

    data class DownloadableModule(
        val catalogId: String,
        @StringRes val displayNameRes: Int,
        val downloadUrl: String,
        val requestedFeatures: Set<String>,
        val requiredPermissions: List<PermissionRequirement>,
        /** Every signed Chimera module ID that must be installed before the feature may be resumed. */
        val requiredModuleIds: Set<String> = emptySet(),
        /** Additional component routes required by this module, independent of the component that triggered it. */
        val requiredComponentClassNames: Set<String> = emptySet(),
    )

    private val modules = listOf(
        DownloadableModule(
            catalogId = "mlkit-document-scanner",
            displayNameRes = R.string.chimera_module_document_scanner_name,
            downloadUrl = documentScannerDownloadUrl(),
            requestedFeatures = setOf(FEATURE_MLKIT_DOCUMENT_SCANNER),
            requiredModuleIds = DynamicModuleRegistry.MLKIT_DOCUMENT_SCANNER_MODULE_IDS,
            requiredComponentClassNames = setOf(COMPONENT_MLKIT_DOCUMENT_SCANNER),
            requiredPermissions = listOf(
                PermissionRequirement(
                    permission = Manifest.permission.CAMERA,
                    labelRes = R.string.chimera_permission_camera_label,
                    descriptionRes = R.string.chimera_permission_camera_description
                )
            )
        )
    )

    private fun documentScannerDownloadUrl(): String {
        val supportedAbis = if (Build.VERSION.SDK_INT >= 21) {
            Build.SUPPORTED_ABIS.asIterable()
        } else {
            listOfNotNull(Build.CPU_ABI, Build.CPU_ABI2.takeIf(String::isNotEmpty))
        }
        val bundle = if (supportedAbis.any { it == "x86" || it == "x86_64" }) {
            DOCUMENT_SCANNER_X86_BUNDLE
        } else {
            DOCUMENT_SCANNER_BUNDLE
        }
        return MODULE_RELEASE_BASE_URL + bundle
    }

    /** Returns every independently downloadable module needed by [requestedFeatures], in catalog order. */
    @JvmStatic
    fun resolveModules(requestedFeatures: Iterable<RequestedFeature>): List<DownloadableModule> {
        val requestedNames = normalizeRequestedFeatures(requestedFeatures).mapTo(mutableSetOf()) { it.name }
        return modules.filter { candidate ->
            candidate.requestedFeatures.any(requestedNames::contains)
        }
    }

    /** True for a feature that is provided by a known, independently downloadable module. */
    @JvmStatic
    fun isKnownDynamicFeature(featureName: String?): Boolean {
        if (featureName.isNullOrEmpty()) return false
        return modules.any { featureName in it.requestedFeatures }
    }

    /** Creates the internal permission-and-download page for a known module request. */
    @JvmStatic
    fun createModuleDownloadIntent(context: Context, requestedFeatureNames: Iterable<String>): Intent? {
        return createModuleDownloadIntentForRequests(
            context,
            requestedFeatureNames.map(::RequestedFeature),
        )
    }

    /** Creates the internal permission-and-download page while retaining requested minimum versions. */
    @JvmStatic
    fun createModuleDownloadIntentForRequests(
        context: Context,
        requestedFeatures: Iterable<RequestedFeature>,
    ): Intent? {
        val features = normalizeRequestedFeatures(requestedFeatures)
        val availabilityRequest = ApiFeatureRequest().apply {
            this.features = features.map { Feature(it.name, it.minVersion) }
        }
        return createModuleDownloadIntentForRequests(context, features, availabilityRequest)
    }

    /**
     * Creates the internal permission-and-download page while retaining the original API request
     * for the external download application's post-download availability check.
     */
    @JvmStatic
    fun createModuleDownloadIntentForRequests(
        context: Context,
        requestedFeatures: Iterable<RequestedFeature>,
        apiFeatureRequest: ApiFeatureRequest?,
    ): Intent? {
        if (!DynamicModuleSettings.isAvailable(context)) return null
        val features = normalizeRequestedFeatures(requestedFeatures)
        if (features.isEmpty() ||
            features.any { it.minVersion < -1L || !isKnownDynamicFeature(it.name) }
        ) return null
        return Intent(context, ModuleDownloadActivity::class.java).apply {
            putRequestedFeatures(features)
            apiFeatureRequest?.let { putExtra(EXTRA_API_FEATURE_REQUEST, SafeParcelableSerializer.serializeToBytes(it)) }
            putRequestIdentity("download")
            putContainerActivity(context)
        }
    }

    @JvmStatic
    fun createModuleDownloadIntent(context: Context, requestedFeatureNames: String?): Intent? {
        return createModuleDownloadIntent(context, splitFeatureNames(requestedFeatureNames))
    }

    /** Creates the permission page used when an installed module was launched after permission revocation. */
    @JvmStatic
    fun createModulePermissionIntent(
        context: Context,
        requestedFeatureNames: String?
    ): Intent? {
        if (!DynamicModuleSettings.isAvailable(context)) return null
        val features = splitFeatureNames(requestedFeatureNames)
            .map(::RequestedFeature)
        if (features.isEmpty() || features.any { !isKnownDynamicFeature(it.name) }) return null
        return Intent(context, ModuleDownloadActivity::class.java).apply {
            putRequestedFeatures(features)
            putRequestIdentity("permission")
            putExtra(
                ModuleDownloadActivity.EXTRA_AFTER_AUTHORIZATION_ACTION,
                ModuleDownloadActivity.ACTION_CONTINUE_MODULE
            )
            putContainerActivity(context)
        }
    }

    @JvmStatic
    fun requestedFeatureNamesForActivity(context: Context, componentClassName: String): String? {
        modules.firstOrNull { componentClassName in it.requiredComponentClassNames }
            ?.requestedFeatures
            ?.joinToString(",")
            ?.let { return it }
        return try {
            context.packageManager.getActivityInfo(
                ComponentName(context, componentClassName),
                PackageManager.MATCH_DISABLED_COMPONENTS or PackageManager.GET_META_DATA
            ).metaData?.getString("chimera.requested_features")
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    @JvmStatic
    fun hasMissingPermissions(context: Context, requestedFeatureNames: String?): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        val resolvedModules = resolveModules(splitFeatureNames(requestedFeatureNames).map(::RequestedFeature))
        return resolvedModules.any { module ->
            module.requiredPermissions.any {
                context.checkSelfPermission(it.permission) != PackageManager.PERMISSION_GRANTED
            }
        }
    }

    /**
     * Re-reads the cross-process Chimera manifest and verifies the requested feature versions plus
     * every required module artifact and component route. This is the authoritative gate used after
     * a `.mods` import; the import completion broadcast itself is deliberately only a hint.
     */
    @JvmStatic
    fun isModuleInstalled(
        context: Context,
        module: DownloadableModule,
        requestedFeatures: Iterable<RequestedFeature>,
        componentClassName: String?,
    ): Boolean {
        return runCatching {
            if (!DynamicModuleSettings.isAvailable(context)) return@runCatching false
            ChimeraModuleBootstrap.ensureInitialized(context)
            ChimeraConfigManager.reload()
            val relevantFeatures = normalizeRequestedFeatures(requestedFeatures)
                .filter { it.name in module.requestedFeatures }
            if (relevantFeatures.isEmpty()) return@runCatching false
            val featuresAvailable = relevantFeatures.all { requestedFeature ->
                val descriptor = ChimeraConfigManager.featureConfigByKey(requestedFeature.name)
                    ?: return@all false
                isVersionSatisfied(descriptor.featureVersion, requestedFeature.minVersion)
            }
            val moduleIdsAvailable = module.requiredModuleIds.all { moduleId ->
                verifiedInstalledArtifact(context, ChimeraConfigManager.findModuleByModuleId(moduleId))
            }
            val requiredComponents = buildSet {
                addAll(module.requiredComponentClassNames)
                componentClassName?.takeIf { it.isNotEmpty() }?.let(::add)
            }
            val componentsAvailable = requiredComponents.all { className ->
                verifiedInstalledArtifact(context, ChimeraConfigManager.findModuleByComponent(className))
            }
            featuresAvailable && moduleIdsAvailable && componentsAvailable
        }.getOrDefault(false)
    }

    @JvmStatic
    fun isModuleInstalled(
        context: Context,
        module: DownloadableModule,
        componentClassName: String?,
    ): Boolean = isModuleInstalled(
        context,
        module,
        module.requestedFeatures.map(::RequestedFeature),
        componentClassName,
    )

    /**
     * Creates the external HTTPS intent without app-private parcelables. Generic system resolvers
     * cannot load GMS classes; the request remains persisted inside GMS until import completes.
     */
    @JvmStatic
    fun createExternalDownloadIntent(
        downloadUrl: String,
        apiFeatureRequest: ApiFeatureRequest?,
    ): Intent {
        require(modules.any { it.downloadUrl == downloadUrl }) { "Unknown module download URL" }
        val uri = Uri.parse(downloadUrl)
        require(uri.scheme == "https") { "Module download URL must use HTTPS" }
        return Intent(Intent.ACTION_VIEW, uri).apply {
            apiFeatureRequest?.let { putExtra(EXTRA_API_FEATURE_REQUEST, SafeParcelableSerializer.serializeToBytes(it)) }
            Log.d(TAG, "Creating external module download intent for ${apiFeatureRequest?.features?.size} feature entries")
        }
    }

    /** Builds a system chooser that reports an actual target selection when the platform supports it. */
    @JvmStatic
    fun createExternalDownloadChooserIntent(
        downloadUrl: String,
        title: CharSequence?,
        apiFeatureRequest: ApiFeatureRequest?,
        selectionCallback: IntentSender?,
    ): Intent {
        val downloadIntent = createExternalDownloadIntent(downloadUrl, apiFeatureRequest)
        return if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 &&
            selectionCallback != null
        ) {
            Intent.createChooser(downloadIntent, title, selectionCallback)
        } else {
            Intent.createChooser(downloadIntent, title)
        }
    }

    private fun splitFeatureNames(requestedFeatureNames: String?): List<String> {
        return requestedFeatureNames.orEmpty().split(',', ';')
    }

    fun requestedFeaturesFromIntent(intent: Intent): List<RequestedFeature> {
        val names = intent.getStringArrayListExtra(ModuleDownloadActivity.EXTRA_REQUESTED_FEATURE_NAMES)
            .orEmpty()
        val versions = intent.getLongArrayExtra(ModuleDownloadActivity.EXTRA_REQUESTED_FEATURE_VERSIONS)
        return normalizeRequestedFeatures(names.mapIndexed { index, name ->
            RequestedFeature(name, versions?.getOrNull(index) ?: 0L)
        })
    }

    private fun normalizeRequestedFeatures(
        requestedFeatures: Iterable<RequestedFeature>,
    ): List<RequestedFeature> {
        val normalized = linkedMapOf<String, Long>()
        requestedFeatures.forEach { requestedFeature ->
            val name = requestedFeature.name.trim()
            if (name.isEmpty()) return@forEach
            normalized[name] = normalized[name]
                ?.let { mergeMinimumVersion(it, requestedFeature.minVersion) }
                ?: requestedFeature.minVersion
        }
        return normalized.map { (name, minVersion) -> RequestedFeature(name, minVersion) }
    }

    private fun mergeMinimumVersion(first: Long, second: Long): Long = when {
        first == 0L -> second
        second == 0L -> first
        first == -1L -> second
        second == -1L -> first
        else -> maxOf(first, second)
    }

    private fun isVersionSatisfied(availableVersion: Long?, requestedVersion: Long): Boolean {
        if (requestedVersion == 0L) return true
        if (availableVersion == null || availableVersion < 0L || requestedVersion < -1L) return false
        return requestedVersion == -1L || availableVersion >= requestedVersion
    }

    private fun verifiedInstalledArtifact(context: Context, module: ChimeraModule?): Boolean {
        val path = module?.installedApkPath?.takeIf { it.isNotEmpty() } ?: return false
        return ChimeraStorage.verifiedModuleApk(
            context = context,
            file = File(path),
            expectedModuleName = null,
            expectedSha256 = module.apkSha256,
        ) != null
    }

    private fun Intent.putRequestedFeatures(features: List<RequestedFeature>) {
        putStringArrayListExtra(
            ModuleDownloadActivity.EXTRA_REQUESTED_FEATURE_NAMES,
            ArrayList(features.map(RequestedFeature::name)),
        )
        putExtra(
            ModuleDownloadActivity.EXTRA_REQUESTED_FEATURE_VERSIONS,
            features.map(RequestedFeature::minVersion).toLongArray(),
        )
    }

    /** Makes otherwise identical PendingIntents independent; extras are not part of PendingIntent identity. */
    private fun Intent.putRequestIdentity(action: String) {
        val requestId = UUID.randomUUID().toString()
        putExtra(ModuleDownloadActivity.EXTRA_REQUEST_ID, requestId)
        data = Uri.Builder()
            .scheme("chimera-module")
            .authority(action)
            .appendPath(requestId)
            .build()
    }

    /**
     * Component verification is meaningful only for a Chimera Activity request. ModuleInstallService
     * also creates this page; persisting its Service class here would make an otherwise successful
     * import fail the Activity-route check forever.
     */
    private fun Intent.putContainerActivity(context: Context) {
        if (context is Activity) {
            putExtra(
                ModuleDownloadActivity.EXTRA_CONTAINER_COMPONENT_CLASS_NAME,
                context.javaClass.name
            )
        }
    }
}
