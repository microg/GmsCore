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

private val PERMISSION_TAG: String = ChimeraPermissionActImpl::class.java.simpleName
private const val REQUEST_MODULE_PERMISSION = 0x4348
private const val STATE_PERMISSION_FLOW_LAUNCHED = "permission_flow_launched"

/** Handles an installed Chimera Activity whose runtime permission was revoked after installation. */
open class ChimeraPermissionActImpl : Activity() {
    private var requestedFeatureNames: String? = null
    private var permissionFlowLaunched = false

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        permissionFlowLaunched = bundle?.getBoolean(STATE_PERMISSION_FLOW_LAUNCHED) ?: false
        val containerActivity = getContainerActivity()
        requestedFeatureNames = ModuleDownloadRegistry.requestedFeatureNamesForActivity(
            containerActivity,
            containerActivity.javaClass.name
        )
        Log.d(PERMISSION_TAG, "Requested features: $requestedFeatureNames")
    }

    override fun onResume() {
        super.onResume()
        if (permissionFlowLaunched) return
        permissionFlowLaunched = true

        val containerActivity = getContainerActivity()
        val permissionIntent = ModuleDownloadRegistry.createModulePermissionIntent(
            containerActivity,
            requestedFeatureNames
        )
        if (permissionIntent == null) {
            Log.w(PERMISSION_TAG, "No permission flow configured for features: $requestedFeatureNames")
            finish()
            return
        }
        startActivityForResult(permissionIntent, REQUEST_MODULE_PERMISSION)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_PERMISSION_FLOW_LAUNCHED, permissionFlowLaunched)
        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQUEST_MODULE_PERMISSION) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }

        if (resultCode == RESULT_OK) {
            // Recreate the same container Activity so its original caller and result chain remain intact.
            getContainerActivity().recreate()
        } else {
            finish()
        }
    }
}
