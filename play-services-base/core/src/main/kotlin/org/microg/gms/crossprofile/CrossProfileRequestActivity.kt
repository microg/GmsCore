/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.crossprofile

import android.app.Activity
import android.content.Intent
import android.content.pm.CrossProfileApps
import android.os.Bundle
import android.os.UserManager
import android.util.Log
import androidx.annotation.RequiresApi
import org.microg.gms.settings.SettingsContract.CROSS_PROFILE_PERMISSION
import org.microg.gms.settings.SettingsContract.CROSS_PROFILE_SHARED_PREFERENCES_NAME
import androidx.core.content.edit

/**
 * Two-step process:
 *   1. request to hear back from `CrossProfileRequestActivity`
 *   2. receive resulting URI as intent data
 *
 * This dance so complicated because Android platform does not offer better APIs that only need
 * `INTERACT_ACROSS_PROFILES`, an appops permission (and not `INTERACT_ACROSS_USERS`, a
 * privileged|system permission).
 */
@RequiresApi(30)
class CrossProfileRequestActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check that we are work profile
        val userManager = getSystemService(UserManager::class.java)
        if (!userManager.isManagedProfile) {
            Log.w(CrossProfileSendActivity.TAG, "I was asked to send a cross-profile request, but I am not on a work profile!")
            finish()
            return
        }

        val crossProfileApps = getSystemService(CrossProfileApps::class.java)

        val targetProfiles = crossProfileApps.targetUserProfiles

        if (!crossProfileApps.canInteractAcrossProfiles() || targetProfiles.isEmpty()) {
            Log.w(
                TAG, "I am supposed to send a cross-profile request, but the prerequisites are not met: " +
                        "can interact = ${crossProfileApps.canInteractAcrossProfiles()}, " +
                        "#targetProfiles = ${targetProfiles.size}")
            finish()
            return
        }

        val intent = Intent(this, CrossProfileSendActivity::class.java)

        Log.d(TAG, "asking for cross-profile URI")
        crossProfileApps.startActivity(
            intent,
            targetProfiles.first(),
            // if this parameter is provided, it works like `startActivityForResult` (with requestCode 0)
            this
        )

        // finish only after receiving result
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, data?.data.toString())

        val uri = data?.data
        if (uri == null) {
            Log.w(TAG, "expected to receive data, but intent did not contain any.")
            finish()
            return
        }

        contentResolver.takePersistableUriPermission(uri, 0)

        val preferences = getSharedPreferences(CROSS_PROFILE_SHARED_PREFERENCES_NAME, MODE_PRIVATE)
        Log.i(TAG, "storing work URI")
        preferences.edit { putString(CROSS_PROFILE_PERMISSION, uri.toString()) }

        finish()
    }

    companion object {
        const val TAG = "GmsCrossProfileRequest"
    }
}