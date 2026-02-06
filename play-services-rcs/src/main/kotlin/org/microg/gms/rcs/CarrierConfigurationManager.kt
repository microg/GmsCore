/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * CarrierConfigurationManager - Carrier-specific RCS configurations
 * 
 * Contains known carrier configurations for RCS provisioning.
 * Includes auto-configuration server URLs and feature flags.
 */

package org.microg.gms.rcs

import android.util.Log

object CarrierConfigurationManager {

    private const val TAG = "CarrierConfig"

    private val carrierConfigurations = mapOf(
        "310260" to CarrierConfiguration(
            carrierName = "T-Mobile US",
            mccMnc = "310260",
            rcsEnabled = true,
            autoConfigUrl = "https://rcs-acs-prod-us.sandbox.google.com/rcs/config",
            universalProfile = true,
            chatbotSupported = true,
            groupChatSupported = true,
            fileTransferMaxSize = 100 * 1024 * 1024
        ),
        
        "311480" to CarrierConfiguration(
            carrierName = "Verizon Wireless",
            mccMnc = "311480",
            rcsEnabled = true,
            autoConfigUrl = "https://msg.pc.t-mobile.com/HTTP/ACS",
            universalProfile = true,
            chatbotSupported = true,
            groupChatSupported = true,
            fileTransferMaxSize = 100 * 1024 * 1024
        ),
        
        "310410" to CarrierConfiguration(
            carrierName = "AT&T",
            mccMnc = "310410",
            rcsEnabled = true,
            autoConfigUrl = "https://rcs-acs-att.google.com",
            universalProfile = true,
            chatbotSupported = true,
            groupChatSupported = true,
            fileTransferMaxSize = 100 * 1024 * 1024
        ),
        
        "310120" to CarrierConfiguration(
            carrierName = "Sprint",
            mccMnc = "310120",
            rcsEnabled = true,
            autoConfigUrl = null,
            universalProfile = true,
            chatbotSupported = false,
            groupChatSupported = true,
            fileTransferMaxSize = 50 * 1024 * 1024
        ),

        "234010" to CarrierConfiguration(
            carrierName = "O2 UK",
            mccMnc = "234010",
            rcsEnabled = true,
            autoConfigUrl = "https://config.rcs.mnc010.mcc234.pub.3gppnetwork.org",
            universalProfile = true,
            chatbotSupported = true,
            groupChatSupported = true,
            fileTransferMaxSize = 100 * 1024 * 1024
        ),
        
        "234015" to CarrierConfiguration(
            carrierName = "Vodafone UK",
            mccMnc = "234015",
            rcsEnabled = true,
            autoConfigUrl = "https://config.rcs.mnc015.mcc234.pub.3gppnetwork.org",
            universalProfile = true,
            chatbotSupported = true,
            groupChatSupported = true,
            fileTransferMaxSize = 100 * 1024 * 1024
        ),

        "262001" to CarrierConfiguration(
            carrierName = "Telekom Germany",
            mccMnc = "262001",
            rcsEnabled = true,
            autoConfigUrl = "https://config.rcs.mnc001.mcc262.pub.3gppnetwork.org",
            universalProfile = true,
            chatbotSupported = true,
            groupChatSupported = true,
            fileTransferMaxSize = 100 * 1024 * 1024
        ),
        
        "262002" to CarrierConfiguration(
            carrierName = "Vodafone Germany",
            mccMnc = "262002",
            rcsEnabled = true,
            autoConfigUrl = "https://config.rcs.mnc002.mcc262.pub.3gppnetwork.org",
            universalProfile = true,
            chatbotSupported = true,
            groupChatSupported = true,
            fileTransferMaxSize = 100 * 1024 * 1024
        ),

        "405" to CarrierConfiguration(
            carrierName = "India Default",
            mccMnc = "405",
            rcsEnabled = true,
            autoConfigUrl = "https://jibe.google.com/rcs/config",
            universalProfile = true,
            chatbotSupported = true,
            groupChatSupported = true,
            fileTransferMaxSize = 100 * 1024 * 1024
        ),
        
        "40401" to CarrierConfiguration(
            carrierName = "Vodafone India",
            mccMnc = "40401",
            rcsEnabled = true,
            autoConfigUrl = "https://jibe.google.com/rcs/config",
            universalProfile = true,
            chatbotSupported = true,
            groupChatSupported = true,
            fileTransferMaxSize = 100 * 1024 * 1024
        ),
        
        "40410" to CarrierConfiguration(
            carrierName = "Airtel India",
            mccMnc = "40410",
            rcsEnabled = true,
            autoConfigUrl = "https://jibe.google.com/rcs/config",
            universalProfile = true,
            chatbotSupported = true,
            groupChatSupported = true,
            fileTransferMaxSize = 100 * 1024 * 1024
        ),
        
        "40445" to CarrierConfiguration(
            carrierName = "Jio India",
            mccMnc = "40445",
            rcsEnabled = true,
            autoConfigUrl = "https://jibe.google.com/rcs/config",
            universalProfile = true,
            chatbotSupported = true,
            groupChatSupported = true,
            fileTransferMaxSize = 100 * 1024 * 1024
        )
    )

    private val defaultConfiguration = CarrierConfiguration(
        carrierName = "Default",
        mccMnc = "",
        rcsEnabled = true,
        autoConfigUrl = "https://jibe.google.com/rcs/config",
        universalProfile = true,
        chatbotSupported = false,
        groupChatSupported = true,
        fileTransferMaxSize = 25 * 1024 * 1024
    )

    fun getCarrierConfig(mccMnc: String): CarrierConfiguration? {
        val exactMatch = carrierConfigurations[mccMnc]
        if (exactMatch != null) {
            Log.d(TAG, "Found exact carrier config for $mccMnc: ${exactMatch.carrierName}")
            return exactMatch
        }

        val mccOnlyMatch = carrierConfigurations.entries.find { 
            mccMnc.startsWith(it.key) || it.key.startsWith(mccMnc.take(3))
        }?.value
        
        if (mccOnlyMatch != null) {
            Log.d(TAG, "Found partial carrier config for $mccMnc: ${mccOnlyMatch.carrierName}")
            return mccOnlyMatch
        }

        Log.d(TAG, "No carrier config found for $mccMnc, using default")
        return defaultConfiguration
    }

    fun getConfiguration(mccMnc: String): com.google.android.gms.rcs.RcsConfiguration? {
        val carrierConfig = getCarrierConfig(mccMnc) ?: return null
        
        return RcsConfigurationBuilder()
            .setRcsVersion("UP2.4")
            .setRcsProfile("UP2.4")
            .setClientVendor("microG")
            .setCarrierMccMnc(carrierConfig.mccMnc)
            .setCarrierName(carrierConfig.carrierName)
            .setAutoConfigurationServerUrl(carrierConfig.autoConfigUrl)
            .setMaxFileTransferSize(carrierConfig.fileTransferMaxSize)
            .build()
    }

    fun isCarrierSupported(mccMnc: String): Boolean {
        return carrierConfigurations.containsKey(mccMnc) ||
               carrierConfigurations.keys.any { mccMnc.startsWith(it.take(3)) }
    }

    fun getSupportedCarriers(): List<CarrierConfiguration> {
        return carrierConfigurations.values.toList()
    }
}

data class CarrierConfiguration(
    val carrierName: String,
    val mccMnc: String,
    val rcsEnabled: Boolean,
    val autoConfigUrl: String?,
    val universalProfile: Boolean,
    val chatbotSupported: Boolean,
    val groupChatSupported: Boolean,
    val fileTransferMaxSize: Int
)
