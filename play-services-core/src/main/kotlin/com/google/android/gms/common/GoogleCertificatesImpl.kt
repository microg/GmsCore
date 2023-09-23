/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.common

import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import androidx.annotation.Keep
import com.google.android.gms.common.internal.CertData
import com.google.android.gms.common.internal.IGoogleCertificatesApi
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "GoogleCertificates"

@Keep
class GoogleCertificatesImpl : IGoogleCertificatesApi.Stub() {
    private val googleCertificates = emptySet<CertData>()
    private val googleReleaseCertificates = emptySet<CertData>()

    override fun getGoogleCertificates(): IObjectWrapper {
        Log.d(TAG, "unimplemented Method: getGoogleCertificates")
        return ObjectWrapper.wrap(googleCertificates.toTypedArray<IBinder>())
    }

    override fun getGoogleReleaseCertificates(): IObjectWrapper {
        Log.d(TAG, "unimplemented Method: getGoogleReleaseCertificates")
        return ObjectWrapper.wrap(googleReleaseCertificates.toTypedArray<IBinder>())
    }

    override fun isGoogleReleaseSigned(packageName: String, certData: IObjectWrapper): Boolean {
        return PackageUtils.isGooglePackage(packageName, ObjectWrapper.unwrapTyped(certData, ByteArray::class.java))
    }

    override fun isGoogleSigned(packageName: String, certData: IObjectWrapper): Boolean {
        return PackageUtils.isGooglePackage(packageName, ObjectWrapper.unwrapTyped(certData, ByteArray::class.java))
    }

    override fun isGoogleOrPlatformSigned(query: GoogleCertificatesQuery, packageManager: IObjectWrapper): Boolean {
        val pm = ObjectWrapper.unwrapTyped(packageManager, PackageManager::class.java)
        return if (query == null || query.callingPackage == null) {
            false
        } else if (query.getCertData() == null) {
            if (pm == null) false else PackageUtils.isGooglePackage(pm, query.callingPackage)
        } else {
            PackageUtils.isGooglePackage(query.callingPackage, query.getCertData().bytes)
        }
    }

    override fun isPackageGoogleOrPlatformSigned(query: GoogleCertificatesLookupQuery): GoogleCertificatesLookupResponse {
        return certificateLookup(query, true)
    }

    override fun isPackageGoogleOrPlatformSignedAvailable(): Boolean {
        return true
    }

    override fun queryPackageSigned(query: GoogleCertificatesLookupQuery): GoogleCertificatesLookupResponse {
        if (!isFineGrainedPackageVerificationAvailable) throw IllegalStateException("API unavailable")
        return certificateLookup(query, false)
    }

    override fun isFineGrainedPackageVerificationAvailable(): Boolean {
        return true
    }

    private fun certificateLookup(query: GoogleCertificatesLookupQuery, allowPlatform: Boolean): GoogleCertificatesLookupResponse {
        val context = query.context
            ?: return GoogleCertificatesLookupResponse(false, "context is null", 5, 1)
        val packageManager = context.packageManager
            ?: return GoogleCertificatesLookupResponse(false, "context has no package manager", 5, 1)
        val callingPackage = query.callingPackage
            ?: return GoogleCertificatesLookupResponse(false, "callingPackage is null", 5, 1)
        val signatureDigest = PackageUtils.firstSignatureDigest(packageManager, callingPackage)
            ?: return GoogleCertificatesLookupResponse(false, "callingPackage not found", 4, 1)
        return if (PackageUtils.isGooglePackage(callingPackage, signatureDigest)) {
            GoogleCertificatesLookupResponse(true, null, 1, 3)
        } else {
            GoogleCertificatesLookupResponse(false, "not allowed", 2, 1)
        }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
