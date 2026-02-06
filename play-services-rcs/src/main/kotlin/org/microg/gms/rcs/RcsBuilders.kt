/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * Builders for RCS data models
 */

package org.microg.gms.rcs

import com.google.android.gms.rcs.RcsCapabilities
import com.google.android.gms.rcs.RcsConfiguration

class RcsConfigurationBuilder {
    private var rcsVersion: String = "UP2.4"
    private var rcsProfile: String = "UP2.4"
    private var clientVendor: String = "microG"
    private var clientVersion: String = "1.0.0"
    private var carrierMccMnc: String? = null
    private var carrierName: String? = null
    private var autoConfigurationServerUrl: String? = null
    private var maxFileTransferSize: Int = 100 * 1024 * 1024

    fun setRcsVersion(version: String): RcsConfigurationBuilder {
        this.rcsVersion = version
        return this
    }

    fun setRcsProfile(profile: String): RcsConfigurationBuilder {
        this.rcsProfile = profile
        return this
    }

    fun setClientVendor(vendor: String): RcsConfigurationBuilder {
        this.clientVendor = vendor
        return this
    }

    fun setClientVersion(version: String): RcsConfigurationBuilder {
        this.clientVersion = version
        return this
    }

    fun setCarrierMccMnc(mccMnc: String?): RcsConfigurationBuilder {
        this.carrierMccMnc = mccMnc
        return this
    }

    fun setCarrierName(name: String?): RcsConfigurationBuilder {
        this.carrierName = name
        return this
    }

    fun setAutoConfigurationServerUrl(url: String?): RcsConfigurationBuilder {
        this.autoConfigurationServerUrl = url
        return this
    }

    fun setMaxFileTransferSize(size: Int): RcsConfigurationBuilder {
        this.maxFileTransferSize = size
        return this
    }

    fun build(): RcsConfiguration {
        return RcsConfiguration(
            rcsVersion = rcsVersion,
            rcsProfile = rcsProfile,
            clientVendor = clientVendor,
            clientVersion = clientVersion,
            carrierMccMnc = carrierMccMnc,
            carrierName = carrierName,
            autoConfigurationServerUrl = autoConfigurationServerUrl,
            maxFileTransferSize = maxFileTransferSize
        )
    }
}

class RcsCapabilitiesBuilder {
    private var phoneNumber: String = ""
    private var isRcsEnabled: Boolean = false
    private var isChatSupported: Boolean = false
    private var isFileTransferSupported: Boolean = false
    private var isGroupChatSupported: Boolean = false
    private var isVideoCallSupported: Boolean = false
    private var isAudioCallSupported: Boolean = false
    private var isGeoLocationPushSupported: Boolean = false
    private var isChatbotSupported: Boolean = false

    fun setPhoneNumber(number: String): RcsCapabilitiesBuilder {
        this.phoneNumber = number
        return this
    }

    fun setRcsEnabled(enabled: Boolean): RcsCapabilitiesBuilder {
        this.isRcsEnabled = enabled
        return this
    }

    fun setChatSupported(supported: Boolean): RcsCapabilitiesBuilder {
        this.isChatSupported = supported
        return this
    }

    fun setFileTransferSupported(supported: Boolean): RcsCapabilitiesBuilder {
        this.isFileTransferSupported = supported
        return this
    }

    fun setGroupChatSupported(supported: Boolean): RcsCapabilitiesBuilder {
        this.isGroupChatSupported = supported
        return this
    }

    fun setVideoCallSupported(supported: Boolean): RcsCapabilitiesBuilder {
        this.isVideoCallSupported = supported
        return this
    }

    fun setAudioCallSupported(supported: Boolean): RcsCapabilitiesBuilder {
        this.isAudioCallSupported = supported
        return this
    }

    fun setGeoLocationPushSupported(supported: Boolean): RcsCapabilitiesBuilder {
        this.isGeoLocationPushSupported = supported
        return this
    }

    fun setChatbotSupported(supported: Boolean): RcsCapabilitiesBuilder {
        this.isChatbotSupported = supported
        return this
    }

    fun build(): RcsCapabilities {
        return RcsCapabilities(
            phoneNumber = phoneNumber,
            isRcsEnabled = isRcsEnabled,
            isChatSupported = isChatSupported,
            isFileTransferSupported = isFileTransferSupported,
            isGroupChatSupported = isGroupChatSupported,
            isVideoCallSupported = isVideoCallSupported,
            isAudioCallSupported = isAudioCallSupported,
            isGeoLocationPushSupported = isGeoLocationPushSupported,
            isChatbotSupported = isChatbotSupported,
            capabilitiesTimestamp = System.currentTimeMillis()
        )
    }
}
