/*
 * Copyright 2024-2026 microG Project Team
 * Licensed under Apache-2.0
 *
 * RCS Data Models - Parcelable data classes for RCS AIDL
 */

package com.google.android.gms.rcs

import android.os.Parcel
import android.os.Parcelable

data class RcsConfiguration(
    val rcsVersion: String?,
    val rcsProfile: String?,
    val clientVendor: String?,
    val clientVersion: String?,
    val carrierMccMnc: String?,
    val carrierName: String?,
    val autoConfigurationServerUrl: String?,
    val maxFileTransferSize: Int,
    val sipProxy: String?,
    val sipRealm: String?,
    val imPublicUserIdentity: String?
) : Parcelable {
    
    constructor(parcel: Parcel) : this(
        rcsVersion = parcel.readString(),
        rcsProfile = parcel.readString(),
        clientVendor = parcel.readString(),
        clientVersion = parcel.readString(),
        carrierMccMnc = parcel.readString(),
        carrierName = parcel.readString(),
        autoConfigurationServerUrl = parcel.readString(),
        maxFileTransferSize = parcel.readInt(),
        sipProxy = parcel.readString(),
        sipRealm = parcel.readString(),
        imPublicUserIdentity = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(rcsVersion)
        parcel.writeString(rcsProfile)
        parcel.writeString(clientVendor)
        parcel.writeString(clientVersion)
        parcel.writeString(carrierMccMnc)
        parcel.writeString(carrierName)
        parcel.writeString(autoConfigurationServerUrl)
        parcel.writeInt(maxFileTransferSize)
        parcel.writeString(sipProxy)
        parcel.writeString(sipRealm)
        parcel.writeString(imPublicUserIdentity)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<RcsConfiguration> {
        override fun createFromParcel(parcel: Parcel): RcsConfiguration {
            return RcsConfiguration(parcel)
        }

        override fun newArray(size: Int): Array<RcsConfiguration?> {
            return arrayOfNulls(size)
        }
    }
}

data class RcsCapabilities(
    val phoneNumber: String,
    val isRcsEnabled: Boolean,
    val isChatSupported: Boolean,
    val isFileTransferSupported: Boolean,
    val isGroupChatSupported: Boolean,
    val isVideoCallSupported: Boolean,
    val isAudioCallSupported: Boolean,
    val isGeoLocationPushSupported: Boolean,
    val isChatbotSupported: Boolean,
    val capabilitiesTimestamp: Long
) : Parcelable {
    
    constructor(parcel: Parcel) : this(
        phoneNumber = parcel.readString() ?: "",
        isRcsEnabled = parcel.readInt() == 1,
        isChatSupported = parcel.readInt() == 1,
        isFileTransferSupported = parcel.readInt() == 1,
        isGroupChatSupported = parcel.readInt() == 1,
        isVideoCallSupported = parcel.readInt() == 1,
        isAudioCallSupported = parcel.readInt() == 1,
        isGeoLocationPushSupported = parcel.readInt() == 1,
        isChatbotSupported = parcel.readInt() == 1,
        capabilitiesTimestamp = parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(phoneNumber)
        parcel.writeInt(if (isRcsEnabled) 1 else 0)
        parcel.writeInt(if (isChatSupported) 1 else 0)
        parcel.writeInt(if (isFileTransferSupported) 1 else 0)
        parcel.writeInt(if (isGroupChatSupported) 1 else 0)
        parcel.writeInt(if (isVideoCallSupported) 1 else 0)
        parcel.writeInt(if (isAudioCallSupported) 1 else 0)
        parcel.writeInt(if (isGeoLocationPushSupported) 1 else 0)
        parcel.writeInt(if (isChatbotSupported) 1 else 0)
        parcel.writeLong(capabilitiesTimestamp)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<RcsCapabilities> {
        override fun createFromParcel(parcel: Parcel): RcsCapabilities {
            return RcsCapabilities(parcel)
        }

        override fun newArray(size: Int): Array<RcsCapabilities?> {
            return arrayOfNulls(size)
        }
    }
}
