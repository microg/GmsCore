/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Magic byte that prefixes every Nearby Share advertisement payload.
 */
const val SHARING_ADVERTISEMENT_MAGIC: Byte = 0x4E

/**
 * Version of the advertisement payload understood by this implementation.
 */
const val SHARING_ADVERTISEMENT_VERSION: Int = 1

/**
 * Size, in bytes, of the fixed advertisement header (magic, version, device type, name length).
 */
const val SHARING_ADVERTISEMENT_HEADER_SIZE: Int = 4

/**
 * Minimum and maximum size, in bytes, of the advertised device name.
 */
const val MIN_DEVICE_NAME_BYTES: Int = 1
const val MAX_DEVICE_NAME_BYTES: Int = 64

/**
 * Size, in bytes, of a share-target identifier.
 */
const val SHARE_TARGET_ID_LENGTH: Int = 16

/**
 * Smallest valid advertisement payload: header, a one byte name and the identifier.
 */
const val MIN_SHARING_ADVERTISEMENT_SIZE: Int =
    SHARING_ADVERTISEMENT_HEADER_SIZE + MIN_DEVICE_NAME_BYTES + SHARE_TARGET_ID_LENGTH

/**
 * Device type codes carried in the advertisement.
 */
const val SHARING_DEVICE_TYPE_UNKNOWN: Int = 0
const val SHARING_DEVICE_TYPE_PHONE: Int = 1
const val SHARING_DEVICE_TYPE_TABLET: Int = 2
const val SHARING_DEVICE_TYPE_LAPTOP: Int = 3

/**
 * Thrown when a Nearby Share advertisement payload, or one of its inputs, is malformed.
 */
class InvalidSharingAdvertisementException(message: String) : IllegalArgumentException(message)

/**
 * Immutable representation of a Nearby Share advertisement, as broadcast by a device that is
 * willing to receive a share.
 */
class SharingAdvertisement(
    val version: Int,
    val deviceType: Int,
    val deviceName: String,
    val shareTargetId: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SharingAdvertisement) return false
        return version == other.version &&
                deviceType == other.deviceType &&
                deviceName == other.deviceName &&
                shareTargetId.contentEquals(other.shareTargetId)
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + deviceType
        result = 31 * result + deviceName.hashCode()
        result = 31 * result + shareTargetId.contentHashCode()
        return result
    }

    override fun toString(): String =
        "SharingAdvertisement(version=$version, deviceType=$deviceType, deviceName=$deviceName, " +
                "shareTargetId=${shareTargetId.joinToString("") { "%02x".format(it) }})"
}

private fun isValidDeviceType(deviceType: Int): Boolean =
    deviceType in SHARING_DEVICE_TYPE_UNKNOWN..SHARING_DEVICE_TYPE_LAPTOP

/**
 * Encodes [advertisement] into the byte representation that is broadcast over BLE.
 *
 * @throws InvalidSharingAdvertisementException if the advertisement or any of its fields is out of
 * its permitted range.
 */
fun encodeSharingAdvertisement(advertisement: SharingAdvertisement?): ByteArray {
    if (advertisement == null) {
        throw InvalidSharingAdvertisementException("Advertisement must not be null")
    }
    if (advertisement.version != SHARING_ADVERTISEMENT_VERSION) {
        throw InvalidSharingAdvertisementException("Unsupported advertisement version: ${advertisement.version}")
    }
    if (!isValidDeviceType(advertisement.deviceType)) {
        throw InvalidSharingAdvertisementException("Unsupported device type: ${advertisement.deviceType}")
    }
    val nameBytes = advertisement.deviceName.toByteArray(StandardCharsets.UTF_8)
    if (nameBytes.size < MIN_DEVICE_NAME_BYTES || nameBytes.size > MAX_DEVICE_NAME_BYTES) {
        throw InvalidSharingAdvertisementException(
            "Device name must be $MIN_DEVICE_NAME_BYTES..$MAX_DEVICE_NAME_BYTES bytes, was ${nameBytes.size}"
        )
    }
    if (advertisement.shareTargetId.size != SHARE_TARGET_ID_LENGTH) {
        throw InvalidSharingAdvertisementException(
            "Share target id must be $SHARE_TARGET_ID_LENGTH bytes, was ${advertisement.shareTargetId.size}"
        )
    }
    return ByteBuffer.allocate(SHARING_ADVERTISEMENT_HEADER_SIZE + nameBytes.size + SHARE_TARGET_ID_LENGTH)
        .order(ByteOrder.BIG_ENDIAN)
        .put(SHARING_ADVERTISEMENT_MAGIC)
        .put(advertisement.version.toByte())
        .put(advertisement.deviceType.toByte())
        .put(nameBytes.size.toByte())
        .put(nameBytes)
        .put(advertisement.shareTargetId)
        .array()
}

