/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.component

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.chimera.android.Activity
import com.google.android.chimera.config.ModuleDownloadRegistry

private const val REQUEST_MODULE_DOWNLOAD = 0x4349
private const val STATE_DOWNLOAD_FLOW_LAUNCHED = "download_flow_launched"

open class ChimeraFallbackActImpl : Activity() {

    private var requestedFeatureNames: String? = null
    private var downloadFlowLaunched = false

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        downloadFlowLaunched = bundle?.getBoolean(STATE_DOWNLOAD_FLOW_LAUNCHED) ?: false
        Log.d(TAG, "onCreate ${getContainerActivity().javaClass.name}")

        val containerActivity = getContainerActivity()
        requestedFeatureNames = ModuleDownloadRegistry.requestedFeatureNamesForActivity(
            containerActivity,
            containerActivity.javaClass.name
        )
        Log.d(TAG, "Requested features: $requestedFeatureNames")
    }

    override fun onResume() {
        super.onResume()
        if (downloadFlowLaunched) return
        downloadFlowLaunched = true

        // The shared page explains the module and its permissions, then enforces authorization before download.
        Log.w(TAG, "Module not available locally, features: $requestedFeatureNames")
        val containerActivity = getContainerActivity()
        val downloadIntent = ModuleDownloadRegistry.createModuleDownloadIntent(
            containerActivity,
            requestedFeatureNames
        )
        if (downloadIntent == null) {
            Log.w(TAG, "No module download configured for features: $requestedFeatureNames")
            finish()
            return
        }
        // Keep the original container Activity (and therefore its caller/result chain) alive while
        // the user downloads and imports the module. ModuleDownloadActivity only returns RESULT_OK
        // after it has verified that the requested feature is now installed.
        startActivityForResult(downloadIntent, REQUEST_MODULE_DOWNLOAD)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_DOWNLOAD_FLOW_LAUNCHED, downloadFlowLaunched)
        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQUEST_MODULE_DOWNLOAD) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }

        if (resultCode == RESULT_OK) {
            // Re-resolve the dynamic implementation in the same container Activity. Recreating it
            // preserves the external caller and delivers the module's eventual result normally.
            getContainerActivity().recreate()
        } else {
            finish()
        }
    }
}
