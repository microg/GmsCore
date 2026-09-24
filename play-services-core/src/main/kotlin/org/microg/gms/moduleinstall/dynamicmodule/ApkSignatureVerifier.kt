/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.moduleinstall.dynamicmodule

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.util.Log
import org.microg.gms.common.KNOWN_GOOGLE_CERT_SHA256
import java.io.File
import java.security.MessageDigest

/**
 * Import-time trust gate: a module apk is accepted only if it is signed *solely* by Google.
 *
 * Trust is per-apk, not per-container: every module apk inside the imported bundle must itself carry a
 * Google signing certificate (the bundle as a whole is not signed). Google signs its GMS/Chimera modules
 * with more than one key (the main GMS release cert for MLKit-style modules, the `gmscore_modules_percy`
 * cert for Dynamite/Chimera integ modules, etc.), so the allowlist holds every known Google module cert.
 *
 * On API 28+, getPackageArchiveInfo runs PackageParser.collectCertificates, which cryptographically
 * verifies the apk's v2/v3 (or v1) signature against the apk contents before returning the certificate —
 * so a non-Google apk cannot make the PackageManager report a Google cert it does not actually carry.
 */
object ApkSignatureVerifier {
    private const val TAG = "GmsModule/ApkSig"

    /**
     * SHA-256 (lowercase hex) of every Google signing certificate accepted for module import. Reuses the
     * shared Google cert allowlist [KNOWN_GOOGLE_CERT_SHA256] (the privileged platform certs `f0fd6c5b…`,
     * `7ce83c1b…` + the official-apps/DroidGuard cert `3d7a1223…`) — one source of truth with the rest of
     * GmsCore — plus the module-only `gmscore_modules_percy` cert `afee62fb…`, which signs Dynamite/Chimera
     * integ modules but no Google app (so it is intentionally not in KnownGooglePackages). All confirmed with
     * keytool against the genuine GMS module apks. An unexpected reject of a known-good Google bundle prints
     * the offending signer hash via [isGoogleCert]'s Log.d — the signal to add it.
     */
    private val GOOGLE_CERT_SHA256: Set<String> = KNOWN_GOOGLE_CERT_SHA256 +
        "afee62fb653c9d863d1a6d6046b35225bf6380dd4657b105e8331a1de49f9f91"

    /**
     * Returns true iff [apkFile] is signed *solely* by Google. The apk's signing certificate(s) are read
     * without installing it; for a multi-signer apk EVERY signer must be a known Google cert (a single
     * Google signer alongside an attacker key is rejected), while for a single-signer apk any cert in its
     * v3 signing lineage being Google is sufficient (so Google key-rotation does not false-reject).
     */
    fun isGoogleSignedApk(context: Context, apkFile: File): Boolean {
        val pm = context.packageManager
        val path = apkFile.absolutePath
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageArchiveInfo(path, PackageManager.GET_SIGNING_CERTIFICATES)
                val si = info?.signingInfo ?: return logNoCert(apkFile)
                if (si.hasMultipleSigners()) {
                    // Multi-signer: a trust gate must require EVERY current signer to be Google, otherwise an
                    // apk co-signed by Google plus an attacker key would pass.
                    val signers = si.apkContentsSigners
                    signers.isNotEmpty() && signers.all { isGoogleCert(it, apkFile) }
                } else {
                    // Single-signer: use signingCertificateHistory (intentional — NOT apkContentsSigners) so an
                    // allowlisted ancestor still matches after Google rotates the key. The history is the v3
                    // signing lineage, cryptographically chained, so an attacker cannot insert a Google cert
                    // into it without Google's private key; matching any lineage cert is therefore safe here.
                    si.signingCertificateHistory?.any { isGoogleCert(it, apkFile) } ?: logNoCert(apkFile)
                }
            } else {
                // Pre-P: only the v1 (JAR) signing certificate is available. Require every signer to be Google.
                @Suppress("DEPRECATION")
                val info = pm.getPackageArchiveInfo(path, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                val sigs = info?.signatures ?: return logNoCert(apkFile)
                sigs.isNotEmpty() && sigs.all { isGoogleCert(it, apkFile) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "failed to read signature of ${apkFile.name}: ${e.message}")
            false
        }
    }

    /** True iff [sig]'s certificate SHA-256 is in the Google allowlist; logs the hash otherwise. */
    private fun isGoogleCert(sig: Signature, apkFile: File): Boolean {
        val hex = MessageDigest.getInstance("SHA-256").digest(sig.toByteArray())
            .joinToString("") { "%02x".format(it) }
        if (hex in GOOGLE_CERT_SHA256) return true
        Log.d(TAG, "non-Google signer in ${apkFile.name}: $hex")
        return false
    }

    private fun logNoCert(apkFile: File): Boolean {
        Log.w(TAG, "no signing certificate found in ${apkFile.name}")
        return false
    }
}
