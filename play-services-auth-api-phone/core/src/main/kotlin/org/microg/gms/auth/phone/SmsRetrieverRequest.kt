/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.phone

import android.app.PendingIntent

data class SmsRetrieverRequest(
    val id: Int,
    val type: SmsRetrieverRequestType,
    val packageName: String,
    val timeoutPendingIntent: PendingIntent,
    val appHashString: String? = null,
    val creation: Long = System.currentTimeMillis(),
    val senderPhoneNumber: String? = null
)

enum class SmsRetrieverRequestType {
    RETRIEVER, USER_CONSENT
}