/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.microg.gms.nearby.core.R
import org.microg.gms.nearby.sharing.NearbyDevice
import org.microg.gms.nearby.sharing.NearbySharingClient
import org.microg.gms.nearby.sharing.NearbySharingPreferences
import org.microg.gms.nearby.sharing.NearbySharingScanner
import java.io.File
import java.io.FileOutputStream

class NearbySharingActivity : AppCompatActivity() {
    private var scanner: NearbySharingScanner? = null
    private var client: NearbySharingClient? = null
    private lateinit var prefs: NearbySharingPreferences

    private lateinit var statusText: TextView
    private lateinit var searchProgress: ProgressBar
    private lateinit var pinText: TextView
    private lateinit var transferProgress: ProgressBar
    private lateinit var devicesContainer: LinearLayout
    private lateinit var btnCancel: Button

    private val filesToSend = mutableListOf<File>()
    private var textToSend: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nearby_sharing_activity)

        prefs = NearbySharingPreferences(this)
        statusText = findViewById(R.id.status_text)
        searchProgress = findViewById(R.id.search_progress)
        pinText = findViewById(R.id.pin_text)
        transferProgress = findViewById(R.id.transfer_progress)
        devicesContainer = findViewById(R.id.devices_container)
        btnCancel = findViewById(R.id.btn_cancel)

        btnCancel.setOnClickListener { finish() }

        parseIntent(intent)

        startDiscovery()
    }

    private fun parseIntent(intent: Intent?) {
        if (intent == null) return

        if (intent.hasExtra(Intent.EXTRA_TEXT)) {
            textToSend = intent.getStringExtra(Intent.EXTRA_TEXT)
        }

        val streamUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        if (streamUri != null) {
            resolveUriToFile(streamUri)?.let { filesToSend.add(it) }
        }

        val streamUris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        if (streamUris != null) {
            for (uri in streamUris) {
                resolveUriToFile(uri)?.let { filesToSend.add(it) }
            }
        }
    }

    private fun resolveUriToFile(uri: Uri): File? {
        if (uri.scheme == "file") {
            return uri.path?.let { File(it) }
        }
        return try {
            val contentResolver = contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileName = uri.lastPathSegment ?: "shared_file_${System.currentTimeMillis()}"
            val tempFile = File(cacheDir, fileName)
            tempFile.outputStream().use { fos ->
                inputStream.copyTo(fos)
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    private fun startDiscovery() {
        scanner = NearbySharingScanner(
            this,
            onDeviceFound = { device ->
                runOnUiThread { addDeviceView(device) }
            },
            onDeviceLost = { device ->
                runOnUiThread { removeDeviceView(device) }
            }
        )
        scanner?.startDiscovery()
    }

    private fun addDeviceView(device: NearbyDevice) {
        val view = TextView(this).apply {
            text = "📱  ${device.name}"
            textSize = 18f
            setPadding(16, 24, 16, 24)
            setBackgroundResource(android.R.drawable.list_selector_background)
            setOnClickListener {
                startTransfer(device)
            }
        }
        view.tag = device.id
        devicesContainer.addView(view)
    }

    private fun removeDeviceView(device: NearbyDevice) {
        val child = devicesContainer.findViewWithTag<View>(device.id)
        if (child != null) {
            devicesContainer.removeView(child)
        }
    }

    private fun startTransfer(device: NearbyDevice) {
        scanner?.stopDiscovery()
        devicesContainer.visibility = View.GONE
        searchProgress.visibility = View.GONE
        statusText.text = getString(R.string.nearby_share_connecting, device.name)

        client = NearbySharingClient(this, prefs.deviceName, object : NearbySharingClient.Listener {
            override fun onConnectionInitiated(targetDevice: NearbyDevice, pinCode: String) {
                runOnUiThread {
                    pinText.visibility = View.VISIBLE
                    pinText.text = getString(R.string.nearby_share_verify_pin, pinCode)
                }
            }

            override fun onTransferAccepted() {
                runOnUiThread {
                    statusText.text = getString(R.string.nearby_share_sending)
                    transferProgress.visibility = View.VISIBLE
                }
            }

            override fun onTransferProgress(bytesTransferred: Long, totalBytes: Long) {
                runOnUiThread {
                    if (totalBytes > 0) {
                        transferProgress.progress = ((bytesTransferred * 100) / totalBytes).toInt()
                    }
                }
            }

            override fun onTransferCompleted() {
                runOnUiThread {
                    statusText.text = getString(R.string.nearby_share_sent)
                    transferProgress.progress = 100
                    btnCancel.text = getString(android.R.string.ok)
                }
            }

            override fun onError(error: Throwable) {
                runOnUiThread {
                    statusText.text = "Error: ${error.localizedMessage}"
                    pinText.visibility = View.GONE
                }
            }
        })

        lifecycleScope.launch {
            client?.sendFiles(device, filesToSend, textToSend)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scanner?.stopDiscovery()
    }
}
