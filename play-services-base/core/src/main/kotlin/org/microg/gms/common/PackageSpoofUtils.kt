package org.microg.gms.common

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import org.microg.gms.base.core.BuildConfig

/**
 * utilities to spoof package information
 */
internal object PackageSpoofUtils {
    private const val TAG = "SpoofUtils"
    private const val META_SPOOF_PACKAGE_NAME =
        "${BuildConfig.BASE_PACKAGE_NAME}.android.gms.SPOOFED_PACKAGE_NAME"
    private const val META_SPOOF_PACKAGE_SIGNATURE =
        "${BuildConfig.BASE_PACKAGE_NAME}.android.gms.SPOOFED_PACKAGE_SIGNATURE"

    private val spoofedPackageNameCache = HashMap<String, String>()
    private val spoofedPackageSignatureCache = HashMap<String, String>()

    /**
     * spoof the package name of a package, if a spoofed name is set
     *
     * @param packageManager manager used to get package information
     * @param realPackageName the real package name
     * @return either the spoofed or the real package name, depending on if the package requested spoofing
     */
    @JvmStatic
    fun spoofPackageName(
        packageManager: PackageManager,
        realPackageName: String?
    ): String? {
        if (realPackageName.isNullOrEmpty()) return realPackageName

        val spoofedPackageName = getSpoofedPackageName(packageManager, realPackageName)
        return if (!spoofedPackageName.isNullOrEmpty()) {
            Log.i(TAG, "package name of $realPackageName spoofed to $spoofedPackageName")
            spoofedPackageName
        } else realPackageName
    }

    /**
     * spoof the signature of a package, if a spoofed name is set
     *
     * @param packageManager manager used to get package information
     * @param packageName name of the package to check
     * @param realSignature the real package signature
     * @return either the spoofed or the real signature, depending on if the package requested spoofing
     */
    @JvmStatic
    @JvmName("spoofStringSignature")
    fun spoofSignature(
        packageManager: PackageManager,
        packageName: String,
        realSignature: String?
    ): String? {
        val spoofedSignature = getSpoofedSignature(packageManager, packageName)
        return if (!spoofedSignature.isNullOrEmpty()) {
            Log.i(TAG, "package signature of $packageName spoofed to $spoofedSignature")
            spoofedSignature
        } else realSignature
    }

    /**
     * spoof the signature of a package, if a spoofed name is set
     *
     * @param packageManager manager used to get package information
     * @param packageName name of the package to check
     * @param realSignature the real package signature
     * @return either the spoofed or the real signature, depending on if the package requested spoofing
     */
    @JvmStatic
    @JvmName("spoofBytesSignature")
    fun spoofSignature(
        packageManager: PackageManager,
        packageName: String,
        realSignature: ByteArray?
    ): ByteArray? {
        val spoofedSignatureString = getSpoofedSignature(packageManager, packageName)
        return if (!spoofedSignatureString.isNullOrEmpty()) {
            Log.i(TAG, "package signature of $packageName spoofed to $spoofedSignatureString")

            // convert hex string to bytes
            spoofedSignatureString.chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
        } else realSignature
    }

    /**
     * get the spoofed package name for a package
     *
     * @param packageManager manager used to get package information
     * @param packageName name of the package to get spoofed name of
     * @return spoofed name string, or null if none set
     */
    private fun getSpoofedPackageName(
        packageManager: PackageManager,
        packageName: String
    ): String? {
        return spoofedPackageNameCache[packageName] ?: run {
            // fetch value
            val meta = getPackageMetadata(packageManager, packageName)
            val spoofedPackageName = meta?.getString(META_SPOOF_PACKAGE_NAME)
            if (spoofedPackageName != null) {
                spoofedPackageNameCache[packageName] = spoofedPackageName
            }

            spoofedPackageName
        }
    }

    /**
     * get the spoofed package signature for a package
     *
     * @param packageManager manager used to get package information
     * @param packageName name of the package to get spoofed signature of
     * @return spoofed signature string, or null if none set
     */
    private fun getSpoofedSignature(
        packageManager: PackageManager,
        packageName: String
    ): String? {
        return spoofedPackageSignatureCache[packageName] ?: run {
            // fetch value
            val meta = getPackageMetadata(packageManager, packageName)
            val spoofedSignature = meta?.getString(META_SPOOF_PACKAGE_SIGNATURE)
            if (spoofedSignature != null) {
                spoofedPackageSignatureCache[packageName] = spoofedSignature
            }

            spoofedSignature
        }
    }

    /**
     * get package metadata
     *
     * @param packageManager manager used to get package information
     * @param packageName name of the package to get metadata of
     * @return package metadata bundle
     */
    private fun getPackageMetadata(packageManager: PackageManager, packageName: String): Bundle? {
        return try {
            // PackageManager.getPackageInfo() has been deprecated in targetSdkVersion 30+
            // To solve this, add the QUERY_ALL_PACKAGES permission to AndroidManifest.xml
            packageManager
                .getPackageInfo(packageName, PackageManager.GET_META_DATA)
                ?.applicationInfo
                ?.metaData
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "failed to get application metadata for $packageName", e)
            null
        }
    }
}