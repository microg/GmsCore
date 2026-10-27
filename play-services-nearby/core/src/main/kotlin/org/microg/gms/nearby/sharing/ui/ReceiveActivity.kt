/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.nearby.sharing.FileMetadata
import org.microg.gms.nearby.core.R
import org.microg.gms.nearby.sharing.NearbySharingPreferences
import org.microg.gms.nearby.sharing.NearbySharingServer
import java.io.File

class ReceiveActivity : AppCompatActivity() {
    private var server: NearbySharingServer? = null
    private lateinit var prefs: NearbySharingPreferences

    private lateinit var visibleAsText: TextView
    private lateinit var waitingProgress: ProgressBar
    private lateinit var pinText: TextView
    private lateinit var transferProgress: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var btnDone: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nearby_receive_activity)

        prefs = NearbySharingPreferences(this)
        visibleAsText = findViewById(R.id.visible_as_text)
        waitingProgress = findViewById(R.id.waiting_progress)
        pinText = findViewById(R.id.pin_text)
        transferProgress = findViewById(R.id.transfer_progress)
        statusText = findViewById(R.id.status_text)
        btnDone = findViewById(R.id.btn_done)

        visibleAsText.text = getString(R.string.nearby_share_waiting_receive, prefs.deviceName)
        btnDone.setOnClickListener { finish() }

        startServer()
    }

    private fun startServer() {
        server = NearbySharingServer(this, prefs.deviceName, listener = object : NearbySharingServer.Listener {
            override fun onServerStarted(port: Int) {
                runOnUiThread {
                    visibleAsText.text = getString(R.string.nearby_share_waiting_receive, prefs.deviceName)
                }
            }

            override fun onConnectionInitiated(remoteDeviceName: String, pinCode: String) {
                runOnUiThread {
                    pinText.visibility = View.VISIBLE
                    pinText.text = getString(R.string.nearby_share_verify_pin, pinCode)
                    statusText.visibility = View.VISIBLE
                    statusText.text = getString(R.string.nearby_share_receiving, remoteDeviceName)
                }
            }

            override fun onTransferStarted(files: List<FileMetadata>) {
                runOnUiThread {
                    waitingProgress.visibility = View.GONE
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

            override fun onTransferCompleted(savedFiles: List<File>) {
                runOnUiThread {
                    transferProgress.progress = 100
                    statusText.text = getString(R.string.nearby_share_sent) + ": ${savedFiles.size} file(s) saved"
                }
            }

            override fun onError(error: Throwable) {
                runOnUiThread {
                    statusText.visibility = View.VISIBLE
                    statusText.text = "Error: ${error.localizedMessage}"
                }
            }
        })
        server?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
    }
}
