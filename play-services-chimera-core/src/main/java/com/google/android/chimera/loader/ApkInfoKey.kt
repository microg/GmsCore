/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.loader

import com.google.android.chimera.config.registry.ApkRegistry
import com.google.android.chimera.config.registry.ApkType

class ApkInfoKey(val apkInfo: ApkRegistry.ApkInfo) {
    private val hashCode = when (apkInfo.apkType) {
        ApkType.CONTAINER -> listOf(
            apkInfo.apkType,
            apkInfo.versionCode,
            apkInfo.packageName,
        ).hashCode()
        else -> listOf(
            apkInfo.apkType,
            apkInfo.versionCode,
            apkInfo.sourceUri,
            apkInfo.moduleName,
            apkInfo.moduleVersion,
            apkInfo.apkSha256,
        ).hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ApkInfoKey) return false

        if (apkInfo.apkType != other.apkInfo.apkType) return false
        if (apkInfo.versionCode != other.apkInfo.versionCode) return false

        return when (apkInfo.apkType) {
            ApkType.CONTAINER -> {
                apkInfo.packageName == other.apkInfo.packageName
            }
            else -> {
                apkInfo.sourceUri == other.apkInfo.sourceUri
                        && apkInfo.moduleName == other.apkInfo.moduleName
                        && apkInfo.moduleVersion == other.apkInfo.moduleVersion
                        && apkInfo.apkSha256 == other.apkInfo.apkSha256
            }
        }
    }

    override fun hashCode(): Int {
        return hashCode
    }
}
