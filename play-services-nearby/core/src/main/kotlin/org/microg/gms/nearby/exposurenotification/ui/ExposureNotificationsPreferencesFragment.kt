/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import org.microg.gms.nearby.core.R
import org.microg.gms.nearby.exposurenotification.*
import org.microg.gms.ui.AppIconPreference
import org.microg.gms.ui.getApplicationInfoIfExists
import org.microg.gms.ui.navigate


class ExposureNotificationsPreferencesFragment : PreferenceFragmentCompat() {
    private lateinit var exposureEnableInfo: Preference
    private lateinit var exposureBluetoothOff: Preference
    private lateinit var exposureLocationOff: Preference
    private lateinit var exposureNearbyNotGranted: Preference
    private lateinit var exposureBluetoothUnsupported: Preference
    private lateinit var exposureBluetoothNoAdvertisement: Preference
    private lateinit var exposureApps: PreferenceCategory
    private lateinit var exposureAppsNone: Preference
    private lateinit var collectedRpis: Preference
    private lateinit var advertisingId: Preference
    private var turningBluetoothOn: Boolean = false
    private val handler = Handler()
    private val updateStatusRunnable = Runnable { updateStatus() }
    private val updateContentRunnable = Runnable { updateContent() }
    private var permissionRequestCode = 33

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_exposure_notifications)
    }

    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {
        exposureEnableInfo = preferenceScreen.findPreference("pref_exposure_enable_info") ?: exposureEnableInfo
        exposureBluetoothOff = preferenceScreen.findPreference("pref_exposure_error_bluetooth_off") ?: exposureBluetoothOff
        exposureLocationOff = preferenceScreen.findPreference("pref_exposure_error_location_off") ?: exposureLocationOff
        exposureNearbyNotGranted = preferenceScreen.findPreference("pref_exposure_error_nearby_not_granted") ?: exposureNearbyNotGranted
        exposureBluetoothUnsupported = preferenceScreen.findPreference("pref_exposure_error_bluetooth_unsupported") ?: exposureBluetoothUnsupported
        exposureBluetoothNoAdvertisement = preferenceScreen.findPreference("pref_exposure_error_bluetooth_no_advertise") ?: exposureBluetoothNoAdvertisement
        exposureApps = preferenceScreen.findPreference("prefcat_exposure_apps") ?: exposureApps
        exposureAppsNone = preferenceScreen.findPreference("pref_exposure_apps_none") ?: exposureAppsNone
        collectedRpis = preferenceScreen.findPreference("pref_exposure_collected_rpis") ?: collectedRpis
        advertisingId = preferenceScreen.findPreference("pref_exposure_advertising_id") ?: advertisingId

        exposureLocationOff.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
            true
        }

        exposureBluetoothOff.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            lifecycleScope.launchWhenStarted {
                turningBluetoothOn = true
                it.isVisible = false
                val adapter = BluetoothAdapter.getDefaultAdapter()
                if (adapter != null && !adapter.enableAsync(requireContext())) {
                    turningBluetoothOn = false
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(intent, 144)
                } else {
                    turningBluetoothOn = false
                    updateStatus()
                }
            }
            true
        }

        exposureNearbyNotGranted.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val nearbyPermissions = arrayOf("android.permission.BLUETOOTH_ADVERTISE", "android.permission.BLUETOOTH_SCAN")
            requestPermissions(nearbyPermissions, ++permissionRequestCode)
            true
        }

        collectedRpis.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openExposureRpis)
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.permissionRequestCode) {
            updateStatus()
            // Tell the NotifyService that it should update the notification
            val intent = Intent(NOTIFICATION_UPDATE_ACTION)
            requireContext().sendBroadcast(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        updateStatus()
        updateContent()
    }

    override fun onPause() {
        super.onPause()

        handler.removeCallbacks(updateStatusRunnable)
        handler.removeCallbacks(updateContentRunnable)
    }

    private fun updateStatus() {
        val appContext = requireContext().applicationContext
        lifecycleScope.launchWhenResumed {
            handler.postDelayed(updateStatusRunnable, UPDATE_STATUS_INTERVAL)
            val enabled = getExposureNotificationsServiceInfo(appContext).configuration.enabled
            exposureEnableInfo.isVisible = !enabled

            val bluetoothSupported = ScannerService.isSupported(appContext)
            val advertisingSupported = if (bluetoothSupported == true) AdvertiserService.isSupported(appContext) else bluetoothSupported

            val nearbyPermissions = arrayOf("android.permission.BLUETOOTH_ADVERTISE", "android.permission.BLUETOOTH_SCAN")
            // Expresses implication (API 31+ → all new permissions granted) ≡ (¬API 31+ | all new permissions granted)
            val nearbyPermissionsGranted = Build.VERSION.SDK_INT < 31 || nearbyPermissions.all {
                ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED
            }
            exposureNearbyNotGranted.isVisible = enabled && !nearbyPermissionsGranted
            exposureLocationOff.isVisible = enabled && bluetoothSupported != false && !LocationManagerCompat.isLocationEnabled(appContext.getSystemService(LOCATION_SERVICE) as LocationManager)
            exposureBluetoothOff.isVisible = enabled && bluetoothSupported == null && !turningBluetoothOn
            exposureBluetoothUnsupported.isVisible = enabled && bluetoothSupported == false
            exposureBluetoothNoAdvertisement.isVisible = enabled && bluetoothSupported == true && advertisingSupported != true

            advertisingId.isVisible = enabled && advertisingSupported == true
        }
    }

    private fun updateContent() {
        val context = requireContext()
        lifecycleScope.launchWhenResumed {
            handler.postDelayed(updateContentRunnable, UPDATE_CONTENT_INTERVAL)
            val (apps, lastHourKeys, currentId) = ExposureDatabase.with(context) { database ->
                val apps = database.appList.map { packageName ->
                    context.packageManager.getApplicationInfoIfExists(packageName)
                }.filterNotNull().mapIndexed { idx, applicationInfo ->
                    val pref = AppIconPreference(context)
                    pref.order = idx
                    pref.title = applicationInfo.loadLabel(context.packageManager)
                    pref.icon = applicationInfo.loadIcon(context.packageManager)
                    pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        findNavController().navigate(requireContext(), R.id.openExposureAppDetails, bundleOf(
                                "package" to applicationInfo.packageName
                        ))
                        true
                    }
                    pref.key = "pref_exposure_app_" + applicationInfo.packageName
                    pref
                }
                val lastHourKeys = database.hourRpiCount
                val currentId = database.currentRpiId
                Triple(apps, lastHourKeys, currentId)
            }
            collectedRpis.summary = getString(R.string.pref_exposure_collected_rpis_summary, lastHourKeys)
            if (currentId != null) {
                advertisingId.isVisible = true
                advertisingId.summary = currentId.toString()
            } else {
                advertisingId.isVisible = false
            }
            exposureApps.removeAll()
            if (apps.isEmpty()) {
                exposureApps.addPreference(exposureAppsNone)
            } else {
                for (app in apps) {
                    exposureApps.addPreference(app)
                }
            }
        }
    }

    companion object {
        private const val UPDATE_STATUS_INTERVAL = 1000L
        private const val UPDATE_CONTENT_INTERVAL = 60000L
    }
}
