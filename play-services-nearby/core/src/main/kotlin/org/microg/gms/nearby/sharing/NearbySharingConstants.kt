/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.sharing

object NearbySharingConstants {
    const val TAG = "NearbySharing"

    // mDNS service configuration
    const val SERVICE_TYPE = "_FC9F5ED42C8A._tcp."
    val SERVICE_ID_HASH = byteArrayOf(0xFC.toByte(), 0x9F.toByte(), 0x5E.toByte())
    const val PCP_BYTE: Byte = 0x23

    // Intents & Actions
    const val ACTION_SHARE_NEARBY = "com.google.android.gms.SHARE_NEARBY"
    const val ACTION_RECEIVE_NEARBY = "com.google.android.gms.RECEIVE_NEARBY"
    const val ACTION_SHARING_SETTINGS = "com.google.android.gms.settings.SHARING"
    const val ACTION_SEND_FOLDER = "com.google.android.gms.nearby.SEND_FOLDER"
    const val ACTION_START_SERVICE = "com.google.android.gms.nearby.sharing.START_SERVICE"
    const val ACTION_SERVICE_START = "com.google.android.gms.nearby.sharing.service.NearbySharingService.START"

    // Extras
    const val EXTRA_PARENT_FOLDER = "com.google.android.gms.nearby.PARENT_FOLDER"
    const val EXTRA_FILE_COUNT = "com.google.android.gms.nearby.FILE_COUNT"
    const val EXTRA_SEND_FOLDER_CONTENT_URI = "com.google.android.gms.nearby.SEND_FOLDER_CONTENT_URI"
    const val EXTRA_REQUIRED_PACKAGE = "com.google.android.gms.nearby.sharing.EXTRA_REQUIRED_PACKAGE"

    // Slice
    const val SLICE_URI_STRING = "content://com.google.android.gms.nearby.sharing/scan"

    // Protocol Limits
    const val MAX_FRAME_LENGTH = 5 * 1024 * 1024
    const val CHUNK_SIZE = 64 * 1024

    // Salt and crypto constants
    val D2D_SALT = byteArrayOf(
        0x82.toByte(), 0xAA.toByte(), 0x55.toByte(), 0xA0.toByte(),
        0xD3.toByte(), 0x97.toByte(), 0xF8.toByte(), 0x83.toByte(),
        0x46.toByte(), 0xCA.toByte(), 0x1C.toByte(), 0xEE.toByte(),
        0x8D.toByte(), 0x39.toByte(), 0x09.toByte(), 0xB9.toByte(),
        0x5F.toByte(), 0x13.toByte(), 0xFA.toByte(), 0x7D.toByte(),
        0xEB.toByte(), 0x1D.toByte(), 0x4A.toByte(), 0xB3.toByte(),
        0x83.toByte(), 0x76.toByte(), 0xB8.toByte(), 0x25.toByte(),
        0x6D.toByte(), 0xA8.toByte(), 0x55.toByte(), 0x10.toByte()
    )
}
