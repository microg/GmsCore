/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.backup.component

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.R

class BackupSettingsCollapsingActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(this, getString(R.string.backup_disabled), Toast.LENGTH_SHORT).show()
        finish()
    }
}
