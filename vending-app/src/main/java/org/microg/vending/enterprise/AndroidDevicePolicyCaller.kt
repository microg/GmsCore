/*
 * SPDX-FileCopyrightText: 2026 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.enterprise

import android.content.pm.PackageManager
import io.grpc.Status
import io.grpc.binder.SecurityPolicy

class AndroidDevicePolicyCaller(private val packageManager: PackageManager) : SecurityPolicy() {

    override fun checkAuthorization(uid: Int): Status {
        val packageNames = packageManager.getPackagesForUid(uid).orEmpty()

        if (ANDROID_DEVICE_POLICY in packageNames) {
            return Status.OK
        }

        return Status.PERMISSION_DENIED.withDescription("Only Android Device Policy can request a policy sync")
    }

    companion object {
        private const val ANDROID_DEVICE_POLICY = "com.google.android.apps.work.clouddpc"
    }
}
