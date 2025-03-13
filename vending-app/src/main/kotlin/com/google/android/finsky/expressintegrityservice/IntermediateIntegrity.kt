/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.expressintegrityservice

import com.android.vending.Timestamp
import com.google.android.finsky.ClientKey
import okio.ByteString

data class IntermediateIntegrity(
    var packageName: String,
    var cloudProjectNumber: Long,
    var accountName: String,
    var callerKey: ClientKey,
    var intermediateToken: ByteString?,
    var serverGenerated: Timestamp?,
    var webViewRequestMode: Int,
    var testErrorCode: Int
)