/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.utils

import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.pm.Signature
import android.util.Base64
import com.google.android.gms.common.internal.CertData
import java.security.MessageDigest
import java.util.*

fun PackageManager.isPlatformCertificate(cert: CertData) = getPlatformCertificates().contains(cert)
fun PackageManager.getPlatformCertificates() = getCertificates("android")

fun PackageManager.getCertificates(packageName: String): List<CertData> = try {
    getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures.map { CertData(it.toByteArray()) }
} catch (e: NameNotFoundException) {
    emptyList()
}

@Deprecated("It's actually a certificate", ReplaceWith("getCertificates"))
fun PackageManager.getSignatures(packageName: String): Array<Signature> = try {
    getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
} catch (e: NameNotFoundException) {
    emptyArray()
}

fun PackageManager.getApplicationLabel(packageName: String): CharSequence = try {
    getApplicationLabel(getApplicationInfo(packageName, 0))
} catch (e: Exception) {
    packageName
}

fun PackageManager.getExtendedPackageInfo(packageName: String) = ExtendedPackageInfo(this, packageName)

fun ByteArray.toBase64(vararg flags: Int): String = Base64.encodeToString(this, flags.fold(0) { a, b -> a or b })
fun ByteArray.toHexString(separator: String = ""): String = joinToString(separator) { "%02x".format(it) }

fun PackageManager.getFirstSignatureDigest(packageName: String, md: String): ByteArray? =
    getCertificates(packageName).firstOrNull()?.digest(md)

fun ByteArray.digest(md: String): ByteArray = MessageDigest.getInstance(md).digest(this)
@Deprecated("It's actually a certificate")
fun Signature.digest(md: String): ByteArray = toByteArray().digest(md)
fun CertData.digest(md: String): ByteArray = bytes.digest(md)
