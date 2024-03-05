/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.backup.component

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.R
import org.microg.gms.profile.Build
import org.microg.gms.profile.ProfileManager

private val ACTION_BACKUP = hashMapOf(
    "HUAWEI" to "com.huawei.KoBackup.StartHwBackup"
)

class BackupSettingsCollapsingActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProfileManager.ensureInitialized(this)

        val intent = Intent()
        intent.setAction(ACTION_BACKUP[Build.BRAND])
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, getString(R.string.backup_disabled), Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
