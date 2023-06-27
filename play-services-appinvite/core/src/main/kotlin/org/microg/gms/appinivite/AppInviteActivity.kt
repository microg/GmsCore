/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.appinivite

import android.app.Activity
import android.os.Bundle
import android.util.Log

private const val TAG = "AppInviteActivity"

class AppInviteActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent?.data
        if (uri == null) {
            finish()
            return
        }
        Log.d(TAG, "uri: $uri")
        // TODO datamixer-pa.googleapis.com/
    }
}