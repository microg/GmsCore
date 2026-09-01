/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.ComponentName
import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.core.net.toUri
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import com.google.android.gms.R
import java.util.Locale
import com.google.android.material.transition.MaterialSharedAxis
import org.microg.gms.checkin.CheckinPreferences
import org.microg.gms.common.ForegroundServiceOemUtils
import org.microg.gms.gcm.GcmDatabase
import org.microg.gms.gcm.GcmPrefs
import org.microg.gms.safetynet.SafetyNetPreferences
import org.microg.gms.ui.settings.SettingsProvider
import org.microg.gms.ui.settings.getAllSettingsProviders
import org.microg.tools.ui.ResourceSettingsFragment

class SettingsFragment : ResourceSettingsFragment() {
    private val createdPreferences = mutableListOf<Preference>()

    private val requestIgnoreBatteryOptimizationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            updateBatteryOptimizationPreference()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        updateBatteryOptimizationPreference()

        findPreference<Preference>(PREF_ACCOUNTS)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.accountManagerFragment)
            true
        }
        findPreference<Preference>(PREF_CHECKIN)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openCheckinSettings)
            true
        }
        findPreference<Preference>(PREF_GCM)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openGcmSettings)
            true
        }
        findPreference<Preference>(PREF_SNET)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openSafetyNetSettings)
            true
        }
        findPreference<Preference>(PREF_LOCATION)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openLocationSettings)
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
        findPreference<Preference>(PREF_PRIVACY)?.setOnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.privacyFragment)
            true
        }
        if (resources.getBoolean(R.bool.hide_launcher_icon_available)) {
            findPreference<SwitchPreferenceCompat>(PREF_HIDE_LAUNCHER_ICON)?.setOnPreferenceChangeListener { _, newValue ->
                val shouldHide = newValue as Boolean
                toggleLauncherIconVisibility(hide = shouldHide)
                true
            }
        } else {
            // Icon is always shown in this build; the toggle only exists in the noicon variant.
            findPreference<SwitchPreferenceCompat>(PREF_HIDE_LAUNCHER_ICON)?.isVisible = false
        }
        findPreference<Preference>(PREF_GITHUB)?.setOnPreferenceClickListener {
            openGithub()
            true
        }
        setupLanguagePreference()

        findPreference<Preference>(PREF_ABOUT)!!.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                findNavController().navigate(requireContext(), R.id.openAbout)
                true
            }
            summary = getString(org.microg.tools.ui.R.string.about_version_str, AboutFragment.getSelfVersion(context))
        }

        for (entry in getAllSettingsProviders(requireContext()).flatMap { it.getEntriesStatic(requireContext()) }) {
            entry.createPreference()
        }
    }

    private fun SettingsProvider.Companion.Entry.createPreference(): Preference? {
        val preference = Preference(requireContext()).fillFromEntry(this)
        try {
            if (findPreference<PreferenceCategory>(when (group) {
                    SettingsProvider.Companion.Group.HEADER -> "prefcat_header"
                    SettingsProvider.Companion.Group.GOOGLE -> "prefcat_google_services"
                    SettingsProvider.Companion.Group.OTHER -> "prefcat_other_services"
                    SettingsProvider.Companion.Group.FOOTER -> "prefcat_footer"
                })?.addPreference(preference) == true) {
                createdPreferences.add(preference)
                return preference
            } else {
                Log.w(TAG, "Preference not added $key")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed adding preference $key", e)
        }
        return null
    }

    private fun Preference.fillFromEntry(entry: SettingsProvider.Companion.Entry): Preference {
        key = entry.key
        title = entry.title
        summary = entry.summary
        icon = entry.icon
        isPersistent = false
        isVisible = true
        layoutResource = R.layout.preference_material_middle
        setOnPreferenceClickListener {
            findNavController().navigate(requireContext(), entry.navigationId)
            true
        }
        return this
    }

    override fun onResume() {
        super.onResume()
        updateBatteryOptimizationPreference()
        if (resources.getBoolean(R.bool.hide_launcher_icon_available)) updateLauncherIconSwitchState()
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
        updateLanguagePreferenceSummary()

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

    private val Context.isIgnoringBatteryOptimizations: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                (getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isIgnoringBatteryOptimizations(packageName) == true

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
        val pm = ctx.packageManager
        val state = pm.getComponentEnabledSetting(component)
        val manifestEnabled = try {
            pm.getActivityInfo(component, 0).enabled
        } catch (e: PackageManager.NameNotFoundException) {
            true
        }
        // Effective state: a runtime ENABLED setting wins, a runtime disable wins, and
        // otherwise the manifest default (android:enabled on the launcher alias) applies.
        // Keeps the switch accurate for fresh installs and explicit runtime toggles alike.
        val isHidden = when (state) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> false
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> !manifestEnabled
            else -> true
        }

        findPreference<SwitchPreferenceCompat>(PREF_HIDE_LAUNCHER_ICON)?.isChecked = isHidden
    }

    private fun setupLanguagePreference() {
        val preference = findPreference<ListPreference>(PREF_LANGUAGE) ?: return
        val tags = resources.getStringArray(R.array.pref_language_values)
        val entries = arrayOfNulls<String>(tags.size + 1)
        val values = arrayOfNulls<String>(tags.size + 1)
        entries[0] = getString(R.string.pref_language_system_default)
        values[0] = ""
        for (i in tags.indices) {
            values[i + 1] = tags[i]
            entries[i + 1] = displayName(tags[i])
        }
        preference.entries = entries
        preference.entryValues = values
        preference.setOnPreferenceChangeListener { _, newValue ->
            val tag = newValue as String
            AppCompatDelegate.setApplicationLocales(
                if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
            )
            updateLanguagePreferenceSummary()
            true
        }
        updateLanguagePreferenceSummary()
    }

    private fun updateLanguagePreferenceSummary() {
        val preference = findPreference<ListPreference>(PREF_LANGUAGE) ?: return
        val current = AppCompatDelegate.getApplicationLocales().get(0)
        if (current == null) {
            preference.value = ""
            preference.summary = getString(R.string.pref_language_system_default)
        } else {
            val tag = current.toLanguageTag()
            preference.value = tag
            preference.summary = displayName(tag)
        }
    }

    private fun displayName(tag: String): String {
        // "in"/"iw" are legacy ISO codes; use the modern ones for display only.
        val displayTag = when {
            tag.startsWith("in-") -> "id-" + tag.substring(3)
            tag.startsWith("iw-") -> "he-" + tag.substring(3)
            else -> tag
        }
        val locale = Locale.forLanguageTag(displayTag)
        return locale.getDisplayName(locale)
    }

    private fun openGithub() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, PREF_GITHUB_URL.toUri()))
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Error opening link: $PREF_GITHUB_URL", e)
        }
    }

    private fun requestIgnoringBatteryOptimizations() {
        val ctx = context ?: return
        ForegroundServiceOemUtils.openBatteryOptimizationSettings(ctx) { intent ->
            requestIgnoreBatteryOptimizationLauncher.launch(intent)
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
        const val PREF_HIDE_LAUNCHER_ICON = "pref_hide_launcher_icon"
        const val PREF_GITHUB = "pref_github"
        const val PREF_LANGUAGE = "pref_language"
        const val PREF_PRIVACY = "pref_privacy"
        const val PREF_IGNORE_BATTERY_OPTIMIZATION = "pref_ignore_battery_optimization"

        private const val ACTIVITY_LAUNCHER_CONTROL = "org.microg.gms.ui.SettingsActivityLauncher"
        private const val PREF_GITHUB_URL = "https://github.com/MorpheApp/MicroG-RE"
    }

    init {
        preferencesResource = R.xml.preferences_start
    }
}
