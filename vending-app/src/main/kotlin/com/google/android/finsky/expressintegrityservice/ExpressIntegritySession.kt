/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.expressintegrityservice
data class ExpressIntegritySession(
    var packageName: String,
    var cloudProjectVersion: Long,
    var sessionId: Long,
    var requestHash: String?,
    var originatingWarmUpSessionId: Long,
    var verdictOptOut: List<Int>?,
    var webViewRequestMode: Int
)