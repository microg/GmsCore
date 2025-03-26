/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.crossprofile

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserManager
import android.util.Log

class UserInitReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver") // exported="false"
    override fun onReceive(context: Context, intent: Intent?) {

        // Check that we are work profile
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val userManager = context.getSystemService(UserManager::class.java)
            if (userManager.isManagedProfile) {
                Log.d(TAG, "A new managed profile is being initialized; telling `CrossProfileRequestActivity` to request access to main profile's data.")
                // CrossProfileActivity will check whether permissions are present
                context.startActivity(
                    Intent(context, CrossProfileRequestActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } else {
                Log.d(TAG, "A new user is being initialized, but it is not a managed profile. Not connecting data")
            }
        }
    }

    companion object {
        const val TAG = "GmsUserInit"
    }
}