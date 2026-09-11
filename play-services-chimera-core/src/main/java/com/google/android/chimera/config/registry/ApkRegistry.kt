/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.config.registry

object ApkRegistry {
    class ApkInfo(
        val apkType: ApkType,
        val apkPath: String,
        val moduleApiClassName: String,
        val packageName: String,
        val versionCode: String,
        val dependCount: Int,
        val dependModuleNames: Array<String>,
        val moduleType: Int,
        val sourceUri: String,
        val moduleName: String,
        val moduleVersion: String,
        val apkSha256: String,
    )

    fun createDynamicApkInfo(
        apkPath: String,
        moduleName: String,
        moduleVersion: String,
        moduleApiClassName: String,
        sourceUri: String = "",
        apkSha256: String = ""
    ): ApkInfo {
        return ApkInfo(
            ApkType.FILE, apkPath, moduleApiClassName,
            "com.google.android.gms", "0",
            1, arrayOf("ROOT"),
            0, sourceUri.ifEmpty { "file://$apkPath" }, moduleName, moduleVersion, apkSha256
        )
    }
}
