/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * SimCardHelper - SIM card detection and information retrieval
 */

package org.microg.gms.rcs

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat

object SimCardHelper {

    private const val TAG = "SimCardHelper"

    fun isSimCardReady(context: Context): Boolean {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val simState = telephonyManager.simState
            
            val isReady = simState == TelephonyManager.SIM_STATE_READY
            Log.d(TAG, "SIM state check: state=$simState, isReady=$isReady")
            
            isReady
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to check SIM state", exception)
            false
        }
    }

    fun getSimCardInfo(context: Context): SimCardInfo? {
        if (!isSimCardReady(context)) {
            return null
        }

        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            
            val operatorName = telephonyManager.networkOperatorName
            val mccMnc = telephonyManager.networkOperator
            val countryCode = telephonyManager.networkCountryIso
            val phoneNumber = getPhoneNumber(context)
            
            if (mccMnc.isNullOrBlank()) {
                Log.w(TAG, "MCC/MNC not available")
                return null
            }

            val simInfo = SimCardInfo(
                carrierName = operatorName ?: "Unknown Carrier",
                mccMnc = mccMnc,
                countryCode = countryCode?.uppercase() ?: "",
                phoneNumber = phoneNumber
            )
            
            Log.d(TAG, "SIM card info retrieved: carrier=${simInfo.carrierName}, mccMnc=${simInfo.mccMnc}")
            
            simInfo
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to get SIM card info", exception)
            null
        }
    }

    fun getPhoneNumber(context: Context): String? {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_NUMBERS
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(TAG, "No permission to read phone number")
            return null
        }

        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val phoneNumber = telephonyManager.line1Number
            
            if (phoneNumber.isNullOrBlank()) {
                Log.d(TAG, "Phone number not available from TelephonyManager, trying SubscriptionManager")
                return getPhoneNumberFromSubscription(context)
            }
            
            phoneNumber
        } catch (securityException: SecurityException) {
            Log.w(TAG, "SecurityException when getting phone number", securityException)
            null
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to get phone number", exception)
            null
        }
    }

    private fun getPhoneNumberFromSubscription(context: Context): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            return null
        }

        return try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val activeSubscriptionInfo = subscriptionManager?.activeSubscriptionInfoList?.firstOrNull()
            
            activeSubscriptionInfo?.number
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to get phone number from subscription", exception)
            null
        }
    }

    fun getSubscriptionCount(context: Context): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            return 1
        }

        return try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            subscriptionManager?.activeSubscriptionInfoCount ?: 1
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to get subscription count", exception)
            1
        }
    }

    fun isMultiSimDevice(context: Context): Boolean {
        return getSubscriptionCount(context) > 1
    }
}

data class SimCardInfo(
    val carrierName: String,
    val mccMnc: String,
    val countryCode: String,
    val phoneNumber: String?
) {
    val mcc: String
        get() = if (mccMnc.length >= 3) mccMnc.substring(0, 3) else ""
    
    val mnc: String
        get() = if (mccMnc.length > 3) mccMnc.substring(3) else ""
}
