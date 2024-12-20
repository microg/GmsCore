package org.microg.gms.common

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import com.google.android.gms.common.BuildConfig
import java.util.*

// Slightly modified implementation from https://github.com/shadow578
/**
 * Utilities to spoof package information.
 */
object PackageSpoofUtils {
    private const val TAG = "SpoofUtils"
    private const val META_SPOOF_PACKAGE_NAME =
        BuildConfig.BASE_PACKAGE_NAME + ".android.gms.SPOOFED_PACKAGE_NAME"
    private const val META_SPOOF_PACKAGE_SIGNATURE =
        BuildConfig.BASE_PACKAGE_NAME + ".android.gms.SPOOFED_PACKAGE_SIGNATURE"

    private val spoofedPackageNameCache = HashMap<String, String>()
    private val spoofedPackageSignatureCache = HashMap<String, String>()

    /**
     * Spoof the package name of a package, if a spoofed name is set.
     *
     * @param packageManager [PackageManager] used to get package information.
     * @param realPackageName The real package name.
     * @return Either the spoofed or the real package name, depending on if the package requested spoofing.
     */
    @JvmStatic
    fun spoofPackageName(
        packageManager: PackageManager,
        realPackageName: String?
    ): String? {
        if (realPackageName.isNullOrEmpty()) return realPackageName

        val spoofedPackageName = getSpoofedPackageName(packageManager, realPackageName)
        return if (!spoofedPackageName.isNullOrEmpty()) {
            Log.i(TAG, "Package name of $realPackageName spoofed to $spoofedPackageName")
            spoofedPackageName
        } else realPackageName
    }

    /**
     * Spoof the signature of a package, if a spoofed name is set.
     *
     * @param packageManager [PackageManager] used to get package information.
     * @param packageName Name of the package to check.
     * @param realSignature The real package signature.
     * @return Either the spoofed or the real signature, depending on if the package requested spoofing.
     */
    @JvmStatic
    @JvmName("spoofStringSignature")
    fun spoofSignature(
        packageManager: PackageManager,
        packageName: String,
        realSignature: String?
    ): String? {
        val spoofedSignature = getSpoofedSignature(packageManager, packageName)

        if (spoofedSignature.isNullOrEmpty()) return realSignature

        Log.i(TAG, "Package signature of $packageName spoofed to $spoofedSignature")
        return spoofedSignature
    }

    /**
     * Spoof the signature of a package, if a spoofed name is set.
     *
     * @param packageManager [PackageManager] used to get package information.
     * @param packageName Name of the package to check.
     * @param realSignature The real package signature.
     * @return Either the spoofed or the real signature, depending on if the package requested spoofing.
     */
    @JvmStatic
    @JvmName("spoofBytesSignature")
    fun spoofSignature(
        packageManager: PackageManager,
        packageName: String,
        realSignature: ByteArray?
    ): ByteArray? {
        val spoofedSignatureString = getSpoofedSignature(packageManager, packageName)

        if (spoofedSignatureString.isNullOrEmpty()) return realSignature

        Log.i(TAG, "Package signature of $packageName spoofed to $spoofedSignatureString")

        return spoofedSignatureString.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    /**
     * Get the spoofed package name for a package.
     *
     * @param packageManager [PackageManager] used to get package information.
     * @param packageName Name of the package to get the spoofed name of.
     * @return Spoofed name string, or null if none set.
     */
    private fun getSpoofedPackageName(
        packageManager: PackageManager,
        packageName: String
    ): String? {
        return spoofedPackageNameCache[packageName] ?: run {
            val meta = getPackageMetadata(packageManager, packageName)
            val spoofedPackageName = meta?.getString(META_SPOOF_PACKAGE_NAME)
            if (spoofedPackageName != null) {
                spoofedPackageNameCache[packageName] = spoofedPackageName
            }

            spoofedPackageName
        }
    }

    /**
     * Get the spoofed package signature for a package.
     *
     * @param packageManager [PackageManager] used to get package information.
     * @param packageName Name of the package to get spoofed signature of.
     * @return Spoofed signature string, or null if none set.
     */
    private fun getSpoofedSignature(
        packageManager: PackageManager,
        packageName: String
    ): String? {
        return spoofedPackageSignatureCache[packageName] ?: run {
            val meta = getPackageMetadata(packageManager, packageName)
            val spoofedSignature = meta?.getString(META_SPOOF_PACKAGE_SIGNATURE)
            if (spoofedSignature != null) {
                spoofedPackageSignatureCache[packageName] = spoofedSignature
            }

            spoofedSignature
        }
    }

    /**
     * Get package metadata.
     *
     * @param packageManager [PackageManager] used to get package information.
     * @param packageName Name of the package to get metadata of.
     * @return Package metadata bundle.
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
            Log.e(TAG, "Failed to get application metadata for $packageName", e)
            null
        }
    }
}