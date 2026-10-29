/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.format.DateUtils
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.microg.gms.checkin.CheckinManager
import org.microg.gms.checkin.CheckinPreferences
import org.microg.gms.checkin.getCheckinServiceInfo
import org.microg.gms.profile.ProfileManager
import org.microg.gms.profile.ProfileManager.PROFILE_AUTO
import org.microg.gms.profile.ProfileManager.PROFILE_NATIVE
import org.microg.gms.profile.ProfileManager.PROFILE_REAL
import org.microg.gms.profile.ProfileManager.PROFILE_SYSTEM
import org.microg.gms.profile.ProfileManager.PROFILE_USER
import java.io.File
import java.io.FileOutputStream

class DeviceRegistrationFragment : PreferenceFragmentCompat() {
    private lateinit var switchBarPreference: SwitchBarPreference
    private lateinit var deviceProfile: ListPreference
    private lateinit var importProfile: Preference
    private lateinit var serial: Preference
    private lateinit var statusCategory: PreferenceCategory
    private lateinit var status: Preference
    private lateinit var androidId: Preference
    private val handler = Handler()
    private val updateRunnable = Runnable { updateStatus() }
    private lateinit var profileFileImport: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profileFileImport = registerForActivityResult(ActivityResultContracts.GetContent(), this::onFileSelected)
    }

    private fun onFileSelected(uri: Uri?) {
        if (uri == null) return
        try {
            val context = requireContext()
            val file = File.createTempFile("profile_", ".xml", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { inputStream.copyTo(it) }
            }
            val success = ProfileManager.importUserProfile(context, file)
            file.delete()
            if (success && ProfileManager.isAutoProfile(context, PROFILE_USER)) {
                ProfileManager.setProfile(context, PROFILE_USER)
            }
            updateStatus()
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_device_registration)
    }

    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {
        switchBarPreference = preferenceScreen.findPreference("pref_checkin_enabled") ?: switchBarPreference
        deviceProfile = preferenceScreen.findPreference("pref_device_profile") ?: deviceProfile
        importProfile = preferenceScreen.findPreference("pref_device_profile_import") ?: importProfile
        serial = preferenceScreen.findPreference("pref_device_serial") ?: serial
        statusCategory = preferenceScreen.findPreference("prefcat_device_registration_status") ?: statusCategory
        status = preferenceScreen.findPreference("pref_device_registration_status") ?: status
        androidId = preferenceScreen.findPreference("pref_device_registration_android_id") ?: androidId

        deviceProfile.setOnPreferenceChangeListener { _, newValue ->
            ProfileManager.setProfile(requireContext(), newValue as String? ?: PROFILE_AUTO)
            updateStatus()
            true
        }
        importProfile.setOnPreferenceClickListener {
            profileFileImport.launch("text/xml")
            true
        }
        switchBarPreference.setOnPreferenceChangeListener { _, newValue ->
            val newStatus = newValue as Boolean
            CheckinPreferences.setEnabled(requireContext(), newStatus)
            true
        }

        findPreference<Preference>("pref_device_registration_data")?.setOnPreferenceClickListener {
            val rawCheckInRequest = CheckinManager.getLastRawCheckInRequest(context)
            MaterialAlertDialogBuilder(it.context)
                .setTitle(R.string.pref_device_registration_data_title)
                .setMessage(rawCheckInRequest ?: getString(R.string.data_na))
                .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                .create()
                .show()
            true
        }
    }

    private fun configureProfilePreference() {
        val context = requireContext()
        val configuredProfile = ProfileManager.getConfiguredProfile(context)
        val autoProfile = ProfileManager.getAutoProfile(context)
        val autoProfileName = when (autoProfile) {
            PROFILE_NATIVE -> getString(R.string.profile_name_native)
            PROFILE_REAL -> getString(R.string.profile_name_real)
            else -> ProfileManager.getProfileName(context, autoProfile)
        }
        val profiles =
            mutableListOf(PROFILE_AUTO, PROFILE_NATIVE, PROFILE_REAL)
        val profileNames = mutableListOf(getString(R.string.profile_name_auto, autoProfileName), getString(R.string.profile_name_native), getString(R.string.profile_name_real))
        if (ProfileManager.hasProfile(context, PROFILE_SYSTEM)) {
            profiles.add(PROFILE_SYSTEM)
            profileNames.add(getString(R.string.profile_name_system, ProfileManager.getProfileName(context, PROFILE_SYSTEM)))
        }
        if (ProfileManager.hasProfile(context, PROFILE_USER)) {
            profiles.add(PROFILE_USER)
            profileNames.add(getString(R.string.profile_name_user, ProfileManager.getProfileName(context, PROFILE_USER)))
        }
        for (profile in R.xml::class.java.declaredFields.map { it.name }
            .filter { it.startsWith("profile_") }
            .map { it.substring(8) }
            .sorted()) {
            val profileName = ProfileManager.getProfileName(context, profile)
            if (profileName != null) {
                profiles.add(profile)
                profileNames.add(profileName)
            }
        }
        deviceProfile.entryValues = profiles.toTypedArray()
        deviceProfile.entries = profileNames.toTypedArray()
        deviceProfile.value = configuredProfile
        deviceProfile.summary =
            profiles.indexOf(configuredProfile).takeIf { it >= 0 }?.let { profileNames[it] } ?: "Unknown"
    }

    override fun onResume() {
        super.onResume()

        switchBarPreference.isChecked = CheckinPreferences.isEnabled(requireContext())

        updateStatus()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }

    private fun updateStatus() {
        handler.removeCallbacks(updateRunnable)
        handler.postDelayed(updateRunnable, UPDATE_INTERVAL)
        val appContext = requireContext().applicationContext
        lifecycleScope.launchWhenResumed {
            configureProfilePreference()
            serial.summary = ProfileManager.getSerial(appContext)
            val serviceInfo = getCheckinServiceInfo(appContext)
            statusCategory.isVisible = serviceInfo.configuration.enabled
            if (serviceInfo.lastCheckin > 0) {
                status.summary = getString(
                    R.string.checkin_last_registration,
                    DateUtils.getRelativeTimeSpanString(serviceInfo.lastCheckin, System.currentTimeMillis(), 0)
                )
                androidId.isVisible = true
                androidId.summary = serviceInfo.androidId.toString(16)
            } else {
                status.summary = getString(R.string.checkin_not_registered)
                androidId.isVisible = false
            }
        }
    }

    companion object {
        private const val UPDATE_INTERVAL = 1000L
    }
}
