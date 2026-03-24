/*
 * Copyright (C) 2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.microg.gms.people

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import org.microg.gms.auth.phone.EXTRA_PERMISSIONS
import org.microg.gms.location.manager.AskPermissionActivity

private const val TAG = "ContactSyncService"

private val REQUIRED_PERMISSIONS = arrayOf("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS")

class ContactSyncService : Service() {

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind: ")
        if (isMissingPermissions(this)) {
            Log.d(TAG, "onPerformSync isMissingPermissions")
            val permissionIntent = Intent(this, AskPermissionActivity::class.java)
            permissionIntent.putExtra(EXTRA_PERMISSIONS, arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS))
            permissionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(permissionIntent)
        }
        return SyncAdapterProxy.get(this).syncAdapterBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind: ")
        return super.onUnbind(intent)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun isMissingPermissions(context: Context): Boolean {
        for (str in REQUIRED_PERMISSIONS) {
            if (context.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                return true
            }
        }
        return false
    }

}