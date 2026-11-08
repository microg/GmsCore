/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation.core.proto.builder

import com.google.android.gms.constellation.VerifyPhoneNumberRequest.ImsiRequest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.microg.gms.constellation.core.proto.VerificationParam

class SyncRequestVerificationParamsTest {

    @Test
    fun targetedSimWithoutPhoneNumberHintIsSentWithEmptyValue() {
        val params = buildVerificationParams(listOf(ImsiRequest("310260000000001", null)))

        assertEquals(listOf(VerificationParam(key = "310260000000001", value_ = "")), params)
    }

    @Test
    fun targetedSimPhoneNumberHintIsPreserved() {
        val params = buildVerificationParams(
            listOf(
                ImsiRequest("310260000000001", "+15551234567"),
                ImsiRequest("310260000000002", null)
            )
        )

        assertEquals(
            listOf(
                VerificationParam(key = "310260000000001", value_ = "+15551234567"),
                VerificationParam(key = "310260000000002", value_ = "")
            ),
            params
        )
    }
}
