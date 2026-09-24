/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.component

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import com.google.android.chimera.config.DynamicModuleSettings
import com.google.android.chimera.config.ModuleDownloadRegistry
import com.google.android.gms.common.Feature
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import com.google.android.gms.common.moduleinstall.internal.ApiFeatureRequest
import org.microg.gms.chimera.core.R
import java.util.UUID

const val TAG = "ChimeraModuleDownload"
private const val REQUEST_MODULE_PERMISSIONS = 1
private const val STATE_PERMISSION_REQUESTED = "permission_requested"
private const val STATE_SETTINGS_REQUIRED = "settings_required"
private const val STATE_WAITING_FOR_SETTINGS = "waiting_for_settings"
private const val STATE_WAITING_FOR_IMPORT = "waiting_for_import"
private const val STATE_WAITING_FOR_DOWNLOAD_SELECTION = "waiting_for_download_selection"
private const val STATE_AFTER_AUTHORIZATION_ACTION = "after_authorization_action"
private const val STATE_STATUS_MESSAGE = "status_message"
private const val STATE_REQUEST_ID = "request_id"
private const val STATE_CURRENT_MODULE_INDEX = "current_module_index"

/**
 * Shows a module action and its permission requirements in one page. The requested action is only
 * performed after every permission registered for the module has been granted.
 */
class ModuleDownloadActivity : Activity() {
    private lateinit var requestId: String
    private lateinit var requestedFeatures: List<ModuleDownloadRegistry.RequestedFeature>
    private lateinit var modules: List<ModuleDownloadRegistry.DownloadableModule>
    private lateinit var module: ModuleDownloadRegistry.DownloadableModule
    private var currentModuleIndex = 0
    private var apiFeatureRequest: ApiFeatureRequest? = null
    private var containerComponentClassName: String? = null
    private var afterAuthorizationAction = ACTION_DOWNLOAD_MODULE
    private var dialog: AlertDialog? = null
    private var permissionRequested = false
    private var settingsRequired = false
    private var waitingForSettings = false
    private var waitingForImport = false
    private var waitingForDownloadSelection = false
    private var actionStarted = false
    private var statusMessageRes: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestId = savedInstanceState?.getString(STATE_REQUEST_ID)
            ?: intent.getStringExtra(EXTRA_REQUEST_ID)
                    ?: UUID.randomUUID().toString()
        if (!DynamicModuleSettings.isAvailable(this)) {
            cancelAndFinish()
            return
        }

        afterAuthorizationAction = savedInstanceState?.getInt(STATE_AFTER_AUTHORIZATION_ACTION)
            ?: intent.getIntExtra(
                EXTRA_AFTER_AUTHORIZATION_ACTION,
                ACTION_DOWNLOAD_MODULE
            )

        val intentFeatures = ModuleDownloadRegistry.requestedFeaturesFromIntent(intent)
        val persistedRequest = if (afterAuthorizationAction == ACTION_DOWNLOAD_MODULE) {
            PendingModuleRequestStore.load(this, requestId)
        } else {
            // A permission-only launch must never inherit a download hand-off for this request.
            PendingModuleRequestStore.remove(this, requestId)
            null
        }
        requestedFeatures = persistedRequest?.features ?: intentFeatures
        apiFeatureRequest = intent
            .getByteArrayExtra(ModuleDownloadRegistry.EXTRA_API_FEATURE_REQUEST)
            ?.let { bytes ->
                runCatching {
                    SafeParcelableSerializer.deserializeFromBytes(
                        bytes,
                        ApiFeatureRequest.CREATOR,
                    )
                }.onFailure { error ->
                    Log.w(TAG, "Unable to deserialize API feature request", error)
                }.getOrNull()
            }
            ?: ApiFeatureRequest().apply {
                features = requestedFeatures.map { Feature(it.name, it.minVersion) }
            }
        Log.d(TAG, "onCreate: apiFeatureRequest features=${apiFeatureRequest?.features?.size}")
        modules = ModuleDownloadRegistry.resolveModules(requestedFeatures)
        if (modules.isEmpty()) {
            Log.w(TAG, "No downloadable module for features: $requestedFeatures")
            PendingModuleRequestStore.remove(this, requestId)
            finish()
            return
        }
        containerComponentClassName = intent.getStringExtra(EXTRA_CONTAINER_COMPONENT_CLASS_NAME)
            ?: persistedRequest?.componentClassName
        val restoredModuleIndex = when {
            savedInstanceState?.containsKey(STATE_CURRENT_MODULE_INDEX) == true ->
                savedInstanceState.getInt(STATE_CURRENT_MODULE_INDEX).coerceIn(modules.indices)

            persistedRequest != null -> modules
                .indexOfFirst { it.catalogId == persistedRequest.currentCatalogId }
                .takeIf { it >= 0 }

            else -> null
        }
        currentModuleIndex = restoredModuleIndex
            ?: modules.indices.firstOrNull { !isModuleInstalled(modules[it]) }
                    ?: 0
        module = modules[currentModuleIndex]
        if (savedInstanceState == null && persistedRequest != null) {
            afterAuthorizationAction = persistedRequest.afterAuthorizationAction
        }

