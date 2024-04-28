/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.settings

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest
import org.microg.gms.common.Constants
import org.microg.gms.location.core.R
import org.microg.gms.location.manager.AskPermissionActivity
import org.microg.gms.location.manager.EXTRA_PERMISSIONS
import org.microg.gms.ui.buildAlertDialog

const val ACTION_LOCATION_SETTINGS_CHECKER = "com.google.android.gms.location.settings.CHECK_SETTINGS"
const val EXTRA_ORIGINAL_PACKAGE_NAME = "originalPackageName"
const val EXTRA_SETTINGS_REQUEST = "locationSettingsRequests"
const val EXTRA_REQUESTS = "locationRequests"
const val EXTRA_SETTINGS_STATES = "com.google.android.gms.location.LOCATION_SETTINGS_STATES"
const val EXTRA_SHOW_MG_SETTINGS = "showMgLocationSettings"

private const val REQUEST_CODE_LOCATION = 120
private const val TAG = "LocationSettings"

class LocationSettingsCheckerActivity : Activity(), DialogInterface.OnCancelListener, DialogInterface.OnClickListener {
    private var alwaysShow = false
    private var needBle = false
    private var improvements = emptyList<Improvement>()
    private val displayList = mutableListOf<Improvement>()
    private var requests: List<LocationRequest>? = null

    private val mgLocationPermission =
        arrayListOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION).apply {
            if (Build.VERSION.SDK_INT >= 29) add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "LocationSettingsCheckerActivity onCreate")
        if (intent.hasExtra(EXTRA_SETTINGS_REQUEST)) {
            try {
                val request = SafeParcelableSerializer.deserializeFromBytes(intent.getByteArrayExtra(EXTRA_SETTINGS_REQUEST), LocationSettingsRequest.CREATOR)
                alwaysShow = request.alwaysShow
                needBle = request.needBle
                requests = request.requests
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
        if (requests == null && intent.hasExtra(EXTRA_REQUESTS)) {
            try {
                val arrayList = intent.getSerializableExtra(EXTRA_REQUESTS) as? ArrayList<*>
                requests = arrayList?.map {
                    SafeParcelableSerializer.deserializeFromBytes(it as ByteArray, LocationRequest.CREATOR)
                }
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
        if (requests == null) {
            finishResult(RESULT_CANCELED)
        } else {
            updateImprovements()
            if (improvements.isEmpty()) {
                finishResult(RESULT_OK)
            } else {
                showDialog()
            }
        }
    }

    enum class Improvement {
        GPS, NLP, GPS_AND_NLP, WIFI, WIFI_SCANNING, BLUETOOTH, BLE_SCANNING, PERMISSIONS, DATA_SOURCE
    }

    private fun updateImprovements() {
        val detailedStates = getDetailedLocationSettingsStates()
        // TODO: Correctly determine the needed improvements based on requests
        improvements = listOfNotNull(
            Improvement.GPS_AND_NLP.takeIf { !displayList.contains(it) && (!detailedStates.gpsUsable || !detailedStates.networkLocationUsable) },
            Improvement.PERMISSIONS.takeIf { !displayList.contains(it) && (!detailedStates.coarseLocationPermission || !detailedStates.fineLocationPermission) },
        )
    }

    private fun showDialog() {

        val alertDialog = buildAlertDialog()
            .setOnCancelListener(this)
            .setPositiveButton(R.string.location_settings_dialog_btn_sure, this)
            .setNegativeButton(R.string.location_settings_dialog_btn_cancel, this)
            .create()
        alertDialog.setCanceledOnTouchOutside(false)

        val view = layoutInflater.inflate(R.layout.location_settings_dialog, null)
        view.findViewById<TextView>(R.id.message_title)
            .setText(if (alwaysShow) R.string.location_settings_dialog_message_title_to_continue else R.string.location_settings_dialog_message_title_for_better_experience)

        val messages = view.findViewById<LinearLayout>(R.id.messages)
        for ((messageIndex, improvement) in improvements.withIndex()) {
            val item = layoutInflater.inflate(R.layout.location_settings_dialog_item, messages, false)
            item.findViewById<TextView>(android.R.id.text1).text = when (improvement) {
                Improvement.GPS_AND_NLP -> getString(R.string.location_settings_dialog_message_location_services_gps_and_nlp)
                Improvement.PERMISSIONS -> getString(R.string.location_settings_dialog_message_gls_consent)
                else -> {
                    Log.w(TAG, "Unsupported improvement: $improvement")
                    ""
                }
            }
            item.findViewById<ImageView>(android.R.id.icon).setImageDrawable(
                when (improvement) {
                    Improvement.GPS_AND_NLP -> ContextCompat.getDrawable(this, R.drawable.ic_gps)
                    Improvement.PERMISSIONS -> ContextCompat.getDrawable(this, R.drawable.ic_mg)
                    else -> {
                        Log.w(TAG, "Unsupported improvement: $improvement")
                        null
                    }
                }
            )
            messages.addView(item, messageIndex + 1)
        }

        alertDialog.setView(view)
        alertDialog.show()
    }

    private fun handleContinue() {
        val improvement = improvements.firstOrNull() ?: return finishResult(RESULT_OK)
        displayList.add(improvement)
        when (improvement) {
            Improvement.PERMISSIONS -> {
                val targetIntent = if (intent.hasExtra(EXTRA_SHOW_MG_SETTINGS) && intent.getBooleanExtra(EXTRA_SHOW_MG_SETTINGS, false)) {
                    Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", Constants.GMS_PACKAGE_NAME, null)
                    }
                }else{
                    Intent(this, AskPermissionActivity::class.java).apply {
                        putExtra(EXTRA_PERMISSIONS, mgLocationPermission.toTypedArray())
                    }
                }
                startActivityForResult(targetIntent, REQUEST_CODE_LOCATION)
                return
            }
            Improvement.GPS, Improvement.NLP, Improvement.GPS_AND_NLP -> {
                // TODO: If we have permission to, just activate directly
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(intent, REQUEST_CODE_LOCATION)
                return // We will continue from onActivityResult
            }
            else -> {
                Log.w(TAG, "Unsupported improvement: $improvement")
            }
        }
        updateImprovements()
        handleContinue()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_LOCATION) {
            checkImprovements()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun checkImprovements() {
        // Check if we improved, if so continue, otherwise show dialog again
        val oldImprovements = improvements
        updateImprovements()
        if (oldImprovements == improvements) {
            showDialog()
        } else {
            handleContinue()
        }
    }

    private fun finishResult(resultCode: Int) {
        val states = getDetailedLocationSettingsStates().toApi()
        setResult(resultCode, Intent().apply {
            putExtra(EXTRA_SETTINGS_STATES, SafeParcelableSerializer.serializeToBytes(states))
        })
        finish()
    }

    override fun onBackPressed() {
        finishResult(RESULT_CANCELED)
    }

    override fun onCancel(dialog: DialogInterface?) {
        finishResult(RESULT_CANCELED)
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        Log.d(TAG, "Not yet implemented: onClick")
        when (which) {
            DialogInterface.BUTTON_NEGATIVE -> finishResult(RESULT_CANCELED)
            DialogInterface.BUTTON_POSITIVE -> handleContinue()
        }
    }
}