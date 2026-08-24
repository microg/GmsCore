/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.finsky.expressintegrityservice
data class ExpressIntegritySession(
    var packageName: String,
    var cloudProjectNumber: Long,
    var sessionId: Long,
    var requestHash: String?,
    var originatingWarmUpSessionId: Long,
    var verdictOptOut: List<Int>?,
    var webViewRequestMode: Int
) {
    override fun toString(): String {
        return "ExpressIntegritySession(packageName='$packageName', cloudProjectNumber=$cloudProjectNumber, sessionId=$sessionId, requestHash=$requestHash, originatingWarmUpSessionId=$originatingWarmUpSessionId, verdictOptOut=${
            verdictOptOut?.joinToString(
                prefix = "[", postfix = "]"
            )
        }, webViewRequestMode=$webViewRequestMode)"
    }
}