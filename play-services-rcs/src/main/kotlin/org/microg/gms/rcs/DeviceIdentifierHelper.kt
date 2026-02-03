/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * DeviceIdentifierHelper - Manages READ_DEVICE_IDENTIFIERS permission
 * 
 * This is the KEY component that makes RCS work with microG.
 * Google Messages requires this permission to access IMEI/MEID for
 * RCS registration with Google Jibe servers.
 */

package org.microg.gms.rcs

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.telephony.TelephonyManager
import android.util.Log

class DeviceIdentifierHelper(private val context: Context) {

    companion object {
        private const val TAG = "DeviceIdentifier"
        private const val APP_OPS_READ_DEVICE_IDENTIFIERS = "android:read_device_identifiers"
        private const val OP_READ_DEVICE_IDENTIFIERS = 89

        fun hasReadDeviceIdentifiersPermission(context: Context): Boolean {
            return try {
                val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val uid = Process.myUid()
                val packageName = context.packageName
                
                val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOpsManager.unsafeCheckOpNoThrow(
                        APP_OPS_READ_DEVICE_IDENTIFIERS,
                        uid,
                        packageName
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appOpsManager.checkOpNoThrow(
                        APP_OPS_READ_DEVICE_IDENTIFIERS,
                        uid,
                        packageName
                    )
                }
                
                val isAllowed = result == AppOpsManager.MODE_ALLOWED
                Log.d(TAG, "READ_DEVICE_IDENTIFIERS permission check: $isAllowed (result=$result)")
                
                isAllowed
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to check device identifiers permission", exception)
                false
            }
        }

        fun getDeviceId(context: Context): String? {
            if (!hasReadDeviceIdentifiersPermission(context)) {
                Log.w(TAG, "Cannot get device ID: permission not granted")
                return null
            }

            return try {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    telephonyManager.imei ?: telephonyManager.meid
                } else {
                    @Suppress("DEPRECATION")
                    telephonyManager.deviceId
                }
            } catch (securityException: SecurityException) {
                Log.e(TAG, "SecurityException when getting device ID", securityException)
                null
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to get device ID", exception)
                null
            }
        }

        fun getImei(context: Context, slotIndex: Int = 0): String? {
            if (!hasReadDeviceIdentifiersPermission(context)) {
                return null
            }

            return try {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    telephonyManager.getImei(slotIndex)
                } else {
                    @Suppress("DEPRECATION")
                    telephonyManager.deviceId
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to get IMEI for slot $slotIndex", exception)
                null
            }
        }

        fun getMeid(context: Context, slotIndex: Int = 0): String? {
            if (!hasReadDeviceIdentifiersPermission(context)) {
                return null
            }

            return try {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    telephonyManager.getMeid(slotIndex)
                } else {
                    null
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to get MEID for slot $slotIndex", exception)
                null
            }
        }

        fun getAdbGrantCommand(): String {
            return "adb shell appops set com.google.android.gms READ_DEVICE_IDENTIFIERS allow"
        }

        fun getInstructionsForGrantingPermission(): String {
            return """
                To grant READ_DEVICE_IDENTIFIERS permission for microG:
                
                1. Enable USB debugging on your device
                2. Connect to a computer with ADB installed
                3. Run the following command:
                
                   adb shell appops set com.google.android.gms READ_DEVICE_IDENTIFIERS allow
                
                4. Verify with:
                
                   adb shell appops get com.google.android.gms READ_DEVICE_IDENTIFIERS
                
                5. Restart Google Messages and enable RCS
            """.trimIndent()
        }
    }
}
