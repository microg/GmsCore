/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import org.microg.gms.common.*

class ExtendedPackageInfo(private val packageManager: PackageManager, val packageName: String) {
    constructor(context: Context, packageName: String) : this(context.packageManager, packageName)

    private val basicPackageInfo by lazy { kotlin.runCatching { packageManager.getPackageInfo(packageName, 0) }.getOrNull() }
    private val basicApplicationInfo by lazy { kotlin.runCatching { packageManager.getApplicationInfo(packageName, 0) }.getOrNull() }

    val isInstalled by lazy { basicPackageInfo != null }

    val certificates by lazy { packageManager.getCertificates(packageName) }
    private val certificatesHashSha1 by lazy { certificates.map { it.digest("SHA1") } }
    val firstCertificateSha1 by lazy { certificatesHashSha1.firstOrNull() }
    val firstCertificateSha1Hex by lazy { firstCertificateSha1?.toHexString() }
    private val certificatesHashSha256 by lazy { certificates.map { it.digest("SHA-256") } }
    val firstCertificateSha256 by lazy { certificatesHashSha256.firstOrNull() }
    private val certificatesHashSha1Strings by lazy { certificatesHashSha1.map { it.toHexString() } }
    private val certificatesHashSha256Strings by lazy { certificatesHashSha256.map { it.toHexString() } }

    val applicationLabel by lazy { packageManager.getApplicationLabel(packageName) }

    @Deprecated("version code is now a long", replaceWith = ReplaceWith("versionCode"))
    val shortVersionCode by lazy { basicPackageInfo?.versionCode ?: -1 }
    val versionCode by lazy { basicPackageInfo?.let { PackageInfoCompat.getLongVersionCode(it) } ?: -1 }
    val versionName by lazy { basicPackageInfo?.versionName }

    val targetSdkVersion by lazy { basicApplicationInfo?.targetSdkVersion ?: -1 }

    private val packageAndCertHashes by lazy {
        listOf(
            certificatesHashSha1Strings.map { PackageAndCertHash(packageName, "SHA1", it) },
            certificatesHashSha256Strings.map { PackageAndCertHash(packageName, "SHA-256", it) },
        ).flatten()
    }
    val isGooglePackage by lazy { packageAndCertHashes.any { isGooglePackage(it) } }
    val isPlatformPackage by lazy {
        val platformCertificates = packageManager.getPlatformCertificates()
        certificates.any { it in platformCertificates }
    }
    val isGoogleOrPlatformPackage by lazy { isGooglePackage || isPlatformPackage }

    private val googlePackagePermissions by lazy { packageAndCertHashes.flatMap { getGooglePackagePermissions(it) }.toSet() }
    fun hasGooglePackagePermission(permission: GooglePackagePermission) = permission in googlePackagePermissions
}