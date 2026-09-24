/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import com.google.android.chimera.config.ChimeraConfigManager
import com.google.android.chimera.config.ChimeraModuleBootstrap
import com.google.android.chimera.config.DynamicModuleSettings
import com.google.android.chimera.config.InstalledModuleStatus
import com.google.android.chimera.config.ModuleCapabilityStatus
import com.google.android.gms.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.microg.gms.moduleinstall.dynamicmodule.ModuleImportActivity

/**
 * Dynamic module management screen: enable toggle + .mods import entry + list of imported modules.
 * The list only shows modules that are actually imported -- the data source is the chimera_manifest.pb
 * persisted by ChimeraConfigManager (written when an import is flushed to disk, read reliably across
 * processes via reload), rather than enumerating a hardcoded fixed module table.
 */
class DynamicModuleManagerFragment : PreferenceFragmentCompat() {
    private lateinit var dynamicModuleEnabled: TwoStatePreference
    private lateinit var modules: PreferenceCategory
    private lateinit var moduleNone: Preference
    private lateinit var moduleImport: Preference
    private lateinit var modsImport: ActivityResultLauncher<String>
    private var moduleRefreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        modsImport = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                startActivity(Intent(requireContext(), ModuleImportActivity::class.java).apply {
                    data = uri
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                })
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_dynamicmodule)
        modules = preferenceScreen.findPreference("prefcat_dynamicmodule_installed") ?: return
        moduleNone = preferenceScreen.findPreference("pref_dynamicmodule_none") ?: return
        moduleImport = preferenceScreen.findPreference("pref_dynamicmodule_import") ?: return
    }

    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {
        moduleImport.setOnPreferenceClickListener {
            // "*/*": a content:// .mods rarely has a registered MIME type; the importer validates content.
            modsImport.launch("*/*")
            true
        }
        dynamicModuleEnabled = preferenceScreen.findPreference(PREF_DYNAMIC_MODULE_ENABLED) ?: return
        dynamicModuleEnabled.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            if (newValue is Boolean) {
                if (newValue && !DynamicModuleSettings.isRuntimeSupported()) {
                    Toast.makeText(
                        requireContext(),
                        R.string.dynamicmodule_unsupported_android_version,
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@OnPreferenceChangeListener false
                }
                val appContext = requireContext().applicationContext
                val updated = DynamicModuleSettings.setEnabled(appContext, newValue)
                updateModules()
                updated
            } else {
                false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateModules()
    }

    override fun onDestroy() {
        moduleRefreshJob?.cancel()
        moduleRefreshJob = null
        super.onDestroy()
    }

    private fun updateModules() {
        moduleRefreshJob?.cancel()
        val appContext = requireContext().applicationContext
        val runtimeSupported = DynamicModuleSettings.isRuntimeSupported()
        dynamicModuleEnabled.isChecked = DynamicModuleSettings.isEnabled(appContext)
        dynamicModuleEnabled.isEnabled = runtimeSupported
        moduleImport.isEnabled = runtimeSupported && dynamicModuleEnabled.isChecked
        moduleRefreshJob = lifecycleScope.launch {
            val installed = withContext(Dispatchers.IO) {
                // The :ui process AppContext may not be initialized; without initializing first,
                // getChimeraManifest returns an empty store and the list comes up empty.
                ChimeraModuleBootstrap.ensureInitialized(appContext)
                runCatching { ChimeraConfigManager.reload() }
                ChimeraConfigManager.listInstalledModuleStatuses(appContext)
                    .filter { it.moduleName.isNotEmpty() && it.moduleName != "ROOT" }
                    .sortedBy { it.moduleName }
                    .also { Log.d(TAG, "updateModules: ${it.size} installed modules") }
            }
            if (!isResumed) return@launch
            renderModules(appContext, installed)
        }
    }

    private fun renderModules(context: Context, list: List<InstalledModuleStatus>) {
        modules.removeAll()
        modules.isVisible = true
        moduleNone.isVisible = false
        for (module in list) {
            val name = module.moduleName
            val pref = Preference(context).apply {
                key = "pref_dynamicmodule_module_$name"
                title = name
                summary = context.getString(
                    R.string.dynamicmodule_version_fmt,
                    module.moduleVersion.ifEmpty { "?" },
                    context.getString(capabilityStatusString(module.capabilityStatus))
                )
                setOnPreferenceClickListener { confirmDelete(name); true }
            }
            modules.addPreference(pref)
        }
        if (modules.preferenceCount == 0) {
            moduleNone.isVisible = true
            modules.addPreference(moduleNone)
        }
    }

    private fun capabilityStatusString(status: ModuleCapabilityStatus): Int = when (status) {
        ModuleCapabilityStatus.LOADABLE -> R.string.dynamicmodule_state_loaded
        ModuleCapabilityStatus.PARTIAL_COMPONENT_SUPPORT -> R.string.dynamicmodule_state_partial
        ModuleCapabilityStatus.UNSUPPORTED_INITIALIZER -> R.string.dynamicmodule_state_unsupported_initializer
        ModuleCapabilityStatus.UNVERIFIED_ARTIFACT -> R.string.dynamicmodule_state_unverified
    }

    private fun confirmDelete(moduleName: String) {
        // Must use the Activity context (with AppCompat theme), not applicationContext, or AlertDialog crashes.
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dynamicmodule_remove_action)
            .setMessage(moduleName)
            .setPositiveButton(R.string.dynamicmodule_remove_action) { _, _ -> removeModule(moduleName) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun removeModule(moduleName: String) {
        val appContext = requireContext().applicationContext
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    appContext.contentResolver.call(
                        "content://com.google.android.gms.chimera".toUri(),
                        "removeModule", moduleName, null
                    )?.getBoolean("removed", false) ?: false
                }.onFailure { Log.w(TAG, "removeModule IPC failed for $moduleName", it) }.getOrDefault(false)
            }
            Toast.makeText(
                appContext,
                if (ok) R.string.dynamicmodule_remove_done else R.string.dynamicmodule_remove_failed,
                Toast.LENGTH_SHORT
            ).show()
            updateModules()
        }
    }

    companion object {
        private const val TAG = "DynamicModuleMgr"
        const val PREF_DYNAMIC_MODULE_ENABLED = "pref_dynamicmodule_enabled"
    }
}
