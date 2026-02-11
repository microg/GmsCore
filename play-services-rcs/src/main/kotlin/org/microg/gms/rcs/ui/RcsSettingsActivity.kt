/*
 * Copyright 2024-2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.rcs.IRcsProvisioningCallback
import com.google.android.material.switchmaterial.SwitchMaterial
import org.microg.gms.rcs.DeviceIdentifierHelper
import org.microg.gms.rcs.R
import org.microg.gms.rcs.RcsProvisioningManager
import com.google.android.material.textfield.TextInputEditText
import org.microg.gms.rcs.config.ConfigKeys
import org.microg.gms.rcs.config.RcsConfigManager
import org.microg.gms.rcs.di.RcsServiceLocator
import org.microg.gms.rcs.di.inject
import org.microg.gms.rcs.state.RcsStateMachine
import org.microg.gms.rcs.RcsImplementation

class RcsSettingsActivity : AppCompatActivity() {

    private lateinit var switchEnableRcs: SwitchMaterial
    private lateinit var textServiceStatus: TextView
    private lateinit var textProvisioningStatus: TextView
    private lateinit var textRegistrationStatus: TextView
    private lateinit var btnForceProvisioning: Button
    private lateinit var btnResetConfig: Button
    private lateinit var inputPhoneNumber: TextInputEditText
    private lateinit var inputRecipient: TextInputEditText
    private lateinit var btnSendTestMessage: Button

    private val configManager by lazy { RcsConfigManager.getInstance(this) }


    // Inject dependencies
    private val provisioningManager: RcsProvisioningManager by inject()
    private val rcsStateMachine: RcsStateMachine by inject()
    private val rcsImplementation: RcsImplementation by inject()

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            updateStatus()
            handler.postDelayed(this, 2000) // Refresh every 2 seconds
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(org.microg.gms.rcs.R.layout.activity_rcs_settings)

        // Ensure ServiceLocator is initialized (in case Service hasn't started yet)
        RcsServiceLocator.initialize(this)

        bindViews()
        setupListeners()
        checkPermissions()
    }

    private fun bindViews() {
        switchEnableRcs = findViewById<SwitchMaterial>(org.microg.gms.rcs.R.id.switchEnableRcs)
        textServiceStatus = findViewById(org.microg.gms.rcs.R.id.textServiceStatus)
        textProvisioningStatus = findViewById(org.microg.gms.rcs.R.id.textProvisioningStatus)
        textRegistrationStatus = findViewById(org.microg.gms.rcs.R.id.textRegistrationStatus)
        inputPhoneNumber = findViewById(org.microg.gms.rcs.R.id.inputPhoneNumber)
        inputRecipient = findViewById(org.microg.gms.rcs.R.id.inputRecipient)
        btnForceProvisioning = findViewById(org.microg.gms.rcs.R.id.btnForceProvisioning)
        btnResetConfig = findViewById(org.microg.gms.rcs.R.id.btnResetConfig)
        btnSendTestMessage = findViewById(org.microg.gms.rcs.R.id.btnSendTestMessage)
    }

    private fun setupListeners() {
        switchEnableRcs.setOnCheckedChangeListener { _, isChecked ->
            configManager.set(ConfigKeys.RCS_ENABLED, isChecked)
        }

        btnForceProvisioning.setOnClickListener {
            forceProvisioning()
        }

        btnResetConfig.setOnClickListener {
            resetConfiguration()
        }
        
        btnSendTestMessage.setOnClickListener {
            sendTestMessage()
        }
    }

    private fun checkPermissions() {
        if (!DeviceIdentifierHelper.hasReadDeviceIdentifiersPermission(this)) {
            val command = "adb shell appops set ${packageName} READ_DEVICE_IDENTIFIERS allow"
            
            // Show dialog or toast with instructions
            Toast.makeText(this, "Permission Missing! Copied ADB command to clipboard.", Toast.LENGTH_LONG).show()
            
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ADB Command", command)
            clipboard.setPrimaryClip(clip)
        }
    }

    override fun onResume() {
        super.onResume()
        
        switchEnableRcs.isChecked = configManager.getBoolean(ConfigKeys.RCS_ENABLED, true)
        
        updateStatus()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    private fun updateStatus() {
        // Update Provisioning Status
        val provStatus = try {
            provisioningManager.getProvisioningStatus()
        } catch (e: Exception) {
            -1 // Unknown
        }
        
        textProvisioningStatus.text = when (provStatus) {
            IRcsProvisioningCallback.STATUS_PROVISIONED -> getString(org.microg.gms.rcs.R.string.status_provisioned)
            IRcsProvisioningCallback.STATUS_PROVISIONING -> getString(org.microg.gms.rcs.R.string.status_connecting)
            IRcsProvisioningCallback.STATUS_NOT_PROVISIONED -> getString(org.microg.gms.rcs.R.string.status_unprovisioned)
            IRcsProvisioningCallback.STATUS_ERROR -> "Error"
            else -> getString(org.microg.gms.rcs.R.string.status_unknown)
        }

        // Update Phone Number
        val phone = try {
            provisioningManager.getRegisteredPhoneNumber()
        } catch (e: Exception) { null }
        
        if (provStatus == IRcsProvisioningCallback.STATUS_PROVISIONED && !phone.isNullOrEmpty()) {
             textProvisioningStatus.append(" ($phone)")
        }

        // Update Registration Status (from StateMachine)
        // Note: You might need to expose public state from RcsStateMachine
        textRegistrationStatus.text = "Polling..." 
    }

    private fun forceProvisioning() {
        val manualPhone = inputPhoneNumber.text?.toString()
        if (!manualPhone.isNullOrBlank()) {
            provisioningManager.setPreferredPhoneNumber(manualPhone)
        }
        
        Toast.makeText(this, "Starting Provisioning...", Toast.LENGTH_SHORT).show()
        try {
            provisioningManager.refreshRegistration(object : IRcsProvisioningCallback.Stub() {
                override fun onProvisioningComplete(phoneNumber: String?) {
                    runOnUiThread { Toast.makeText(applicationContext, "Success: $phoneNumber", Toast.LENGTH_SHORT).show() }
                }

                override fun onProvisioningError(code: Int, msg: String?) {
                    runOnUiThread { Toast.makeText(applicationContext, "Error $code: $msg", Toast.LENGTH_LONG).show() }
                }

                override fun onProvisioningProgress(progress: Int, msg: String?) {
                    // Optional: Show progress bar
                }
                
                override fun onProvisioningStatus(status: Int) {}
            })
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetConfiguration() {
        try {
            provisioningManager.clearProvisioning()
            Toast.makeText(this, "Configuration Cleared", Toast.LENGTH_SHORT).show()
            updateStatus()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to reset: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun sendTestMessage() {
        val recipient = inputRecipient.text?.toString() ?: ""
        if (recipient.isBlank()) {
            Toast.makeText(this, "Please enter a recipient number", Toast.LENGTH_SHORT).show()
            return
        }
        
        Toast.makeText(this, "Sending...", Toast.LENGTH_SHORT).show()
        
        Thread {
            try {
                val config = provisioningManager.loadConfiguration()
                val realm = config?.sipRealm ?: "unknown"
                val targetUri = if (recipient.contains("sip:")) recipient else "sip:$recipient@$realm"
                
                val result = rcsImplementation.sendMessage(targetUri, "Test message from MicroG RCS")
                
                runOnUiThread {
                    if (result.isSuccessful) {
                         Toast.makeText(this, "Success! Delivered.", Toast.LENGTH_LONG).show()
                    } else {
                         Toast.makeText(this, "Failed: ${result.errorCode} - ${result.errorMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                     Toast.makeText(this, "Exception: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}
