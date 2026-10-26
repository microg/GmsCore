/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.utils

import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi
import com.google.android.gms.common.internal.CertData
import java.security.MessageDigest
import java.util.*

fun PackageManager.isPlatformCertificate(cert: CertData) = getPlatformCertificates().contains(cert)
fun PackageManager.getPlatformCertificates() = getCertificates("android")

fun PackageManager.getCertificates(packageName: String): List<CertData> =
    getSignatures(packageName).map { CertData(it.toByteArray()) }

@Deprecated("It's actually a certificate", ReplaceWith("getCertificates"))
fun PackageManager.getSignatures(packageName: String): Array<Signature> = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        getSignInfo(packageName)?.signingCertificateHistory
    } else {
        null
    } ?: getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
    ?: emptyArray()
} catch (e: NameNotFoundException) {
    emptyArray()
}

@RequiresApi(Build.VERSION_CODES.P)
fun PackageManager.getSignInfo(packageName: String): SigningInfo? = runCatching {
    getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo
}.getOrNull()


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