/**
 * Decodes the byte representation produced by [encodeSharingAdvertisement].
 *
 * @throws InvalidSharingAdvertisementException if the payload is null, truncated or malformed.
 */
fun decodeSharingAdvertisement(payload: ByteArray?): SharingAdvertisement {
    if (payload == null) {
        throw InvalidSharingAdvertisementException("Advertisement payload must not be null")
    }
    if (payload.size < MIN_SHARING_ADVERTISEMENT_SIZE) {
        throw InvalidSharingAdvertisementException("Advertisement payload too short: ${payload.size} bytes")
    }
    if (payload[0] != SHARING_ADVERTISEMENT_MAGIC) {
        throw InvalidSharingAdvertisementException("Bad advertisement magic: ${payload[0]}")
    }
    val version = payload[1].toInt() and 0xFF
    if (version != SHARING_ADVERTISEMENT_VERSION) {
        throw InvalidSharingAdvertisementException("Unsupported advertisement version: $version")
    }
    val deviceType = payload[2].toInt() and 0xFF
    if (!isValidDeviceType(deviceType)) {
        throw InvalidSharingAdvertisementException("Unsupported device type: $deviceType")
    }
    val nameLength = payload[3].toInt() and 0xFF
    if (nameLength < MIN_DEVICE_NAME_BYTES || nameLength > MAX_DEVICE_NAME_BYTES) {
        throw InvalidSharingAdvertisementException("Invalid device name length: $nameLength")
    }
    val expectedSize = SHARING_ADVERTISEMENT_HEADER_SIZE + nameLength + SHARE_TARGET_ID_LENGTH
    if (payload.size != expectedSize) {
        throw InvalidSharingAdvertisementException(
            "Advertisement payload size mismatch: expected $expectedSize, was ${payload.size}"
        )
    }
    val deviceName = String(payload, SHARING_ADVERTISEMENT_HEADER_SIZE, nameLength, StandardCharsets.UTF_8)
    val shareTargetId = payload.copyOfRange(SHARING_ADVERTISEMENT_HEADER_SIZE + nameLength, payload.size)
    return SharingAdvertisement(version, deviceType, deviceName, shareTargetId)
}

/**
 * Derives the stable share-target identifier for [endpointName] and [accountKey].
 *
 * The identifier is the truncated SHA-256 of the account key, a domain separator and the endpoint
 * name, so it is deterministic for a given identity while revealing nothing about the account key,
 * and two different identities or endpoints never collide.
 *
 * @throws InvalidSharingAdvertisementException if the endpoint name or account key is missing.
 */
fun shareTargetId(endpointName: String?, accountKey: ByteArray?): ByteArray {
    if (endpointName.isNullOrEmpty()) {
        throw InvalidSharingAdvertisementException("Endpoint name must not be empty")
    }
    if (accountKey == null || accountKey.isEmpty()) {
        throw InvalidSharingAdvertisementException("Account key must not be empty")
    }
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(accountKey)
    digest.update(0)
    digest.update(endpointName.toByteArray(StandardCharsets.UTF_8))
    return digest.digest().copyOf(SHARE_TARGET_ID_LENGTH)
}
