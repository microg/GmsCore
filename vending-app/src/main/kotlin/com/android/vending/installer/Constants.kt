/*
 * SPDX-FileCopyrightText: 2025 e foundation
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.vending.installer

import android.content.Context
import java.io.File

private const val FILE_SAVE_PATH = "phonesky-download-service"
internal const val TAG = "GmsPackageInstaller"

const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"
const val VENDING_INSTALL_ACTION = "com.android.vending.ACTION_INSTALL"
const val VENDING_INSTALL_DELETE_ACTION = "com.android.vending.ACTION_INSTALL_DELETE"
const val SESSION_ID = "session_id"
const val SESSION_RESULT_RECEIVER_INTENT = "session_result_receiver_intent"
const val SPLIT_LANGUAGE_TAG = "config."

fun Context.packageDownloadLocation() = File(cacheDir, FILE_SAVE_PATH).apply {
    if (!exists()) mkdir()
}
