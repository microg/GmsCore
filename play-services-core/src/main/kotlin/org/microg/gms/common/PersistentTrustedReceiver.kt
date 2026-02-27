/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class PersistentTrustedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TrustedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "Package changed: $intent")
        val action = intent?.action ?: return
        val pkg = intent.data?.schemeSpecificPart ?: return

        if ((Intent.ACTION_PACKAGE_REMOVED.contains(action)
                    && intent.getBooleanExtra(Intent.EXTRA_DATA_REMOVED, false)
                    && !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false))
            || Intent.ACTION_PACKAGE_FULLY_REMOVED.contains(action)
            || Intent.ACTION_PACKAGE_DATA_CLEARED.contains(action)
        ) {
            Log.d(TAG, "Package removed or data cleared: $pkg")
            val data = Data.Builder()
                .putString(PackageIntentOpWorker.PACKAGE_NAME, pkg)
                .build()
            val request = OneTimeWorkRequestBuilder<PackageIntentOpWorker>()
                .setInputData(data)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }

}