/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.microg.gms.nearby.core.R
import org.microg.gms.nearby.sharing.NearbySharingPreferences

class NearbySharingSettingsActivity : AppCompatActivity() {
    private lateinit var prefs: NearbySharingPreferences
    private lateinit var textDeviceName: TextView
    private lateinit var textVisibility: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nearby_settings_activity)

        prefs = NearbySharingPreferences(this)
        textDeviceName = findViewById(R.id.text_device_name)
        textVisibility = findViewById(R.id.text_visibility)

        findViewById<LinearLayout>(R.id.row_device_name).setOnClickListener {
            showDeviceNameDialog()
        }

        findViewById<LinearLayout>(R.id.row_visibility).setOnClickListener {
            showVisibilityDialog()
        }

        updateUi()
    }

    private fun updateUi() {
        textDeviceName.text = prefs.deviceName
        textVisibility.text = when (prefs.visibility) {
            NearbySharingPreferences.VISIBILITY_EVERYONE -> getString(R.string.nearby_share_visibility_everyone)
            NearbySharingPreferences.VISIBILITY_CONTACTS -> getString(R.string.nearby_share_visibility_contacts)
            else -> getString(R.string.nearby_share_visibility_hidden)
        }
    }

    private fun showDeviceNameDialog() {
        val input = EditText(this).apply {
            setText(prefs.deviceName)
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.nearby_share_device_name)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    prefs.deviceName = newName
                    updateUi()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showVisibilityDialog() {
        val options = arrayOf(
            getString(R.string.nearby_share_visibility_everyone),
            getString(R.string.nearby_share_visibility_contacts),
            getString(R.string.nearby_share_visibility_hidden)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.nearby_share_visibility)
            .setSingleChoiceItems(options, prefs.visibility) { dialog, which ->
                prefs.visibility = which
                updateUi()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