        permissionRequested = savedInstanceState?.getBoolean(STATE_PERMISSION_REQUESTED) ?: false
        settingsRequired = savedInstanceState?.getBoolean(STATE_SETTINGS_REQUIRED) ?: false
        waitingForSettings = savedInstanceState?.getBoolean(STATE_WAITING_FOR_SETTINGS) ?: false
        waitingForImport = savedInstanceState?.getBoolean(STATE_WAITING_FOR_IMPORT)
            ?: (persistedRequest != null)
        waitingForDownloadSelection = savedInstanceState
            ?.getBoolean(STATE_WAITING_FOR_DOWNLOAD_SELECTION)
            ?: (persistedRequest != null && !persistedRequest.downloadTargetSelected)
        statusMessageRes = savedInstanceState
            ?.getInt(STATE_STATUS_MESSAGE)
            ?.takeIf { it != 0 }
        showModulePage()
    }

    override fun onResume() {
        super.onResume()
        if (!::module.isInitialized) return
        if (!DynamicModuleSettings.isAvailable(this)) {
            cancelAndFinish()
            return
        }

        if (waitingForDownloadSelection) {
            waitingForDownloadSelection = false
            val targetSelected = PendingModuleRequestStore
                .load(this, requestId)
                ?.downloadTargetSelected == true
            if (!targetSelected) {
                waitingForImport = false
                actionStarted = false
                statusMessageRes = null
                refreshModulePage()
            }
        }

        if (waitingForImport) {
            if (handleImportedModule()) return
            actionStarted = false
            refreshModulePage()
        } else if (afterAuthorizationAction == ACTION_DOWNLOAD_MODULE && isModuleInstalled(module)) {
            handleImportedModule()
            return
        }

        if (!waitingForSettings || actionStarted) return

        waitingForSettings = false
        if (missingPermissions().isEmpty()) {
            performAuthorizedAction()
        } else {
            statusMessageRes = permissionDeniedMessage()
            refreshModulePage()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_PERMISSION_REQUESTED, permissionRequested)
        outState.putBoolean(STATE_SETTINGS_REQUIRED, settingsRequired)
        outState.putBoolean(STATE_WAITING_FOR_SETTINGS, waitingForSettings)
        outState.putBoolean(STATE_WAITING_FOR_IMPORT, waitingForImport)
        outState.putBoolean(STATE_WAITING_FOR_DOWNLOAD_SELECTION, waitingForDownloadSelection)
        outState.putInt(STATE_AFTER_AUTHORIZATION_ACTION, afterAuthorizationAction)
        outState.putString(STATE_REQUEST_ID, requestId)
        outState.putInt(STATE_CURRENT_MODULE_INDEX, currentModuleIndex)
        statusMessageRes?.let { outState.putInt(STATE_STATUS_MESSAGE, it) }
        super.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_MODULE_PERMISSIONS) return

        val missing = missingPermissions()
        if (missing.isEmpty()) {
            performAuthorizedAction()
            return
        }

        settingsRequired = permissionRequested && missing.any {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    !shouldShowRequestPermissionRationale(it.permission)
        }
        statusMessageRes = permissionDeniedMessage()
        refreshModulePage()
    }

    private fun showModulePage() {
        val moduleDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.chimera_module_download_page_title, getString(module.displayNameRes)))
            .setMessage(buildPageMessage())
            .setPositiveButton(primaryButtonText(), null)
            .setNegativeButton(R.string.chimera_module_cancel) { _, _ -> cancelAndFinish() }
            .setOnCancelListener { cancelAndFinish() }
            .create()
        moduleDialog.setOnShowListener {
            moduleDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                handlePrimaryAction()
            }
        }
        dialog = moduleDialog
        moduleDialog.show()
    }

    private fun refreshModulePage() {
        val moduleDialog = dialog ?: return
        moduleDialog.setTitle(
            getString(R.string.chimera_module_download_page_title, getString(module.displayNameRes))
        )
        moduleDialog.setMessage(buildPageMessage())
        moduleDialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
            setText(primaryButtonText())
            isEnabled = !actionStarted
        }
    }

    private fun buildPageMessage(): String {
        val permissionItems = if (module.requiredPermissions.isEmpty()) {
            getString(R.string.chimera_module_no_permissions)
        } else {
            module.requiredPermissions.joinToString("\n") { requirement ->
                val state = if (isPermissionGranted(requirement.permission)) {
                    getString(R.string.chimera_module_permission_granted)
                } else {
                    getString(R.string.chimera_module_permission_required)
                }
                getString(
                    R.string.chimera_module_permission_item,
                    getString(requirement.labelRes),
                    getString(requirement.descriptionRes),
                    state
                )
            }
        }
        return buildString {
            if (afterAuthorizationAction == ACTION_CONTINUE_MODULE) {
                append(getString(R.string.chimera_module_use_section_title))
                append('\n')
                append(getString(R.string.chimera_module_use_description, getString(module.displayNameRes)))
            } else {
                append(getString(R.string.chimera_module_download_section_title))
                append('\n')
                append(getString(R.string.chimera_module_download_description, getString(module.displayNameRes)))
            }
            append("\n\n")
            append(getString(R.string.chimera_module_permission_section_title))
            append('\n')
            append(permissionItems)
            append("\n\n")
            append(
                getString(
                    if (afterAuthorizationAction == ACTION_CONTINUE_MODULE) {
                        R.string.chimera_module_permission_continue_instruction
                    } else {
                        R.string.chimera_module_permission_instruction
                    }
                )
            )
            statusMessageRes?.let {
                append("\n\n")
                append(getString(it))
            }
            if (waitingForImport) {
                append("\n\n")
                append(getString(R.string.chimera_module_waiting_for_import))
            }
        }
    }

    private fun primaryButtonText(): String {
        val missingPermissions = missingPermissions()
        return when {
            missingPermissions.isEmpty() && waitingForImport ->
                getString(R.string.chimera_module_download_again)

            missingPermissions.isEmpty() -> getString(
                if (afterAuthorizationAction == ACTION_CONTINUE_MODULE) {
                    R.string.chimera_module_continue
                } else {
                    R.string.chimera_module_download
                }
            )

            settingsRequired -> getString(R.string.chimera_module_open_permission_settings)
            afterAuthorizationAction == ACTION_CONTINUE_MODULE ->
                getString(R.string.chimera_module_authorize_and_continue)

            else -> getString(R.string.chimera_module_authorize_and_download)
        }
    }

    private fun handlePrimaryAction() {
        if (!DynamicModuleSettings.isAvailable(this)) {
            cancelAndFinish()
            return
        }
        val missing = missingPermissions()
        if (missing.isEmpty()) {
            performAuthorizedAction()
            return
        }
        if (settingsRequired) {
            openPermissionSettings()
            return
        }

        permissionRequested = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                missing.map { it.permission }.distinct().toTypedArray(),
                REQUEST_MODULE_PERMISSIONS
            )
        }
    }

    private fun openPermissionSettings() {
        waitingForSettings = true
        try {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
            )
        } catch (e: ActivityNotFoundException) {
            waitingForSettings = false
            Log.w(TAG, "Unable to open application permission settings", e)
            statusMessageRes = R.string.chimera_module_permission_settings_unavailable
            refreshModulePage()
        }
    }

    private fun performAuthorizedAction() {
        if (!DynamicModuleSettings.isAvailable(this)) {
            cancelAndFinish()
            return
        }
        // Re-check immediately before leaving this page. This is the hard authorization gate.
        if (missingPermissions().isNotEmpty()) {
            statusMessageRes = permissionDeniedMessage()
            refreshModulePage()
            return
        }
        if (afterAuthorizationAction == ACTION_CONTINUE_MODULE && !isModuleInstalled(module)) {
            // The module may have been removed while the permission page was in front. Never return
            // success into a container that can no longer resolve its dynamic implementation.
            afterAuthorizationAction = ACTION_DOWNLOAD_MODULE
            statusMessageRes = null
            refreshModulePage()
            return
        }
        if (afterAuthorizationAction == ACTION_DOWNLOAD_MODULE) {
            val checkedCatalogId = module.catalogId
            if (handleImportedModule()) return
            // A multi-module request advanced to a new module: let the user read its explanation
            // and permission list before opening that module's external download.
            if (module.catalogId != checkedCatalogId) return
        }
        // handleImportedModule may have advanced a multi-module request to its next module.
        if (missingPermissions().isNotEmpty()) {
            statusMessageRes = permissionDeniedMessage()
            refreshModulePage()
            return
        }
        actionStarted = true
        refreshModulePage()
        if (afterAuthorizationAction == ACTION_CONTINUE_MODULE) {
            setResult(RESULT_OK)
            finish()
        } else {
            startExternalModuleDownload()
        }
    }

    private fun startExternalModuleDownload() {
        try {
            waitingForImport = true
            waitingForDownloadSelection = true
            statusMessageRes = null
            PendingModuleRequestStore.save(
                this,
                PendingModuleRequestStore.PendingRequest(
                    requestId = requestId,
                    features = requestedFeatures,
                    currentCatalogId = module.catalogId,
                    componentClassName = containerComponentClassName,
                    afterAuthorizationAction = afterAuthorizationAction,
                    taskId = taskId,
                )
            )
            startActivity(
                ModuleDownloadRegistry.createExternalDownloadChooserIntent(
                    module.downloadUrl,
                    getString(R.string.chimera_module_choose_download_app),
                    apiFeatureRequest,
                    createDownloadSelectionCallback(),
                )
            )
            // The browser/download application does not return an import result. Keep this page and
            // its original task alive until the downloaded file is opened with microG and committed.
            actionStarted = false
        } catch (e: ActivityNotFoundException) {
            actionStarted = false
            waitingForImport = false
            waitingForDownloadSelection = false
            PendingModuleRequestStore.remove(this, requestId)
            Log.w(TAG, "No external application can download this module", e)
            statusMessageRes = R.string.chimera_module_download_app_unavailable
            refreshModulePage()
        }
    }

    private fun createDownloadSelectionCallback() = PendingIntent.getBroadcast(
        this,
        requestId.hashCode(),
        Intent(this, ModuleImportCompletionReceiver::class.java)
            .setAction(ACTION_DOWNLOAD_TARGET_SELECTED)
            .putExtra(EXTRA_REQUEST_ID, requestId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_ONE_SHOT or if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
            // The chooser fills the selected component into this callback Intent.
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        },
    ).intentSender

    /**
     * Returns true only when this Activity completed successfully. A successful `.mods` import can
     * contain an unrelated module, so the requested module is always re-checked from persisted
     * Chimera configuration before the original request is resumed.
     */
    private fun handleImportedModule(): Boolean {
        if (!DynamicModuleSettings.isAvailable(this)) {
            cancelAndFinish()
            return true
        }
        if (!isModuleInstalled(module)) {
            Log.d(TAG, "Import completed without requested module ${module.catalogId}")
            return false
        }

        waitingForImport = false
        waitingForDownloadSelection = false
        actionStarted = false
        PendingModuleRequestStore.remove(this, requestId)

        val nextModuleIndex = modules.indices.firstOrNull { !isModuleInstalled(modules[it]) }
        if (nextModuleIndex != null) {
            currentModuleIndex = nextModuleIndex
            module = modules[currentModuleIndex]
            permissionRequested = false
            settingsRequired = false
            statusMessageRes = null
            refreshModulePage()
            return false
        }

        val moduleWithMissingPermission = modules.indices.firstOrNull { index ->
            missingPermissions(modules[index]).isNotEmpty()
        }
        if (moduleWithMissingPermission != null) {
            // Permission may have been revoked while the importer was in front. The module
            // is installed now, so switch this same page to authorize-and-continue instead of offering
            // another download.
            currentModuleIndex = moduleWithMissingPermission
            module = modules[currentModuleIndex]
            afterAuthorizationAction = ACTION_CONTINUE_MODULE
            statusMessageRes = R.string.chimera_module_permission_denied_continue
            refreshModulePage()
            return false
        }

        Log.i(TAG, "Verified imported modules for request $requestId; resuming original request")
        setResult(RESULT_OK)
        finish()
        return true
    }

    private fun cancelAndFinish() {
        if (::requestId.isInitialized) PendingModuleRequestStore.remove(this, requestId)
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun permissionDeniedMessage(): Int {
        return if (afterAuthorizationAction == ACTION_CONTINUE_MODULE) {
            R.string.chimera_module_permission_denied_continue
        } else {
            R.string.chimera_module_permission_denied
        }
    }

    private fun missingPermissions(): List<ModuleDownloadRegistry.PermissionRequirement> {
        return missingPermissions(module)
    }

    private fun missingPermissions(
        targetModule: ModuleDownloadRegistry.DownloadableModule,
    ): List<ModuleDownloadRegistry.PermissionRequirement> {
        return targetModule.requiredPermissions.filterNot { isPermissionGranted(it.permission) }
    }

    private fun isModuleInstalled(
        targetModule: ModuleDownloadRegistry.DownloadableModule,
    ): Boolean {
        val componentForModule = containerComponentClassName?.takeIf {
            modules.size == 1 || it in targetModule.requiredComponentClassNames
        }
        return ModuleDownloadRegistry.isModuleInstalled(
            this,
            targetModule,
            requestedFeatures,
            componentForModule,
        )
    }

    private fun isPermissionGranted(permission: String): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val ACTION_DOWNLOAD_MODULE = 0
        const val ACTION_CONTINUE_MODULE = 1
        const val ACTION_MODULE_IMPORT_COMPLETED =
            "com.google.android.chimera.component.MODULE_IMPORT_COMPLETED"
        const val ACTION_DOWNLOAD_TARGET_SELECTED =
            "com.google.android.chimera.component.DOWNLOAD_TARGET_SELECTED"
        const val EXTRA_REQUESTED_FEATURE_NAMES =
            "com.google.android.chimera.component.REQUESTED_FEATURE_NAMES"
        const val EXTRA_REQUESTED_FEATURE_VERSIONS =
            "com.google.android.chimera.component.REQUESTED_FEATURE_VERSIONS"
        const val EXTRA_REQUEST_ID =
            "com.google.android.chimera.component.REQUEST_ID"
        const val EXTRA_AFTER_AUTHORIZATION_ACTION =
            "com.google.android.chimera.component.AFTER_AUTHORIZATION_ACTION"
        const val EXTRA_CONTAINER_COMPONENT_CLASS_NAME =
            "com.google.android.chimera.component.CONTAINER_COMPONENT_CLASS_NAME"
    }
}
