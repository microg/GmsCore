/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.location.core.R
import org.microg.gms.location.manager.LocationAppsDatabase
import org.microg.gms.ui.AppHeadingPreference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class LocationAppFragment : PreferenceFragmentCompat() {
    private lateinit var appHeadingPreference: AppHeadingPreference
    private lateinit var lastLocationCategory: PreferenceCategory
    private lateinit var lastLocation: Preference
    private lateinit var lastLocationMap: LocationMapPreference
    private lateinit var forceCoarse: TwoStatePreference
    private lateinit var database: LocationAppsDatabase

    private val packageName: String?
        get() = arguments?.getString("package")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = LocationAppsDatabase(requireContext())
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_location_app_details)
    }

    @SuppressLint("RestrictedApi")
    override fun onBindPreferences() {
        appHeadingPreference = preferenceScreen.findPreference("pref_location_app_heading") ?: appHeadingPreference
        lastLocationCategory = preferenceScreen.findPreference("prefcat_location_app_last_location") ?: lastLocationCategory
        lastLocation = preferenceScreen.findPreference("pref_location_app_last_location") ?: lastLocation
        lastLocationMap = preferenceScreen.findPreference("pref_location_app_last_location_map") ?: lastLocationMap
        forceCoarse = preferenceScreen.findPreference("pref_location_app_force_coarse") ?: forceCoarse
        forceCoarse.setOnPreferenceChangeListener { _, newValue ->
            packageName?.let { database.setForceCoarse(it, newValue as Boolean); true} == true
        }
    }

    override fun onResume() {
        super.onResume()
        updateContent()
    }

    override fun onPause() {
        super.onPause()
        database.close()
    }

    fun Double.toStringWithDigits(digits: Int): String {
        val s = this.toString()
        val i = s.indexOf('.')
        if (i <= 0 || s.length - i - 1 < digits) return s
        if (digits == 0) return s.substring(0, i)
        return s.substring(0, s.indexOf('.') + digits + 1)
    }

    fun updateContent() {
        val context = requireContext()
        lifecycleScope.launchWhenResumed {
            appHeadingPreference.packageName = packageName
            forceCoarse.isChecked = packageName?.let { database.getForceCoarse(it) } == true
            val location = packageName?.let { database.getAppLocation(it) }
            if (location != null) {
                lastLocationCategory.isVisible = true
                lastLocation.title = DateUtils.getRelativeTimeSpanString(location.time)
                lastLocation.intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${location.latitude},${location.longitude}"))
                lastLocationMap.location = location
                val address = try {
                    if (SDK_INT > 33) {
                        suspendCoroutine { continuation ->
                            try {
                                Geocoder(context).getFromLocation(location.latitude, location.longitude, 1) {
                                    continuation.resume(it.firstOrNull())
                                }
                            } catch (e: Exception) {
                                continuation.resumeWithException(e)
                            }
                        }
                    } else {
                        withContext(Dispatchers.IO) { Geocoder(context).getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull() }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, e)
                    null
                }
                if (address != null) {
                    val addressLine = StringBuilder()
                    var i = 0
                    addressLine.append(address.getAddressLine(i))
                    while (addressLine.length < 32 && address.maxAddressLineIndex > i) {
                        i++
                        addressLine.append(", ")
                        addressLine.append(address.getAddressLine(i))
                    }
                    lastLocation.summary = addressLine.toString()
                } else {
                    lastLocation.summary =  "${location.latitude.toStringWithDigits(6)}, ${location.longitude.toStringWithDigits(6)}"
                }
            } else {
                lastLocationCategory.isVisible = false
            }
        }
    }
}