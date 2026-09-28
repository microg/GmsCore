/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.chimera

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.Process
import android.util.Log
import androidx.core.os.bundleOf
import com.google.android.chimera.config.ChimeraConfigManager
import com.google.android.chimera.config.ChimeraModule
import com.google.android.chimera.config.ChimeraStorage
import com.google.android.chimera.config.DynamicModuleSettings
import com.google.android.chimera.config.FeatureCheckUtils
import com.google.android.chimera.config.FeatureMessage
import com.google.android.chimera.config.FeaturesMessage
import com.google.android.chimera.config.ModuleManager
import com.google.android.chimera.config.ModuleDownloadRegistry
import org.microg.gms.DummyService
import org.microg.gms.common.GmsService
import java.io.File
import java.io.FileNotFoundException

class ServiceProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        Log.d(TAG, "onCreate")
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        when (method) {
            "featureCheckCall" -> return featureCheckCall(extras)
            "featureFetchCall" -> return featureFetchCall(extras)
            "serviceIntentCall" -> {
                val serviceAction = extras?.getString("serviceActionBundleKey") ?: return null
                val context = context!!
                var intent = Intent(serviceAction).apply { `package` = context.packageName }
                var resolveInfo = context.packageManager.resolveService(intent, 0)
                if (resolveInfo == null && GmsService.byAction(serviceAction).ACTION != null) {
                    // Try again with action as defined in GmsService
                    val overrideAction = GmsService.byAction(serviceAction).ACTION
                    val overrideActionIntent = Intent(overrideAction).apply { `package` = context.packageName }
                    resolveInfo = context.packageManager.resolveService(overrideActionIntent, 0)
                }
                if (resolveInfo != null) {
                    intent.setClassName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name)
                } else {
                    intent.setClass(context, DummyService::class.java)
                }
                Log.d(TAG, "$method: $serviceAction -> $intent")
                return bundleOf(
                        "serviceResponseIntentKey" to intent
                )
            }
            "removeModule" -> {
                val moduleName = arg ?: return null
                val ctx = context ?: return null
                val callingUid = Binder.getCallingUid()
                if (callingUid != Process.myUid()) {
                    Log.w(TAG, "removeModule rejected from uid=$callingUid")
                    return bundleOf("removed" to false)
                }
                val ok = ChimeraModuleRemover.remove(ctx, moduleName)
                return bundleOf("removed" to ok)
            }
            else -> {
                Log.d(TAG, "$method: $arg, $extras")
                return super.call(method, arg, extras)
            }
        }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        Log.d(TAG, "query: $uri")
        try {
            val ctx = context ?: return MatrixCursor(COLUMNS)
            if (!DynamicModuleSettings.isAvailable(ctx)) return MatrixCursor(COLUMNS)
            if (!isExpectedProviderUri(uri)) {
                Log.w(TAG, "query: rejected unexpected authority=${uri.authority}")
                return MatrixCursor(COLUMNS)
            }
            runCatching { ChimeraConfigManager.reload() }
            val configLastModified = ChimeraConfigManager.getConfigLastModified(ctx)
            val pathSegments = uri.pathSegments
            if (pathSegments.size == 2 && (pathSegments[0] == "api" || pathSegments[0] == "api_force_staging")) {
                val moduleId = pathSegments[1]
                if (!isValidModuleId(moduleId)) {
                    Log.w(TAG, "query: rejected invalid moduleId=$moduleId")
                    return MatrixCursor(COLUMNS)
                }
                // First check installed modules (dynamic chimera_manifest.pb, actual version/path)
                val chimeraModule = ChimeraConfigManager.findModuleByModuleId(moduleId)
                val installedFile = resolveInstalledApkFile(chimeraModule)
                if (chimeraModule != null && installedFile != null) {
                    val version = chimeraModule.moduleVersion?.toIntOrNull() ?: 1
                    return buildModuleQueryCursor(
                        version,
                        installedFile.absolutePath,
                        configLastModified,
                        chimeraModule.apkSha256.orEmpty()
                    )
                }
                Log.w(TAG, "query: module not available: $moduleId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "query: error processing module query for $uri", e)
        }
        return MatrixCursor(COLUMNS)
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val ctx = context ?: rejectOpenFile(uri, "missing context")
        if (!isExpectedProviderUri(uri)) {
            rejectOpenFile(uri, "unexpected authority=${uri.authority}")
        }
        if (mode != "r") {
            rejectOpenFile(uri, "non-readonly mode=$mode")
        }
        if (!DynamicModuleSettings.isAvailable(ctx)) {
            rejectOpenFile(uri, "dynamic modules unavailable")
        }
        runCatching { ChimeraConfigManager.reload() }
        val pathSegments = uri.pathSegments
        if (pathSegments.size == 2 && (pathSegments[0] == "api" || pathSegments[0] == "api_force_staging") &&
            pathSegments[1].all { it.isDigit() }) {
            val configFile = ChimeraConfigManager.getConfigFile(ctx)
            if (!configFile.isFile || !configFile.canRead()) rejectOpenFile(uri, "config file not available")
            Log.d(TAG, "openFile: serving Chimera config ${configFile.absolutePath}")
            return ParcelFileDescriptor.open(configFile, ParcelFileDescriptor.MODE_READ_ONLY)
        }
        if (pathSegments.size != 2 || pathSegments[0] != "module_apk") {
            rejectOpenFile(uri, "invalid path")
        }
        val moduleId = pathSegments[1]
        if (!isValidModuleId(moduleId)) {
            rejectOpenFile(uri, "invalid moduleId=$moduleId")
        }
        Log.d(TAG, "openFile: serving APK for moduleId=$moduleId")

        val apkFile = findModuleApkFile(moduleId)
        if (apkFile != null) {
            Log.d(TAG, "openFile: returning FD for ${apkFile.absolutePath}")
            return ParcelFileDescriptor.open(apkFile, ParcelFileDescriptor.MODE_READ_ONLY)
        }
        rejectOpenFile(uri, "APK not found for moduleId=$moduleId")
    }

    private fun featureCheckCall(extras: Bundle?): Bundle {
        val out = Bundle()
        val bytes = extras?.getByteArray("featuresBundleKey")
        if (bytes == null) {
            Log.e(TAG, "featureCheckCall: missing featuresBundleKey")
            out.putInt("featuresResult", ModuleManager.FEATURE_CHECK_ERROR)
            return out
        }
        val request = try {
            FeaturesMessage.ADAPTER.decode(bytes)
        } catch (e: Exception) {
            Log.e(TAG, "featureCheckCall: malformed feature request", e)
            out.putInt("featuresResult", ModuleManager.FEATURE_CHECK_ERROR)
            return out
        }
        runCatching { ChimeraConfigManager.reload() }
        out.putInt(
            "featuresResult",
            FeatureCheckUtils.checkFeatureMessages(
                request.features,
                allowStaticRegistry = false,
                allowDynamicModules = context?.let(DynamicModuleSettings::isAvailable) == true
            )
        )
        return out
    }

    private fun featureFetchCall(extras: Bundle?): Bundle {
        val out = Bundle()
        val names = extras?.getStringArray("featureNamesBundleKey")
        if (names.isNullOrEmpty()) {
            Log.e(TAG, "featureFetchCall: missing featureNamesBundleKey")
            out.putInt("featuresResult", ModuleManager.FEATURE_CHECK_ERROR)
            return out
        }
        val dynamicModulesEnabled = context?.let { DynamicModuleSettings.isAvailable(it) } == true
        if (dynamicModulesEnabled) runCatching { ChimeraConfigManager.reload() }
        val messages = names.mapNotNull { name ->
            if (!dynamicModulesEnabled) {
                return@mapNotNull null
            }
            ChimeraConfigManager.featureConfigByKey(name)?.let { desc ->
                val featureName = desc.featureName ?: return@let null
                FeatureMessage.Builder()
                    .featureName(featureName)
                    .featureVersion(desc.featureVersion)
                    .build()
            }
        }
        out.putByteArray("featuresResponseListKey", FeaturesMessage.Builder().features(messages).build().encode())
        out.putInt("featuresResult", ModuleManager.FEATURE_CHECK_SUCCESS)
        return out
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        Log.d(TAG, "insert: $uri, $values")
        return uri
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        Log.d(TAG, "update: $uri, $values, $selection, $selectionArgs")
        return 0
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        Log.d(TAG, "delete: $uri, $selection, $selectionArgs")
        return 0
    }

    override fun getType(uri: Uri): String {
        Log.d(TAG, "getType: $uri")
        return "vnd.android.cursor.item/com.google.android.gms.chimera"
    }

    private fun findModuleApkFile(moduleId: String): File? {
        val chimeraModule = ChimeraConfigManager.findModuleByModuleId(moduleId)
        resolveInstalledApkFile(chimeraModule)?.let { file ->
            Log.d(TAG, "findModuleApkFile($moduleId): found via config: ${file.absolutePath}")
            return file
        }
        return null
    }

    private fun resolveInstalledApkFile(chimeraModule: ChimeraModule?): File? {
        val ctx = context ?: return null
        val installedApkPath = chimeraModule?.installedApkPath?.takeIf { it.isNotEmpty() } ?: return null
        // A signed APK may own several moduleIds whose canonical names differ. Its storage filename is
        // only an artifact label, so a config-owned path must not be rejected by a filename/name mismatch.
        return ChimeraStorage.verifiedModuleApk(
            context = ctx,
            file = File(installedApkPath),
            expectedModuleName = null,
            expectedSha256 = chimeraModule.apkSha256,
        )
    }

    private fun rejectOpenFile(uri: Uri, reason: String): Nothing {
        Log.w(TAG, "openFile rejected: $reason for $uri")
        throw FileNotFoundException("No module APK for $uri ($reason)")
    }

    private fun isExpectedProviderUri(uri: Uri): Boolean = uri.authority == EXPECTED_AUTHORITY

    private fun isValidModuleId(value: String): Boolean = isSafeModuleToken(value)

    private fun isSafeModuleToken(value: String): Boolean {
        return value.isNotEmpty() && value.length <= MAX_MODULE_TOKEN_LENGTH && MODULE_TOKEN_RE.matches(value)
    }

    companion object {
        private const val TAG = "ServiceProvider"
        private const val EXPECTED_AUTHORITY = "com.google.android.gms.chimera"
        private const val MAX_MODULE_TOKEN_LENGTH = 200
        private val MODULE_TOKEN_RE = Regex("[A-Za-z0-9_.-]+")
        private val COLUMNS = arrayOf(
            "version", "apkDesc", "loaderPath", "apkDescStr", "moduleConfig",
            "moduleDescriptorIndex", "configLastModTime", "loaderVersion",
            "requestStats", "dynamiteFlags", "disableStandaloneDynamiteLoader2", "apkSha256"
        )

        private fun buildModuleQueryCursor(
            version: Int,
            apkPath: String?,
            configLastModified: Long,
            apkSha256: String,
        ): Cursor {
            val cursor = MatrixCursor(COLUMNS, 1)
            val row = arrayOfNulls<Any>(COLUMNS.size)
            row[0] = version.toLong()         // version
            row[1] = null                      // apkDesc (blob)
            row[2] = apkPath ?: ""             // loaderPath
            row[3] = apkPath ?: ""             // apkDescStr
            row[4] = null                      // moduleConfig
            row[5] = 0L                        // moduleDescriptorIndex
            row[6] = configLastModified        // configLastModTime
            row[7] = 3L                        // loaderVersion
            row[8] = null                      // requestStats
            row[9] = null                      // dynamiteFlags
            row[10] = 0L                       // disableStandaloneDynamiteLoader2
            row[11] = apkSha256                 // content identity for client-side cache invalidation
            cursor.addRow(row)
            return cursor
        }
    }
}
