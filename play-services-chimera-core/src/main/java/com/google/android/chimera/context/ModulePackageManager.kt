/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.chimera.context

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import org.microg.gms.utils.PackageManagerWrapper

class ModulePackageManager(
    private val delegate: PackageManager
) : PackageManagerWrapper(delegate) {

    companion object {
        private const val TAG = "ModulePackageManager"

        private const val PRIVILEGED_FLAGS = (
            0x00100000   // MATCH_HIDDEN_UNTIL_INSTALLED_COMPONENTS
            or 0x00200000 // MATCH_INSTANT
            or 0x20000000 // MATCH_APEX
        )
    }

    override fun getApplicationInfo(packageName: String, flags: Int): ApplicationInfo {
        try {
            return delegate.getApplicationInfo(packageName, flags)
        } catch (e: PackageManager.NameNotFoundException) {
            val strippedFlags = flags and PRIVILEGED_FLAGS.inv()
            if (strippedFlags != flags) {
                Log.d(TAG, "getApplicationInfo($packageName) failed with flags 0x${Integer.toHexString(flags)}, retrying with 0x${Integer.toHexString(strippedFlags)}")
                return delegate.getApplicationInfo(packageName, strippedFlags)
            }
            throw e
        }
    }

    override fun getPackageInfo(packageName: String, flags: Int): PackageInfo {
        try {
            return delegate.getPackageInfo(packageName, flags)
        } catch (e: PackageManager.NameNotFoundException) {
            val strippedFlags = flags and PRIVILEGED_FLAGS.inv()
            if (strippedFlags != flags) {
                Log.d(TAG, "getPackageInfo($packageName) failed with flags 0x${Integer.toHexString(flags)}, retrying with 0x${Integer.toHexString(strippedFlags)}")
                return delegate.getPackageInfo(packageName, strippedFlags)
            }
            throw e
        }
    }
}
