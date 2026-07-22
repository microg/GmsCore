/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.crossprofile

import android.app.Activity
import android.content.Intent
import android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.pm.CrossProfileApps
import android.os.Bundle
import android.os.UserManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import org.microg.gms.settings.SettingsContract.getAuthority

@RequiresApi(30)
class CrossProfileSendActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check that we are primary profile
        val userManager = getSystemService(UserManager::class.java)
        if (userManager.isManagedProfile) {
            Log.w(TAG, "Cross-profile send request was received on work profile!")
            finish()
            return
        }

        // Check prerequisites
        val crossProfileApps = getSystemService(CrossProfileApps::class.java)
        val targetProfiles = crossProfileApps.targetUserProfiles

        if (!crossProfileApps.canInteractAcrossProfiles() || targetProfiles.isEmpty()) {
            Log.w(
                TAG, "received cross-profile request, but I believe I cannot answer, as prerequisites are not met: " +
                    "can interact = ${crossProfileApps.canInteractAcrossProfiles()}, " +
                    "#targetProfiles = ${targetProfiles.size}. Note that this is expected during initial setup of a work profile.")
        }

        // Respond
        Log.d(TAG, "responding to cross-profile request")

        setResult(1, Intent().apply {
            setData("content://${getAuthority(this@CrossProfileSendActivity)}".toUri())
            addFlags(FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        })
        finish()
    }

    companion object {
        const val TAG = "GmsCrossProfileSend"
    }
}