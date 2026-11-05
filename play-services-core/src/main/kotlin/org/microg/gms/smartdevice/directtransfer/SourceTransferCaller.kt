/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import okio.ByteString
import okio.ByteString.Companion.toByteString

/** Identity captured on the Binder thread; never inferred from a caller-supplied package name. */
internal class SourceTransferCaller private constructor(
    val uid: Int,
    val packageName: String,
    private val certificate: ByteString
) {
    fun enforceInstalled(context: Context) {
        check(context.packageManager.getApplicationInfo(packageName, 0).uid == uid) { "Transfer caller changed" }
        check(currentCertificate(context) == certificate) { "Transfer caller signature changed" }
    }

    fun enforceBinder(context: Context) {
        check(Binder.getCallingUid() == uid) { "Different transfer caller" }
        enforceInstalled(context)
    }

    override fun toString() = "SourceTransferCaller(redacted)"

    companion object {
        const val PACKAGE = "com.google.android.apps.wear.companion"
        private const val CERTIFICATE = "48ed0058d1b6638e39a6e4c2df1c4d5fdf593f696bf31b09a88f93141eaf600f"

        fun capture(context: Context, suggestedPackage: String?): SourceTransferCaller {
            require(suggestedPackage == null || suggestedPackage == PACKAGE) { "Unsupported transfer caller" }
            val uid = Binder.getCallingUid()
            check(context.packageManager.getApplicationInfo(PACKAGE, 0).uid == uid) { "Unsupported transfer UID" }
            val certificate = currentCertificate(context)
            check(certificate.hex() == CERTIFICATE) { "Unsupported transfer signature" }
            return SourceTransferCaller(uid, PACKAGE, certificate)
        }

        @Suppress("DEPRECATION")
        private fun currentCertificate(context: Context): ByteString {
            val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
            val info = context.packageManager.getPackageInfo(PACKAGE, flags)
            val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
            check(signatures?.size == 1) { "Unsupported transfer signer set" }
            return requireNotNull(signatures).single().toByteArray().toByteString().sha256()
        }
    }
}
