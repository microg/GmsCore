/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import com.google.android.gms.R
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.launch
import org.microg.gms.checkin.CheckinPreferences
import org.microg.gms.common.ForegroundServiceOemUtils
import org.microg.gms.gcm.GcmDatabase
import org.microg.gms.gcm.GcmPrefs
import org.microg.gms.ui.settings.SettingsProvider
import org.microg.gms.ui.settings.getAllSettingsProviders
import org.microg.tools.ui.ResourceSettingsFragment

class SettingsFragment : ResourceSettingsFragment() {

    companion object {
        private const val TAG = "SettingsFragment"

        const val PREF_ABOUT = "pref_about"
        const val PREF_GCM = "pref_gcm"
        const val PREF_PRIVACY = "pref_privacy"
        const val PREF_CHECKIN = "pref_checkin"
        const val PREF_ACCOUNTS = "pref_accounts"
        const val PREF_HIDE_LAUNCHER_ICON = "pref_hide_launcher_icon"
        const val PREF_SELF_CHECK = "pref_self_check"
        const val PREF_GITHUB = "pref_github"
        const val PREF_IGNORE_BATTERY_OPTIMIZATION = "pref_ignore_battery_optimization"

        private const val ACTIVITY_LAUNCHER_CONTROL = "org.microg.gms.ui.SettingsActivityLauncher"
        private const val PREF_GITHUB_URL = "https://github.com/MorpheApp/MicroG-RE"
    }

    private val createdPreferences = mutableListOf<Preference>()

    private val requestIgnoreBatteryOptimizationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            updateBatteryOptimizationPreference()
        }

    init {
        preferencesResource = R.xml.preferences_start
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setupStaticPreferenceClickListeners()
        updateLauncherIconSwitchState()
        updateBatteryOptimizationPreference()
        updateAboutSummary()
        loadStaticEntries()
    }

        findPreference<Preference>(PREF_ACCOUNTS)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.accountManagerFragment)
            true
        }
        findPreference<Preference>(PREF_CHECKIN)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openCheckinSettings)
            true
        }
        findPreference<Preference>(PREF_GCM)?.setOnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openGcmSettings)
            true
        }
        findPreference<Preference>(PREF_PRIVACY)?.setOnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.privacyFragment)
            true
        }
        findPreference<SwitchPreferenceCompat>(PREF_HIDE_LAUNCHER_ICON)?.setOnPreferenceChangeListener { _, newValue ->
            val shouldHide = newValue as Boolean
            toggleLauncherIconVisibility(hide = shouldHide)
            true
        }
        findPreference<Preference>(PREF_VENDING)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openVendingSettings)
            true
        }
        findPreference<Preference>(PREF_WORK_PROFILE)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openWorkProfileSettings)
            true
        }

        findPreference<Preference>(PREF_ABOUT)!!.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                findNavController().navigate(requireContext(), R.id.openAbout)
                true
            }
            summary = getString(org.microg.tools.ui.R.string.about_version_str, AboutFragment.getSelfVersion(context))
        }

    private fun updateAboutSummary() {
        findPreference<Preference>(PREF_ABOUT)?.summary = getString(
            R.string.about_version_str, AboutFragment.getAppVersion(context)
        )
    }

    private fun loadStaticEntries() {
        val ctx = context ?: return
        getAllSettingsProviders(ctx).flatMap { it.getEntriesStatic(ctx) }
            .forEach { entry -> entry.createPreference(ctx) }
    }

    private fun updateDynamicEntries() {
        lifecycleScope.launch {
            val ctx = context ?: return@launch
            val entries = getAllSettingsProviders(ctx).flatMap { it.getEntriesDynamic(ctx) }

            createdPreferences.forEach { preference ->
                if (entries.none { it.key == preference.key }) preference.isVisible = false
            }

            entries.forEach { entry ->
                val preference = createdPreferences.find { it.key == entry.key }
                if (preference != null) preference.fillFromEntry(entry)
                else entry.createPreference(ctx)
            }
        }
    }

    private val Context.isIgnoringBatteryOptimizations: Boolean
        get() = (getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isIgnoringBatteryOptimizations(
            packageName
        ) == true

    private fun updateBatteryOptimizationPreference() {
        val ctx = context ?: return
        findPreference<Preference>(PREF_IGNORE_BATTERY_OPTIMIZATION)?.apply {
            isVisible = !ctx.isIgnoringBatteryOptimizations
            setOnPreferenceClickListener {
                requestIgnoringBatteryOptimizations()
                true
            }
        }
    }

    private fun requestIgnoringBatteryOptimizations() {
        val ctx = context ?: return
        ForegroundServiceOemUtils.openBatteryOptimizationSettings(ctx) { intent ->
            requestIgnoreBatteryOptimizationLauncher.launch(intent)
        }
    }

    private fun toggleLauncherIconVisibility(hide: Boolean) {
        val newState = if (hide) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }

        val ctx = context ?: return
        val component = ComponentName(ctx, ACTIVITY_LAUNCHER_CONTROL)

        ctx.packageManager.setComponentEnabledSetting(
            component, newState, PackageManager.DONT_KILL_APP
        )
    }

    private fun updateLauncherIconSwitchState() {
        val ctx = context ?: return
        val component = ComponentName(ctx, ACTIVITY_LAUNCHER_CONTROL)
        val state = ctx.packageManager.getComponentEnabledSetting(component)

        val isHidden = state != PackageManager.COMPONENT_ENABLED_STATE_ENABLED

        findPreference<SwitchPreferenceCompat>(PREF_HIDE_LAUNCHER_ICON)?.isChecked = isHidden
    }

    private fun updateGcmSummary() {
        val context = requireContext()
        val pref = findPreference<Preference>(PREF_GCM) ?: return

        if (GcmPrefs.get(context).isEnabled) {
            val database = GcmDatabase(context)
            val regCount = database.registrationList.size
            database.close()
            pref.summary =
                context.getString(org.microg.gms.base.core.R.string.service_status_enabled_short) + " - " + context.resources.getQuantityString(
                    R.plurals.gcm_registered_apps_counter, regCount, regCount
                )
        } else {
            pref.setSummary(org.microg.gms.base.core.R.string.service_status_disabled_short)
        }
    }

    private fun updateCheckinSummary() {
        val summaryRes =
            if (CheckinPreferences.isEnabled(requireContext())) org.microg.gms.base.core.R.string.service_status_enabled_short
            else org.microg.gms.base.core.R.string.service_status_disabled_short
        findPreference<Preference>(PREF_CHECKIN)?.setSummary(summaryRes)
    }

    private fun openGithub() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, PREF_GITHUB_URL.toUri()))
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Error opening link: $PREF_GITHUB_URL", e)
        }
    }

    private fun SettingsProvider.Companion.Entry.createPreference(context: Context): Preference? {
        val preference = Preference(context).fillFromEntry(this)
        val categoryKey = when (group) {
            SettingsProvider.Companion.Group.HEADER -> "prefcat_header"
            SettingsProvider.Companion.Group.GOOGLE -> "prefcat_google_services"
            SettingsProvider.Companion.Group.OTHER -> "prefcat_other_services"
            SettingsProvider.Companion.Group.FOOTER -> "prefcat_footer"
        }
        return try {
            findPreference<PreferenceCategory>(categoryKey)?.addPreference(preference)?.let {
                if (it) createdPreferences.add(preference)
                preference
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed adding preference $key", e)
            null
        }
    }

    private fun Preference.fillFromEntry(entry: SettingsProvider.Companion.Entry): Preference {
        key = entry.key
        title = entry.title
        summary = entry.summary
        icon = entry.icon
        isPersistent = false
        isVisible = true
        setOnPreferenceClickListener {
            findNavController().navigate(context, entry.navigationId)
            true
        }
        return this
    }

    override fun onResume() {
        super.onResume()
        val context = requireContext()
        if (GcmPrefs.get(requireContext()).isEnabled) {
            val database = GcmDatabase(context)
            val regCount = database.registrationList.size
            database.close()
            findPreference<Preference>(PREF_GCM)!!.summary = context.getString(org.microg.gms.base.core.R.string.service_status_enabled_short) + " - " + context.resources.getQuantityString(R.plurals.gcm_registered_apps_counter, regCount, regCount)
        } else {
            findPreference<Preference>(PREF_GCM)!!.setSummary(org.microg.gms.base.core.R.string.service_status_disabled_short)
        }

        findPreference<Preference>(PREF_CHECKIN)!!.setSummary(if (CheckinPreferences.isEnabled(requireContext())) org.microg.gms.base.core.R.string.service_status_enabled_short else org.microg.gms.base.core.R.string.service_status_disabled_short)
        findPreference<Preference>(PREF_SNET)!!.setSummary(if (SafetyNetPreferences.isEnabled(requireContext())) org.microg.gms.base.core.R.string.service_status_enabled_short else org.microg.gms.base.core.R.string.service_status_disabled_short)

        lifecycleScope.launchWhenResumed {
            val entries = getAllSettingsProviders(requireContext()).flatMap { it.getEntriesDynamic(requireContext()) }
            for (preference in createdPreferences) {
                if (!entries.any { it.key == preference.key }) preference.isVisible = false
            }
            for (entry in entries) {
                val preference = createdPreferences.find { it.key == entry.key }
                if (preference != null) preference.fillFromEntry(entry)
                else entry.createPreference()
            }
        }
    }

    companion object {
        const val PREF_ABOUT = "pref_about"
        const val PREF_GCM = "pref_gcm"
        const val PREF_SNET = "pref_snet"
        const val PREF_LOCATION = "pref_location"
        const val PREF_CHECKIN = "pref_checkin"
        const val PREF_VENDING = "pref_vending"
        const val PREF_WORK_PROFILE = "pref_work_profile"
        const val PREF_ACCOUNTS = "pref_accounts"
    }

    init {
        preferencesResource = R.xml.preferences_start
    }
}
