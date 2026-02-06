/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * DeviceIntegrityChecker - Device trust verification
 * 
 * Checks if the device is trusted for RCS operations.
 * Google Messages may require Play Integrity attestation.
 */

package org.microg.gms.rcs

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File

object DeviceIntegrityChecker {

    private const val TAG = "DeviceIntegrity"

    fun isDeviceTrusted(context: Context): Boolean {
        val isRooted = checkForRootAccess()
        val isEmulator = checkIfEmulator()
        val hasUnlockedBootloader = checkBootloaderStatus()
        
        val isTrusted = !isRooted && !isEmulator
        
        Log.d(TAG, "Device trust check: rooted=$isRooted, emulator=$isEmulator, unlocked=$hasUnlockedBootloader, trusted=$isTrusted")
        
        return isTrusted
    }

    private fun checkForRootAccess(): Boolean {
        val rootIndicators = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        
        for (path in rootIndicators) {
            if (File(path).exists()) {
                Log.d(TAG, "Root indicator found: $path")
                return true
            }
        }

        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            Log.d(TAG, "Test keys detected in build")
            return true
        }

        return false
    }

    private fun checkIfEmulator(): Boolean {
        val emulatorIndicators = listOf(
            Build.FINGERPRINT.startsWith("generic"),
            Build.FINGERPRINT.startsWith("unknown"),
            Build.MODEL.contains("google_sdk"),
            Build.MODEL.contains("Emulator"),
            Build.MODEL.contains("Android SDK"),
            Build.MANUFACTURER.contains("Genymotion"),
            Build.BRAND.startsWith("generic"),
            Build.DEVICE.startsWith("generic"),
            Build.PRODUCT.contains("sdk"),
            Build.HARDWARE.contains("goldfish"),
            Build.HARDWARE.contains("ranchu")
        )
        
        val isEmulator = emulatorIndicators.any { it }
        
        if (isEmulator) {
            Log.d(TAG, "Device appears to be an emulator")
        }
        
        return isEmulator
    }

    private fun checkBootloaderStatus(): Boolean {
        return try {
            val propertyValue = getSystemProperty("ro.boot.verifiedbootstate")
            val isUnlocked = propertyValue == "orange" || propertyValue == "yellow"
            
            if (isUnlocked) {
                Log.d(TAG, "Bootloader appears to be unlocked: $propertyValue")
            }
            
            isUnlocked
        } catch (exception: Exception) {
            Log.w(TAG, "Could not check bootloader status", exception)
            false
        }
    }

    private fun getSystemProperty(propertyName: String): String {
        return try {
            val processClass = Class.forName("android.os.SystemProperties")
            val getMethod = processClass.getMethod("get", String::class.java)
            getMethod.invoke(null, propertyName) as? String ?: ""
        } catch (exception: Exception) {
            Log.w(TAG, "Could not get system property: $propertyName", exception)
            ""
        }
    }

    fun getDeviceInfo(): Map<String, String> {
        return mapOf(
            "manufacturer" to Build.MANUFACTURER,
            "model" to Build.MODEL,
            "device" to Build.DEVICE,
            "product" to Build.PRODUCT,
            "brand" to Build.BRAND,
            "hardware" to Build.HARDWARE,
            "androidVersion" to Build.VERSION.RELEASE,
            "sdkVersion" to Build.VERSION.SDK_INT.toString(),
            "securityPatch" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "unknown"
        )
    }
}
