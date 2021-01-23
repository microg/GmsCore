/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.core.ui

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.lifecycleScope
import org.microg.gms.nearby.exposurenotification.*
import org.microg.gms.ui.getApplicationInfoIfExists


class ExposureNotificationsConfirmActivity : AppCompatActivity() {
    private var resultCode: Int = RESULT_CANCELED
        set(value) {
            setResult(value)
            field = value
        }
    private val receiver: ResultReceiver?
        get() = intent.getParcelableExtra(KEY_CONFIRM_RECEIVER)
    private val action: String?
        get() = intent.getStringExtra(KEY_CONFIRM_ACTION)
    private val targetPackageName: String?
        get() = intent.getStringExtra(KEY_CONFIRM_PACKAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.exposure_notifications_confirm_activity)
        val applicationInfo = packageManager.getApplicationInfoIfExists(targetPackageName)
        val selfApplicationInfo = packageManager.getApplicationInfoIfExists(packageName)
        when (action) {
            CONFIRM_ACTION_START -> {
                findViewById<TextView>(android.R.id.title).text = getString(R.string.exposure_confirm_start_title)
                findViewById<TextView>(android.R.id.summary).text = getString(R.string.exposure_confirm_start_summary, applicationInfo?.loadLabel(packageManager)
                        ?: targetPackageName)
                findViewById<Button>(android.R.id.button1).text = getString(R.string.exposure_confirm_start_button)
                findViewById<TextView>(R.id.grant_permission_summary).text = getString(R.string.exposure_confirm_permission_description, selfApplicationInfo?.loadLabel(packageManager)
                        ?: packageName)
                checkPermissions()
                checkBluetooth()
                checkLocation()
            }
            CONFIRM_ACTION_STOP -> {
                findViewById<TextView>(android.R.id.title).text = getString(R.string.exposure_confirm_stop_title)
                findViewById<TextView>(android.R.id.summary).text = getString(R.string.exposure_confirm_stop_summary)
                findViewById<Button>(android.R.id.button1).text = getString(R.string.exposure_confirm_stop_button)
            }
            CONFIRM_ACTION_KEYS -> {
                findViewById<TextView>(android.R.id.title).text = getString(R.string.exposure_confirm_keys_title, applicationInfo?.loadLabel(packageManager)
                        ?: targetPackageName)
                findViewById<TextView>(android.R.id.summary).text = getString(R.string.exposure_confirm_keys_summary)
                findViewById<Button>(android.R.id.button1).text = getString(R.string.exposure_confirm_keys_button)
            }
            else -> {
                resultCode = RESULT_CANCELED
                finish()
            }
        }
        findViewById<Button>(android.R.id.button1).setOnClickListener {
            resultCode = RESULT_OK
            finish()
        }
        findViewById<Button>(android.R.id.button2).setOnClickListener {
            resultCode = RESULT_CANCELED
            finish()
        }
        findViewById<Button>(R.id.grant_permission_button).setOnClickListener {
            requestPermissions()
        }
        findViewById<Button>(R.id.enable_bluetooth_button).setOnClickListener {
            requestBluetooth()
        }
        findViewById<Button>(R.id.enable_location_button).setOnClickListener {
            requestLocation()
        }
    }

    override fun onResume() {
        super.onResume()
        if (permissionNeedsHandling) checkPermissions()
        if (bluetoothNeedsHandling) checkBluetooth()
        if (locationNeedsHandling) checkLocation()
    }

    private fun updateButton() {
        findViewById<Button>(android.R.id.button1).isEnabled = !permissionNeedsHandling && !bluetoothNeedsHandling && !locationNeedsHandling
    }

    // Permissions
    private var permissionNeedsHandling: Boolean = false
    private var permissionRequestCode = 33
    private val permissions by lazy {
        if (Build.VERSION.SDK_INT >= 29) {
            arrayOf("android.permission.ACCESS_BACKGROUND_LOCATION", "android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION")
        } else {
            arrayOf("android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION")
        }
    }
    private fun checkPermissions() {
        permissionNeedsHandling = Build.VERSION.SDK_INT >= 23 && permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        findViewById<View>(R.id.grant_permission_view).visibility = if (permissionNeedsHandling) View.VISIBLE else View.GONE
        updateButton()
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(permissions, ++permissionRequestCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.permissionRequestCode) checkPermissions()
    }

    // Bluetooth
    private var bluetoothNeedsHandling: Boolean = false
    private var bluetoothRequestCode = 112
    private fun checkBluetooth() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothNeedsHandling = adapter?.isEnabled != true
        findViewById<View>(R.id.enable_bluetooth_view).visibility = if (adapter?.isEnabled == false) View.VISIBLE else View.GONE
        updateButton()
    }

    private fun requestBluetooth() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        findViewById<View>(R.id.enable_bluetooth_spinner).visibility = View.VISIBLE
        findViewById<View>(R.id.enable_bluetooth_button).visibility = View.INVISIBLE
        lifecycleScope.launchWhenStarted {
            if (adapter != null && !adapter.enableAsync(this@ExposureNotificationsConfirmActivity)) {
                requestBluetoothViaIntent()
            } else {
                checkBluetooth()
            }
            findViewById<View>(R.id.enable_bluetooth_spinner).visibility = View.INVISIBLE
            findViewById<View>(R.id.enable_bluetooth_button).visibility = View.VISIBLE
        }
    }

    private fun requestBluetoothViaIntent() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(intent, ++bluetoothRequestCode)
    }

    // Location
    private var locationNeedsHandling: Boolean = false
    private var locationRequestCode = 231
    private fun checkLocation() {
        locationNeedsHandling = !LocationManagerCompat.isLocationEnabled(getSystemService(Context.LOCATION_SERVICE) as LocationManager)
        findViewById<View>(R.id.enable_location_view).visibility = if (locationNeedsHandling) View.VISIBLE else View.GONE
        updateButton()
    }

    private fun requestLocation() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivityForResult(intent, ++locationRequestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == bluetoothRequestCode) checkBluetooth()
    }

    override fun finish() {
        receiver?.send(resultCode, Bundle())
        super.finish()
    }
}
