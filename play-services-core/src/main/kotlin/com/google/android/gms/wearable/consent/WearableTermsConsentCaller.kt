/* SPDX-License-Identifier: Apache-2.0 */
package com.google.android.gms.wearable.consent

import android.content.Context
import android.content.pm.PackageManager
import org.microg.gms.common.GooglePackagePermission
import org.microg.gms.utils.ExtendedPackageInfo

/** Snapshot of the framework-provided activity caller, not of an Intent extra or a Binder thread. */
class WearableTermsConsentCaller private constructor(
    private val packageName: String,
    private val uid: Int,
    private val certificate: ByteArray
) {
    fun enforceCurrent(context: Context) {
        try {
            if (context.packageManager.getApplicationInfo(packageName, 0).uid != uid ||
                !certificate.contentEquals(certificate(context, packageName))) throw SecurityException("Terms caller changed")
        } catch (_: PackageManager.NameNotFoundException) { throw SecurityException("Terms caller unavailable") }
    }

    override fun toString() = "WearableTermsConsentCaller(redacted)"

    companion object {
        fun capture(context: Context, packageName: String): WearableTermsConsentCaller {
            if (packageName != "com.google.android.apps.wear.companion" && packageName != "com.google.android.wearable.app") {
                throw SecurityException("Unsupported terms caller")
            }
            try {
                return WearableTermsConsentCaller(packageName,
                    context.packageManager.getApplicationInfo(packageName, 0).uid,
                    certificate(context, packageName).copyOf()).also { it.enforceCurrent(context) }
            } catch (_: PackageManager.NameNotFoundException) { throw SecurityException("Terms caller unavailable") }
        }

        private fun certificate(context: Context, packageName: String): ByteArray {
            val info = ExtendedPackageInfo(context, packageName)
            // Existing package-and-certificate allowlist; this does not grant account permissions.
            if (!info.hasGooglePackagePermission(GooglePackagePermission.ACCOUNT) || info.certificates.size != 1) {
                throw SecurityException("Untrusted terms caller")
            }
            return info.firstCertificateSha256 ?: throw SecurityException("Unavailable terms signer")
        }
    }
}
