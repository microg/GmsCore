/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.os.ParcelUuid
import java.util.*

const val TAG = "ExposureNotification"
val SERVICE_UUID = ParcelUuid(UUID.fromString("0000FD6F-0000-1000-8000-00805F9B34FB"))

const val SCANNING_INTERVAL = 3 * 60 // Google uses 5m, but we use a slightly different scanning and reporting system
const val SCANNING_INTERVAL_MS = SCANNING_INTERVAL * 1000L
const val SCANNING_TIME = 20 // Google uses 4s + 13s (if Bluetooth is used by something else)
const val SCANNING_TIME_MS = SCANNING_TIME * 1000L

const val ROLLING_WINDOW_LENGTH = 10 * 60
const val ROLLING_WINDOW_LENGTH_MS = ROLLING_WINDOW_LENGTH * 1000
const val ROLLING_PERIOD = 144
const val ALLOWED_KEY_OFFSET_MS = 60 * 60 * 1000
const val MINIMUM_EXPOSURE_DURATION_MS = 0
const val KEEP_DAYS = 14

const val ACTION_CONFIRM = "org.microg.gms.nearby.exposurenotification.CONFIRM"
const val KEY_CONFIRM_ACTION = "action"
const val KEY_CONFIRM_RECEIVER = "receiver"
const val KEY_CONFIRM_PACKAGE = "package"
const val CONFIRM_ACTION_START = "start"
const val CONFIRM_ACTION_STOP = "stop"
const val CONFIRM_ACTION_KEYS = "keys"
const val CONFIRM_PERMISSION_VALIDITY = 60 * 60 * 1000L

const val PERMISSION_EXPOSURE_CALLBACK = "com.google.android.gms.nearby.exposurenotification.EXPOSURE_CALLBACK"

const val TX_POWER_LOW = -15

const val ADVERTISER_OFFSET = 60 * 1000
const val CLEANUP_INTERVAL = 24 * 60 * 60 * 1000

const val VERSION_1_0: Byte = 0x40
const val VERSION_1_1: Byte = 0x50

/**
 * No calibration data, using fleet-wide as default options.
 */
const val CONFIDENCE_LOWEST: Byte = 0

/**
 * Using average calibration over models from manufacturer.
 */
const val CONFIDENCE_LOW: Byte = 1

/**
 * Using single-antenna orientation for a similar model.
 */
const val CONFIDENCE_MEDIUM: Byte = 2

/**
 * Using significant calibration data for this model.
 */
const val CONFIDENCE_HIGH: Byte = 3
